import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_kin_ecosystem_sdk/utils.dart';

class FlutterKinEcosystemSdk {
  static MethodChannel _methodChannel =
      MethodChannel('flutter_kin_ecosystem_sdk');

  static const _streamBalance =
      const EventChannel('flutter_kin_ecosystem_sdk_balance');
  static const _streamInfo =
      const EventChannel('flutter_kin_ecosystem_sdk_info');

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
    final String address =
        await _methodChannel.invokeMethod('getPublicAddress');
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
    await _methodChannel.invokeMethod('kinStart', params);
  }

  static Future launchKinMarket() async {
    await _methodChannel.invokeMethod('launchKinMarket');
  }

  static Future kinEarn(String jwt) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'jwt': jwt,
    };
    await _methodChannel.invokeMethod('kinEarn', params);
  }

  static Future kinSpend(String jwt) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'jwt': jwt,
    };
    await _methodChannel.invokeMethod('kinSpend', params);
  }

  static Future kinPayToUser(String jwt) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'jwt': jwt,
    };
    await _methodChannel.invokeMethod('kinPayToUser', params);
  }

  static Future orderConfirmation(String offerId) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'offerId': offerId,
    };
    await _methodChannel.invokeMethod('orderConfirmation', params);
  }
}
