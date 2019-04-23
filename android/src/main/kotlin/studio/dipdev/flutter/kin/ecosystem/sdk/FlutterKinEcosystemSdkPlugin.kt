package studio.dipdev.flutter.kin.ecosystem.sdk

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kin.devplatform.*
import kin.devplatform.base.Observer
import kin.devplatform.data.model.Balance
import kin.devplatform.data.model.OrderConfirmation
import kin.devplatform.exception.KinEcosystemException


class FlutterKinEcosystemSdkPlugin(private var activity: Activity, private var context: Context) : MethodCallHandler {
    var isKinInit = false
    var balance: Long = 0

    private var balanceObserver = object : Observer<Balance>() {
        override fun onChanged(p0: Balance?) {
            if (p0 != null) {
                balance = p0.amount.longValueExact()
                balanceCallback.success(balance)
            }
        }
    }

    companion object {
        lateinit var balanceCallback: EventChannel.EventSink
        lateinit var infoCallback: EventChannel.EventSink

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), Constants.FLUTTER_KIN_ECOSYSTEM_SDK.value)
            val instance = FlutterKinEcosystemSdkPlugin(registrar.activity(), registrar.activity().applicationContext)
            channel.setMethodCallHandler(instance)

            EventChannel(registrar.view(), Constants.FLUTTER_KIN_ECOSYSTEM_SDK_BALANCE.value).setStreamHandler(
                    object : EventChannel.StreamHandler {
                        override fun onListen(args: Any?, events: EventChannel.EventSink) {
                            balanceCallback = events
                        }

                        override fun onCancel(args: Any?) {
                        }
                    }
            )

            EventChannel(registrar.view(), Constants.FLUTTER_KIN_ECOSYSTEM_SDK_INFO.value).setStreamHandler(
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
            call.method == Constants.KIN_START.value -> {
                val token: String = call.argument("token") ?: return
                val initBalanceObserver: Boolean = call.argument("initBalanceObserver") ?: return
                val isProduction: Boolean = call.argument("isProduction") ?: return

                val environment: KinEnvironment = if (isProduction) {
                    Environment.getProduction()
                } else {
                    Environment.getPlayground()
                }
                Kin.start(context, token, environment, object : KinCallback<Void> {
                    override fun onFailure(error: KinEcosystemException?) {
                        isKinInit = false
                        sendError(Constants.KIN_START.value, error)
                    }

                    override fun onResponse(response: Void?) {
                        isKinInit = true
                        if (initBalanceObserver) {
                            try {
                                Kin.addBalanceObserver(balanceObserver)
                            } catch (e: Throwable) {
                                sendError("Balance Observer doesn't initialized ", e)
                            }
                        }
                        sendReport(Constants.KIN_START.value, "Kin started")
                    }
                }, object : KinMigrationListener {
                    override fun onFinish() {
                        sendReport(Constants.KIN_MIGRATION.value, "migrationFinish")
                    }

                    override fun onError(e: java.lang.Exception?) {
                        var migrationStr = "Kin migration failed"
                        if (e != null) migrationStr = e.message!!
                        val err = Error("kinMigration", migrationStr)
                        sendError("-2", "Kin migration failed", err)
                    }

                    override fun onStart() {
                        sendReport(Constants.KIN_MIGRATION.value, "migrationStart")
                    }
                })
            }
            call.method == Constants.LAUNCH_KIN_MARKET.value -> if (ifKinInit()) Kin.launchMarketplace(activity)
            call.method == Constants.GET_WALLET.value -> if (ifKinInit()) result.success(Kin.getPublicAddress())
            call.method == Constants.KIN_EARN.value -> {
                if (!ifKinInit()) return
                val jwt: String? = call.argument("jwt")
                if (jwt != null) kinEarn(jwt)
            }
            call.method == Constants.KIN_SPEND.value -> {
                if (!ifKinInit()) return
                val jwt: String? = call.argument("jwt")
                if (jwt != null) kinSpend(jwt)
            }
            call.method == Constants.KIN_PAY_TO_USER.value -> {
                if (!ifKinInit()) return
                val jwt: String? = call.argument("jwt")
                if (jwt != null) kinPayToUser(jwt)
            }
            call.method == Constants.ORDER_CONFIRMATION.value -> {
                if (!ifKinInit()) return
                val offerId: String? = call.argument("offerId")
                if (offerId != null) orderConfirmation(offerId)
            }
            else -> result.notImplemented()
        }
    }

    /*private fun migrationEmulation(){
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
    }*/

    private fun kinEarn(jwt: String) {
        val prevBalance = balance
        try {
            Kin.requestPayment(jwt, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError(Constants.KIN_EARN.value, p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport(Constants.KIN_EARN.value, p0.toString(), balance - prevBalance)
                }
            })
        } catch (e: Throwable) {
            sendError(Constants.KIN_EARN.value, e)
        }
    }

    private fun kinSpend(jwt: String) {
        try {
            Kin.purchase(jwt, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError(Constants.KIN_SPEND.value, p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport(Constants.KIN_SPEND.value, p0.toString())
                }
            })
        } catch (e: Throwable) {
            sendError(Constants.KIN_SPEND.value, e)
        }
    }

    private fun kinPayToUser(jwt: String) {
        try {
            Kin.payToUser(jwt, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError(Constants.KIN_PAY_TO_USER.value, p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport(Constants.KIN_PAY_TO_USER.value, p0.toString())
                }
            })
        } catch (e: Throwable) {
            sendError(Constants.KIN_PAY_TO_USER.value, e)
        }

    }

    private fun orderConfirmation(offerId: String) {
        try {
            Kin.getOrderConfirmation(offerId, object : KinCallback<OrderConfirmation> {
                override fun onFailure(p0: KinEcosystemException?) {
                    sendError(Constants.ORDER_CONFIRMATION.value, p0)
                }

                override fun onResponse(p0: OrderConfirmation?) {
                    sendReport(Constants.ORDER_CONFIRMATION.value, p0.toString())
                }
            })
        } catch (e: Exception) {
            sendError(Constants.ORDER_CONFIRMATION.value, e)
        }
    }

    private fun sendReport(type: String, message: String, amount: Long? = null) {
        val info: Info = if (amount != null)
            Info(type, message, amount)
        else
            Info(type, message)
        var json: String? = null
        try {
            json = Gson().toJson(info)
        } catch (e: Throwable) {
            sendError("json", e)
        }
        if (json != null) infoCallback.success(json)
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
        if (json != null) infoCallback.error(code, message, json)
    }

    private fun ifKinInit(): Boolean {
        if (!isKinInit) {
            val err = Error(Constants.KIN_START.value, "Kin SDK not started")
            sendError("-1", "Kin SDK not started", err)
        }
        return isKinInit
    }

    data class Info(val type: String, val message: String, val amount: Long? = null)
    data class Error(val type: String, val message: String)

    enum class Constants(val value: String) {
        FLUTTER_KIN_SDK("flutter_kin_sdk"),
        FLUTTER_KIN_ECOSYSTEM_SDK("flutter_kin_ecosystem_sdk"),
        FLUTTER_KIN_ECOSYSTEM_SDK_BALANCE("flutter_kin_ecosystem_sdk_balance"),
        FLUTTER_KIN_ECOSYSTEM_SDK_INFO("flutter_kin_ecosystem_sdk_info"),
        KIN_START("KinStart"),
        LAUNCH_KIN_MARKET("LaunchKinMarket"),
        GET_WALLET("GetWallet"),
        KIN_EARN("KinEarn"),
        KIN_SPEND("KinSpend"),
        KIN_PAY_TO_USER("KinPayToUser"),
        KIN_MIGRATION("KinMigration"),
        ORDER_CONFIRMATION("OrderConfirmation"),
        BALANCE_OBSERVER("BalanceObserver")
    }
}
