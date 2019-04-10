package studio.dipdev.flutter.kinecosystemsdk

import android.app.Activity
import android.content.Context
import android.os.Handler
import com.google.gson.Gson
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kin.devplatform.Environment
import kin.devplatform.Kin
import kin.devplatform.KinCallback
import kin.devplatform.KinEnvironment
import kin.devplatform.KinMigrationListener
import kin.devplatform.base.Observer
import kin.devplatform.data.model.Balance
import kin.devplatform.data.model.OrderConfirmation
import kin.devplatform.exception.KinEcosystemException


class FlutterKinEcosystemSdkPlugin(private var activity: Activity, private var context: Context): MethodCallHandler {
    var isKinInit = false
    var balance: Long = 0

    private var balanceObserver = object : Observer<Balance>() {
        override fun onChanged(p0: Balance?) {
            if (p0 != null) {
                balance = p0.amount.longValueExact()
                balanceCallback?.success(balance)
            }
        }
    }

    companion object {
        var balanceCallback: EventChannel.EventSink? = null
        var infoCallback: EventChannel.EventSink? = null

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_kin_ecosystem_sdk")
            val instance = FlutterKinEcosystemSdkPlugin(registrar.activity(), registrar.activity().applicationContext)
            channel.setMethodCallHandler(instance)

            EventChannel(registrar.view(), "flutter_kin_ecosystem_sdk_balance").setStreamHandler(
                    object : EventChannel.StreamHandler {
                        override fun onListen(args: Any?, events: EventChannel.EventSink) {
                            balanceCallback = events
                        }

                        override fun onCancel(args: Any?) {
                        }
                    }
            )

            EventChannel(registrar.view(), "flutter_kin_ecosystem_sdk_info").setStreamHandler(
                    object : EventChannel.StreamHandler {
                        override fun onListen(args: Any?, events: EventChannel.EventSink) {
                            infoCallback = events
                        }

                        override fun onCancel(args: Any?) {
                        }
                    }
            )

        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when {
            call.method == "kinStart" -> {
                val token: String = call.argument("token") ?: return
                val initBalanceObserver: Boolean = call.argument("initBalanceObserver") ?: return
                val isProduction: Boolean = call.argument("isProduction") ?: return

                Kin.start(context, token, Environment.getProduction(), object : KinCallback<Void> {
                    override fun onFailure(error: KinEcosystemException?) {
                        isKinInit = false
                        sendError("kinStart", error)
                    }

                    override fun onResponse(response: Void?) {
                        isKinInit = true
                        sendReport("kinStart", "Kin started")
                    }
                }, object : KinMigrationListener {
                    override fun onFinish() {
                        sendReport("kinMigration", "migrationFinish")
                    }

                    override fun onError(e: java.lang.Exception?) {
                        var migrationStr = "Kin migration failed"
                        if (e != null) migrationStr = e.message!!
                        val err = Error("kinMigration", migrationStr)
                        sendError("-2", "Kin migration failed", err)
                    }

                    override fun onStart() {
                        sendReport("kinMigration", "migrationStart")
                    }
                })

                val environment: KinEnvironment = if (isProduction){
                    Environment.getProduction()
                }else{
                    Environment.getPlayground()
                }
                Kin.start(context, token, environment, object : KinCallback<Void> {
                    override fun onFailure(error: KinEcosystemException?) {
                        isKinInit = false
                        sendError("kinStart", error)
                    }

                    override fun onResponse(response: Void?) {
                        isKinInit = true
                        sendReport("kinStart", "Kin started")
                        if (initBalanceObserver) {
                            try {
                                Kin.addBalanceObserver(balanceObserver)
                            } catch (e: Throwable) {
                                sendError( "Balance Observer doesn't initialized ", e)
                            }
                        }
//                        migrationEmulation()
                    }
                })
            }
            call.method == "launchKinMarket" -> if (ifKinInit()) Kin.launchMarketplace(activity)
            call.method == "getPublicAddress" -> if (ifKinInit()) result.success(Kin.getPublicAddress())
            call.method == "kinEarn" -> {
                if (!ifKinInit()) return
                val jwt: String? = call.argument("jwt")
                if (jwt != null) kinEarn(jwt)
            }
            call.method == "kinSpend" -> {
                if (!ifKinInit()) return
                val jwt: String? = call.argument("jwt")
                if (jwt != null) kinSpend(jwt)
            }
            call.method == "kinPayToUser" -> {
                if (!ifKinInit()) return
                val jwt: String? = call.argument("jwt")
                if (jwt != null) kinPayToUser(jwt)
            }
            call.method == "orderConfirmation" -> {
                if (!ifKinInit()) return
                val offerId: String? = call.argument("offerId")
                if (offerId != null) orderConfirmation(offerId)
            }
            else -> result.notImplemented()
        }
    }

