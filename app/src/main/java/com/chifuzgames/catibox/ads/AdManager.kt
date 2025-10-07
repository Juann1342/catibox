package com.chifuzgames.catibox.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.LoadAdError

object AdManager {

    //  Banner reutilizable
    var bannerView: AdView? = null

    //  Intersticial (pantalla completa)
    var interstitialAd: InterstitialAd? = null

    //  Recompensado (por ejemplo, para revivir al gato 😺)
    var rewardedAd: RewardedAd? = null

    private val bannerId = "ca-app-pub-3940256099942544/6300978111" // ID de prueba
    private val interstitialId = "ca-app-pub-3940256099942544/1033173712"
    private val rewardedId = "ca-app-pub-3940256099942544/5224354917"

    fun initialize(context: Context) {
        loadBanner(context)
        loadInterstitial(context)
        loadRewarded(context)
    }

    fun loadBanner(context: Context) {
        if (bannerView == null) {
            bannerView = AdView(context).apply {
                adUnitId = bannerId
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }
    }

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, interstitialId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
            }
        })
    }

    fun loadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, rewardedId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
            }
        })
    }
}
