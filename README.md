# flutter_kin_ecosystem_sdk

A flutter Kin Ecosystem SDK plugin to use offers features and launch Kin Marketplace.

Unofficial Kin Ecosystem SDK plugin written in Dart for Flutter.

## Usage
To use this plugin, add `flutter_kin_ecosystem_sdk` as a [dependency in your pubspec.yaml file](https://flutter.io/platform-plugins/).


```yaml
dependencies:
  flutter_kin_ecosystem_sdk: '^0.1.0'
```

### Initializing

``` dart
import 'package:flutter_kin_ecosystem_sdk/flutter_kin_ecosystem_sdk.dart';

// Generate jwt_token and all jwt by yourself and setting in the plugin to have a response
//userID - your application unique identifier for the user
//appID - your application unique identifier as provided by Kin.
// true - initializing balance observer
// true - production mode (false - playground)
await FlutterKinEcosystemSdk.kinStart(jwt_token, userId, appId, true, true);
```

### Receivers

To receive some changes in plugin you can use such ones:

``` dart
// Receive balance scream and get all balance changes
FlutterKinEcosystemSdk.balanceStream.receiveBroadcastStream().listen((balance) {
    print(balance);
});

// Receive all info and error messages from plugin
FlutterKinEcosystemSdk.infoStream.receiveBroadcastStream().listen((jsonStr) {
    print(jsonStr);
});
```

### Some methods

``` dart
// A custom Earn offer allows your users to earn Kin
// as a reward for performing tasks you want to incentives,
// such as setting a profile picture or rating your app
FlutterKinEcosystemSdk.kinEarn(jwt);

// A custom Spend offer allows your users to unlock unique spend opportunities
// that you define within your app
FlutterKinEcosystemSdk.kinSpend(jwt);

// A custom pay to user offer allows your users to unlock
// unique spend opportunities that you define
// within your app offered by other users
FlutterKinEcosystemSdk.kinPayToUser(jwt);
```

## Installation


### Android and iOS

No configuration required - the plugin should work out of the box.