    private fun migrationEmulation(){
        sendReport("kinMigration", "migrationStart")
        Handler().postDelayed(
                {
                    sendReport("kinMigration", "migrationFinish")
                },
                10_000
        )
        Handler().postDelayed(
                {
                    sendReport("kinMigration", "migrationStart")
                },
                20_000
        )
        Handler().postDelayed(
                {
                    val err = Error("kinMigration", "Some migration error")
                    sendError("-2", "Kin migration failed", err)
                },
                30_000
        )
    }

    private fun kinEarn(jwt: String) {
        var prevBalance = balance
        try {
            Kin.requestPayment(jwt, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError("kinEarn", p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport("kinEarn", p0.toString(), balance - prevBalance)
                }
            })
        } catch (e: Throwable) {
            sendError("kinEarn", e)
        }
    }

    private fun kinSpend(jwt: String) {
        try {
            Kin.purchase(jwt, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError("kinSpend", p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport("kinSpend", p0.toString())
                }
            })
        } catch (e: Throwable) {
            sendError("kinSpend", e)
        }
    }

    private fun kinPayToUser(jwt: String) {
        try {
            Kin.payToUser(jwt, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError("kinPayToUser", p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport("kinPayToUser", p0.toString())
                }
            })
        } catch (e: Throwable) {
            sendError("kinPayToUser", e)
        }

    }

    private fun orderConfirmation(offerId: String) {
        try {
            Kin.getOrderConfirmation(offerId, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError("orderConfirmation", p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport("orderConfirmation", p0.toString())
                }
            })
        } catch (e: Exception) {
            sendError("orderConfirmation", e)
        }
    }

    private fun sendReport(type: String, message: String, amount: Long? = null) {
        val info : Info
        if (amount != null)
            info = Info(type, message, amount)
        else
            info = Info(type, message)
        var json: String? = null
        try {
            json = Gson().toJson(info)
        } catch (e: Throwable) {
            sendError("json", e)
        }
        if (json != null) infoCallback?.success(json)
    }

    private fun sendError(type: String, error: Throwable) {
        val err = Error(type, error.localizedMessage)
        var message: String? = error.message
        if (message == null) message = ""
        sendError(message, error.localizedMessage, err)
    }

    private fun sendError(type: String, error: KinEcosystemException?) {
        if (error == null) return
        val err = Error(type, error.localizedMessage)
        sendError(error.code.toString(), error.localizedMessage, err)
    }

    private fun sendError(code: String, message: String?, details: Error) {
        var json: String? = null
        try {
            json = Gson().toJson(details)
        } catch (e: Throwable) {
            sendError("json", e)
        }
        if (json != null) infoCallback?.error(code, message, json)
    }

    private fun ifKinInit(): Boolean {
        if (!isKinInit) {
            val err = Error("kinStart", "Kin SDK not started")
            sendError("-1", "Kin SDK not started", err)
        }
        return isKinInit
    }

    data class Info(val type: String, val message: String, val amount: Long? = null)
    data class Error(val type: String, val message: String)
}
