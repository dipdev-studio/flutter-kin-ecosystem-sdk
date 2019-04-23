import Flutter
import UIKit
import KinDevPlatform

public class SwiftFlutterKinEcosystemSdkPlugin: NSObject, FlutterPlugin {
    var isKinInit: Bool = false
    var currentBalance: Int = 0
    var initBalanceObserver: Bool?
    
    static let balanceFlutterController = FlutterStreamController()
    static let infoFlutterController = FlutterStreamController()
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: Constants.FLUTTER_KIN_ECOSYSTEM_SDK.rawValue, binaryMessenger: registrar.messenger())
        let balanceEventChannel = FlutterEventChannel.init(name: Constants.FLUTTER_KIN_ECOSYSTEM_SDK_BALANCE.rawValue, binaryMessenger: registrar.messenger())
        let infoEventChannel = FlutterEventChannel.init(name: Constants.FLUTTER_KIN_ECOSYSTEM_SDK_INFO.rawValue, binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterKinEcosystemSdkPlugin()
        balanceEventChannel.setStreamHandler(balanceFlutterController)
        infoEventChannel.setStreamHandler(infoFlutterController)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if(call.method.elementsEqual(Constants.KIN_START.rawValue)){
            let arguments = call.arguments as? NSDictionary
            let token = arguments!["token"] as? String
            let userId = arguments!["userId"] as? String
            let appId = arguments!["appId"] as? String
            let initBalanceObserver = arguments!["initBalanceObserver"] as? Bool
            let isProduction = arguments!["isProduction"] as? Bool
            if (token == nil || userId == nil || appId == nil || initBalanceObserver == nil || isProduction == nil) {return}
            let environment : Environment
            if (isProduction!){
                environment = .production
            }else{
                environment = .playground
            }
            Kin.shared.migrationDelegate = self
            do {
                try Kin.shared.start(userId: userId!, appId: appId!, jwt: token, environment: environment)
                sendReport(type: Constants.KIN_START.rawValue, message: "Kin started")
                isKinInit = true
                initializeBalanceObserver(initBalanceObserver: initBalanceObserver!)
            } catch {
                isKinInit = false
                sendError(type: Constants.KIN_START.rawValue, error: error)
            }
        }
        if(call.method.elementsEqual(Constants.LAUNCH_KIN_MARKET.rawValue)){
            if (!ifKinInit()) {return}
            let viewController = (UIApplication.shared.delegate?.window??.rootViewController)!;
            Kin.shared.launchMarketplace(from: viewController)
        }
        if(call.method.elementsEqual(Constants.GET_WALLET.rawValue)){
            if (!ifKinInit()) {return}
            result(Kin.shared.publicAddress)
        }
        if(call.method.elementsEqual(Constants.KIN_EARN.rawValue)){
            if (!ifKinInit()) {return}
            let arguments = call.arguments as? NSDictionary
            let jwt = arguments!["jwt"] as? String
            kinEarn(jwt: jwt!)
        }
        if(call.method.elementsEqual(Constants.KIN_SPEND.rawValue)){
            if (!ifKinInit()) {return}
            let arguments = call.arguments as? NSDictionary
            let jwt = arguments!["jwt"] as? String
            kinSpend(jwt: jwt!)
        }
        if(call.method.elementsEqual(Constants.KIN_PAY_TO_USER.rawValue)){
            if (!ifKinInit()) {return}
            let arguments = call.arguments as? NSDictionary
            let jwt = arguments!["jwt"] as? String
            kinPayToUser(jwt: jwt!)
        }
        if(call.method.elementsEqual(Constants.ORDER_CONFIRMATION.rawValue)){
            let err = PluginError(type: Constants.ORDER_CONFIRMATION.rawValue, message: "Kin SDK iOS doesn't support function orderConfirmation.")
            sendError(code: "-2", message: "Kin SDK iOS doesn't support function orderConfirmation.", details: err)
        }
    }
    
    private func initializeBalanceObserver(initBalanceObserver: Bool){
        if (initBalanceObserver){
            do {
                _ = try Kin.shared.addBalanceObserver { kinBalance in
                    self.currentBalance = (kinBalance.amount as NSDecimalNumber).intValue
                    SwiftFlutterKinEcosystemSdkPlugin.balanceFlutterController.eventCallback?(self.currentBalance)
                }
            } catch {
                self.sendError(type: Constants.BALANCE_OBSERVER.rawValue, error: error)
            }
        }
    }
    
    private func kinEarn(jwt : String){
        let prevBalance = currentBalance
        let handler: ExternalOfferCallback = { jwtConfirmation, error in
            if jwtConfirmation != nil {
                self.sendReport(type: Constants.KIN_EARN.rawValue, message: String(describing: jwtConfirmation), amount: self.currentBalance - prevBalance)
            } else if let e = error {
                self.sendError(type: Constants.KIN_EARN.rawValue, error: e)
            }
        }
        _ = Kin.shared.requestPayment(offerJWT: jwt, completion: handler)
    }
    
    private func kinSpend(jwt : String){
        _ = Kin.shared.purchase(offerJWT: jwt) { jwtConfirmation, error in
            if jwtConfirmation != nil {
                self.sendReport(type: Constants.KIN_SPEND.rawValue, message: String(describing: jwtConfirmation))
            } else if let e = error {
                self.sendError(type: Constants.KIN_SPEND.rawValue, error: e)
            }
        }
    }
    
    private func kinPayToUser(jwt : String){
        _ = Kin.shared.payToUser(offerJWT: jwt) { jwtConfirmation, error in
            if jwtConfirmation != nil {
                self.sendReport(type: Constants.KIN_PAY_TO_USER.rawValue, message: String(describing: jwtConfirmation))
            } else if let e = error {
                self.sendReport(type: Constants.KIN_PAY_TO_USER.rawValue, message: String(describing: e))
            }
        }
    }
    
    private func sendReport(type: String, message: String, amount: Int? = nil){
        var info: Info
        if (amount != nil){
            info = Info(type: type, message: message, amount: amount)
        }else{
            info = Info(type: type, message: message)
        }
        let encoder = JSONEncoder()
        var data: Data? = nil
        do {
            data = try encoder.encode(info)
        } catch {
            sendError(type: "json", error: error)
        }
        if (data != nil) {
            SwiftFlutterKinEcosystemSdkPlugin.infoFlutterController.eventCallback?(String(data: data!, encoding: .utf8)!)
        }
    }
    
    private func sendError(type: String, error: Error) {
        let err = PluginError(type: type, message: error.localizedDescription)
        var message: String? = error.localizedDescription
        if (message == nil) {message = ""}
        sendError(code: "-3", message: message!, details: err)
    }
    
    private func sendError(code: String, message: String?, details: PluginError) {
        let encoder = JSONEncoder()
        var data: Data? = nil
        do {
            data = try encoder.encode(details)
        } catch {
            sendError(type: "json", error: error)
        }
        if (data != nil) {
            SwiftFlutterKinEcosystemSdkPlugin.infoFlutterController.error(code, message: message, details: String(data: data!, encoding: .utf8)!)
        }
    }
    
    class FlutterStreamController : NSObject, FlutterStreamHandler {
        var eventCallback: FlutterEventSink?
        
        public func error(_ code : String, message: String?, details: Any?) {
            eventCallback?(FlutterError(code: code, message: message, details: details))
        }
        
        public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
            eventCallback = events
            return nil
        }
        
        public func onCancel(withArguments arguments: Any?) -> FlutterError? {
            return nil
        }
    }
    
    private func ifKinInit() -> Bool{
        if(!isKinInit){
            let err = PluginError(type: Constants.KIN_START.rawValue, message: "Kin SDK not started")
            sendError(code: "-1", message: "Kin SDK not started", details: err)
        }
        return isKinInit
    }
    
    struct Info:Encodable {
        let type: String
        let message: String
        let amount: Int?
        init(type: String, message: String, amount: Int? = nil) {
            self.type = type
            self.message = message
            self.amount = amount
        }
    }
    
    struct PluginError:Encodable {
        let type: String
        let message: String
    }
}

