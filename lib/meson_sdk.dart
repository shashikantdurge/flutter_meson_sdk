import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';

enum MesonLogLevel { NONE, ERROR, DEBUG }

const MethodChannel _channel = const MethodChannel('com.leher.meson_sdk');

class MesonSdk {
  static Map<int, MesonInterstitial> _mesonInterstitialObjects = {};

  /////consent value that is obtained from User
  static const MES_GDPR_CONSENT_AVAILABLE = 'gdpr_consent_available';

  /////0 if GDPR is not applicable and 1 if applicable
  static const MES_GDPR_CONSENT_GDPR_APPLIES = 'gdpr';

  /////user consent in IAB format <IAB String v1 or v2>
  static const MES_GDPR_CONSENT_IAB = 'gdpr_consent';

  static Future<void> initialize(
    String mesonAppId,
    //TODO this gdprConsent not yet interated
    Map<String, dynamic> gdprConsent,
  ) async {
    _setMethodHandler();
    final error =
        await _channel.invokeMethod('initialize', [mesonAppId, gdprConsent]);
    if (error == null) return null;
    throw error;
  }

  static void _setMethodHandler() {
    _channel.setMethodCallHandler((call) async {
      print('${call.method} ${call.arguments}');
      //First argument is object's hashcode, second is extra params
      List arguments = call.arguments;
      final ad = _mesonInterstitialObjects[arguments.first];

      switch (call.method) {
        case 'onAdImpression':
          ad?.onAdImpression?.call(ad, jsonDecode(arguments[1]));
          break;
        case 'onAdDisplayed':
          ad?.onAdDisplayed?.call(ad);
          break;
        case 'onAdDisplayFailed':
          ad?.onAdDisplayFailed?.call(ad);
          break;
        case 'onAdDismissed':
          ad?.onAdDismissed?.call(ad);
          break;
        case 'onUserLeftApplication':
          ad?.onUserLeftApplication?.call(ad);
          break;
        case 'onAdLoadSucceeded':
          ad?.onAdLoadSucceeded?.call(ad);
          break;
        case 'onAdLoadFailed':
          ad?.onAdLoadFailed?.call(ad, arguments[1]);
          break;
        case 'onRewardsUnlocked':
          ad?.onRewardsUnlocked?.call(ad, arguments[1]);
          break;
        case 'onAdClicked':
          ad?.onAdClicked?.call(ad, arguments[1]);
          break;
      }
    });
  }

  static Future<void> setLogLevel(MesonLogLevel logLevel) async {
    switch (logLevel) {
      case MesonLogLevel.NONE:
        await _channel.invokeMethod('MesonLogLevel.NONE');
        break;
      case MesonLogLevel.ERROR:
        await _channel.invokeMethod('MesonLogLevel.ERROR');
        break;
      case MesonLogLevel.DEBUG:
        await _channel.invokeMethod('MesonLogLevel.DEBUG');
        break;
    }
  }

  static Future<MesonInterstitial> createMesonInterstitial(
      String adUnitId) async {
    final objectHashCode =
        await _channel.invokeMethod('createMesonInterstitial', adUnitId);
    _mesonInterstitialObjects[objectHashCode] =
        MesonInterstitial._(adUnitId, objectHashCode);
    return _mesonInterstitialObjects[objectHashCode]!;
  }
}

class MesonInterstitial {
  final String adUnitId;
  final int nativeObjectHashcode;
  OnAdImpression? onAdImpression;
  OnAdDisplayed? onAdDisplayed;
  OnAdDisplayFailed? onAdDisplayFailed;
  OnAdDismissed? onAdDismissed;
  OnUserLeftApplication? onUserLeftApplication;
  OnAdLoadSucceeded? onAdLoadSucceeded;
  OnAdLoadFailed? onAdLoadFailed;
  OnRewardsUnlocked? onRewardsUnlocked;
  OnAdClicked? onAdClicked;

  MesonInterstitial._(this.adUnitId, this.nativeObjectHashcode);

  Future<void> setAdListener({
    OnAdImpression? onAdImpression,
    OnAdDisplayed? onAdDisplayed,
    OnAdDisplayFailed? onAdDisplayFailed,
    OnAdDismissed? onAdDismissed,
    OnUserLeftApplication? onUserLeftApplication,
    OnAdLoadSucceeded? onAdLoadSucceeded,
    OnAdLoadFailed? onAdLoadFailed,
    OnRewardsUnlocked? onRewardsUnlocked,
    OnAdClicked? onAdClicked,
  }) {
    this.onAdImpression = onAdImpression;
    this.onAdDisplayed = onAdDisplayed;
    this.onAdDisplayFailed = onAdDisplayFailed;
    this.onAdDismissed = onAdDismissed;
    this.onUserLeftApplication = onUserLeftApplication;
    this.onAdLoadSucceeded = onAdLoadSucceeded;
    this.onAdLoadFailed = onAdLoadFailed;
    this.onRewardsUnlocked = onRewardsUnlocked;
    this.onAdClicked = onAdClicked;
    return _channel.invokeMethod('setAdListener', nativeObjectHashcode);
  }

  Future<void> load() {
    return _channel.invokeMethod('load', nativeObjectHashcode);
  }

  Future<void> show() {
    return _channel.invokeMethod('show', nativeObjectHashcode);
  }

  Future<bool> isAdReady() async {
    return await _channel.invokeMethod('isAdReady', nativeObjectHashcode)
        as bool;
  }

  Future<void> destroy() {
    return _channel.invokeMethod('destroy', nativeObjectHashcode);
  }
}

typedef OnAdImpression = void Function(
    MesonInterstitial ad, Map impressionData);
typedef OnAdDisplayed = void Function(MesonInterstitial ad);
typedef OnAdDisplayFailed = void Function(MesonInterstitial ad);
typedef OnAdDismissed = void Function(MesonInterstitial ad);
typedef OnUserLeftApplication = void Function(MesonInterstitial ad);
typedef OnAdLoadSucceeded = void Function(MesonInterstitial ad);
typedef OnAdLoadFailed = void Function(MesonInterstitial ad, String message);
typedef OnRewardsUnlocked = void Function(MesonInterstitial ad, Map rewards);
typedef OnAdClicked = void Function(MesonInterstitial ad, Map params);
