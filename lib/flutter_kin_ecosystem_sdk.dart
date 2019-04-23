import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_kin_ecosystem_sdk/utils.dart';

class FlutterKinEcosystemSdk {
  static MethodChannel _methodChannel =
      MethodChannel(FlutterKinEcosystemSDKConstans.FLUTTER_KIN_ECOSYSTEM_SDK);

  static const _streamBalance = const EventChannel(
      FlutterKinEcosystemSDKConstans.FLUTTER_KIN_ECOSYSTEM_SDK_BALANCE);
  static const _streamInfo = const EventChannel(
      FlutterKinEcosystemSDKConstans.FLUTTER_KIN_ECOSYSTEM_SDK_INFO);

  static StreamController<Info> _streamInfoController =
      new StreamController.broadcast();

  static StreamController<int> _streamBalanceController =
      new StreamController.broadcast();

  static initStreams() {
    _streamInfo.receiveBroadcastStream().listen((data) {
      Info info = Info.fromJson(json.decode(data));
      _streamInfoController.add(info);
    }, onError: (error) {
      Error err = new Error(
          error.code, error.message, Info.fromJson(json.decode(error.details)));
      throw err;
    });

    _streamBalance.receiveBroadcastStream().listen((data) {
      _streamBalanceController.add(int.parse(data.toString()));
    }, onError: (error) {
      throw error;
    });
  }

  static Future<String> get getPublicAddress async {
    final String address = await _methodChannel
        .invokeMethod(FlutterKinEcosystemSDKConstans.GET_WALLET);
    return address;
  }

  static StreamController<int> get balanceStream {
    return _streamBalanceController;
  }

  static StreamController<Info> get infoStream {
    return _streamInfoController;
  }

  static Future kinStart(String token, String userId, String appId,
      bool initBalanceObserver, bool isProduction) async {
    initStreams();
    final Map<String, dynamic> params = <String, dynamic>{
      'token': token,
      'userId': userId,
      'appId': appId,
      'initBalanceObserver': initBalanceObserver,
      'isProduction': isProduction,
    };
    await _methodChannel.invokeMethod(
        FlutterKinEcosystemSDKConstans.KIN_START, params);
  }

  static Future launchKinMarket() async {
    await _methodChannel
        .invokeMethod(FlutterKinEcosystemSDKConstans.LAUNCH_KIN_MARKET);
  }

  static Future kinEarn(String jwt) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'jwt': jwt,
    };
    await _methodChannel.invokeMethod(
        FlutterKinEcosystemSDKConstans.KIN_EARN, params);
  }

  static Future kinSpend(String jwt) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'jwt': jwt,
    };
    await _methodChannel.invokeMethod(
        FlutterKinEcosystemSDKConstans.KIN_SPEND, params);
  }

  static Future kinPayToUser(String jwt) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'jwt': jwt,
    };
    await _methodChannel.invokeMethod(
        FlutterKinEcosystemSDKConstans.KIN_PAY_TO_USER, params);
  }

  static Future orderConfirmation(String offerId) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'offerId': offerId,
    };
    await _methodChannel.invokeMethod(
        FlutterKinEcosystemSDKConstans.ORDER_CONFIRMATION, params);
  }
}

class FlutterKinEcosystemSDKConstans {
  static const String FLUTTER_KIN_SDK = 'flutter_kin_sdk';
  static const String FLUTTER_KIN_ECOSYSTEM_SDK = "flutter_kin_ecosystem_sdk";
  static const String FLUTTER_KIN_ECOSYSTEM_SDK_BALANCE =
      "flutter_kin_ecosystem_sdk_balance";
  static const String FLUTTER_KIN_ECOSYSTEM_SDK_INFO =
      "flutter_kin_ecosystem_sdk_info";
  static const String KIN_START = "KinStart";
  static const String LAUNCH_KIN_MARKET = "LaunchKinMarket";
  static const String GET_WALLET = "GetWallet";
  static const String KIN_EARN = "KinEarn";
  static const String KIN_SPEND = "KinSpend";
  static const String KIN_PAY_TO_USER = "KinPayToUser";
  static const String KIN_MIGRATION = "KinMigration";
  static const String ORDER_CONFIRMATION = "OrderConfirmation";
  static const String BALANCE_OBSERVER = "BalanceObserver";
}