extension SwiftFlutterKinEcosystemSdkPlugin: KinMigrationDelegate {
    public func kinMigrationDidStart() {
        sendReport(type: Constants.KIN_MIGRATION.rawValue, message: "migrationStart")
    }
    
    public func kinMigrationDidFinish() {
        sendReport(type: Constants.KIN_MIGRATION.rawValue, message: "migrationFinish")
        initializeBalanceObserver(initBalanceObserver: initBalanceObserver!)
    }
    
    public func kinMigrationIsReady() {
        sendReport(type: Constants.KIN_MIGRATION.rawValue, message: "migrationReady")
    }
    
    public func kinMigration(error: Error) {
        let err = PluginError(type: Constants.KIN_MIGRATION.rawValue, message: error.localizedDescription)
        sendError(code: "-2", message: "Migration failed", details: err)
    }
    
    enum Constants: String {
        case FLUTTER_KIN_ECOSYSTEM_SDK = "flutter_kin_ecosystem_sdk"
        case FLUTTER_KIN_ECOSYSTEM_SDK_BALANCE = "flutter_kin_ecosystem_sdk_balance"
        case FLUTTER_KIN_ECOSYSTEM_SDK_INFO = "flutter_kin_ecosystem_sdk_info"
        case KIN_START = "KinStart"
        case LAUNCH_KIN_MARKET = "LaunchKinMarket"
        case GET_WALLET = "GetWallet"
        case KIN_EARN = "KinEarn"
        case KIN_SPEND = "KinSpend"
        case KIN_PAY_TO_USER = "KinPayToUser"
        case KIN_MIGRATION = "KinMigration"
        case ORDER_CONFIRMATION = "OrderConfirmation"
        case BALANCE_OBSERVER = "BalanceObserver"
    }
}
