#import "FlutterKinEcosystemSdkPlugin.h"
#import <flutter_kin_ecosystem_sdk/flutter_kin_ecosystem_sdk-Swift.h>

@implementation FlutterKinEcosystemSdkPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterKinEcosystemSdkPlugin registerWithRegistrar:registrar];
}
@end
