package com.leher.meson_sdk

import ai.meson.ads.MesonAdRequestStatus
import ai.meson.ads.MesonInterstitial
import ai.meson.ads.listeners.MesonInterstitialAdListener
import ai.meson.common.sdk.BaseMesonInit
import ai.meson.sdk.MesonSdk
import ai.meson.sdk.MesonSdkConfiguration
import ai.meson.sdk.MesonSdkInitializationListener
import android.app.Activity
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject


/** MesonSdkPlugin */
class MesonSdkPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {
    private var activity: Activity? = null
    private var mesonObjects: HashMap<Int, MesonInterstitial> = HashMap()

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    val listener = object : MesonInterstitialAdListener() {
        override fun onAdImpression(
            ad: MesonInterstitial,
            impressionData: JSONObject?
        ) {
            Log.i("LEHER", "onAdImpression $ad $impressionData")
            channel.invokeMethod("onAdImpression", listOf(ad.hashCode(), impressionData.toString()))
        }

        override fun onAdDisplayed(ad: MesonInterstitial) {
            Log.i("LEHER", "onAdDisplayed $ad")
            channel.invokeMethod("onAdDisplayed", listOf(ad.hashCode()))
        }

        override fun onAdDisplayFailed(ad: MesonInterstitial) {
            Log.i("LEHER", "onAdDisplayFailed $ad")
            channel.invokeMethod("onAdDisplayFailed", listOf(ad.hashCode()))
        }

        override fun onAdDismissed(ad: MesonInterstitial) {
            Log.i("LEHER", "onAdDismissed $ad")
            channel.invokeMethod("onAdDismissed", listOf(ad.hashCode()))
        }

        override fun onUserLeftApplication(ad: MesonInterstitial) {
            Log.i("LEHER", "onUserLeftApplication $ad")
            channel.invokeMethod("onUserLeftApplication", listOf(ad.hashCode()))
        }

        override fun onAdLoadSucceeded(ad: MesonInterstitial) {
            Log.i("LEHER", "onAdLoadSucceeded $ad")
            channel.invokeMethod("onAdLoadSucceeded", listOf(ad.hashCode()))
        }

        override fun onAdLoadFailed(
            ad: MesonInterstitial,
            status: MesonAdRequestStatus
        ) {
            Log.i("LEHER", "onAdLoadFailed $ad ${status.message}")
            channel.invokeMethod("onAdLoadFailed", listOf(ad.hashCode(), status.message))
        }

        override fun onAdClicked(ad: MesonInterstitial, params: HashMap<String, Any>) {
            Log.i("LEHER", "onAdClicked $ad $params")
            channel.invokeMethod("onAdClicked", listOf(ad.hashCode(), params))
        }

        override fun onRewardsUnlocked(ad: MesonInterstitial, rewards: Map<Any, Any>?) {
            Log.i("LEHER", "onRewardsUnlocked $ad $rewards")
            channel.invokeMethod("onRewardsUnlocked", listOf(ad.hashCode(), rewards))
        }

    }


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        MesonSdk.setLogLevel(BaseMesonInit.LogLevel.DEBUG)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.leher.meson_sdk")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.i("LEHER", "onMethodCall ${call.method} ${call.arguments}")
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "initialize" -> {
                val mesonAppId = (call.arguments as List<*>)[0]
                Log.i("LEHER", "Meson App ID $mesonAppId")
                //            val gdprConsent = (call.arguments as List<*>)[1]
                initialize(mesonAppId as String, result)
            }
            "createMesonInterstitial" -> {
                createMesonInterstitial(call.arguments as String, result)
            }
            "setAdListener" -> {
                setAdListener(mesonObjects[call.arguments as Int]!!, result)
            }
            "load" -> {
                load(mesonObjects[call.arguments as Int]!!, result)
            }
            "show" -> {
                show(mesonObjects[call.arguments as Int]!!, result)
            }
            "isAdReady" -> {
                isAdReady(mesonObjects[call.arguments as Int]!!, result)
            }
            "destroy" -> {
                destroy(mesonObjects[call.arguments as Int]!!, result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun initialize(mesonAppId: String, @NonNull result: MethodChannel.Result) {
        if (activity == null) {
            result.error("NULL_CONTEXT", "Context unavailable", "Context unavailable");
            return
        }
        val gdprConsent = JSONObject().run {
            put(
                MesonSdk.MES_GDPR_CONSENT_AVAILABLE,
                true
            ) //consent value that is obtained from User
            put(
                MesonSdk.MES_GDPR_CONSENT_GDPR_APPLIES,
                "0"
            ) //0 if GDPR is not applicable and 1 if applicable
            put(
                MesonSdk.MES_GDPR_CONSENT_IAB,
                "v2"
            )//user consent in IAB format
        }

        val mesonSdkConfiguration =
            MesonSdkConfiguration.Builder(activity!!, "354414df-7f28-4a1f-96c6-8cfc92cd5c25")
                .setConsent(gdprConsent).build()
        /*Initialize Meson SDK*/
        MesonSdk.initialize(mesonSdkConfiguration, object : MesonSdkInitializationListener {
            override fun onComplete(error: Error?) {
                Log.i("LEHER", "MesonSdk.initialize: onComplete ${error?.message}")
                if (error == null) {
                    result.success(null)
                } else {
                    result.error("INITIALIZE_FAILED", error.message, error.localizedMessage)
                    //continue with publisher integration
                }
            }
        })
    }

    private fun createMesonInterstitial(adUnitId: String, @NonNull result: MethodChannel.Result) {
        if (activity == null) {
            result.error("NULL_CONTEXT", "Context unavailable", "Context unavailable");
            return
        }
        val mesonInterstitial = MesonInterstitial(activity!!, adUnitId)
        mesonObjects[mesonInterstitial.hashCode()] = mesonInterstitial
        result.success(mesonInterstitial.hashCode())
    }

    private fun setAdListener(
        mesonInterstitial: MesonInterstitial,
        @NonNull result: MethodChannel.Result
    ) {
        mesonInterstitial.setAdListener(listener)
        result.success(null)
    }

    private fun load(mesonInterstitial: MesonInterstitial, @NonNull result: MethodChannel.Result) {
        mesonInterstitial.load()
        result.success(null)
    }

    private fun show(mesonInterstitial: MesonInterstitial, @NonNull result: MethodChannel.Result) {
        mesonInterstitial.show()
        result.success(null)
    }

    private fun isAdReady(
        mesonInterstitial: MesonInterstitial,
        @NonNull result: MethodChannel.Result
    ) {
        result.success(mesonInterstitial.isAdReady())
    }

    private fun destroy(
        mesonInterstitial: MesonInterstitial,
        @NonNull result: MethodChannel.Result
    ) {
        mesonInterstitial.destroy()
        mesonObjects.remove(mesonInterstitial.hashCode())
        result.success(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.i("MESON_FLUTTER", "onAttachedToActivity $activity");
        this.activity = binding.getActivity();
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.i("MESON_FLUTTER", "onDetachedFromActivityForConfigChanges $activity");
        this.activity = null;
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.i("MESON_FLUTTER", "onReattachedToActivityForConfigChanges $activity");
        this.activity = binding.getActivity();
    }

    override fun onDetachedFromActivity() {
        Log.i("MESON_FLUTTER", "onDetachedFromActivity $activity");
        this.activity = null;
    }
}
