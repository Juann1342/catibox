package com.chifuzgames.catibox.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem

object AdManager {

    // Banner reutilizable
    var bannerView: AdView? = null

    // Interstitial y Rewarded
    var interstitialAd: InterstitialAd? = null
    var rewardedAd: RewardedAd? = null

    private val bannerId = "ca-app-pub-3940256099942544/6300978111" // Banner de prueba
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

    // 👉 Mostrar interstitial
    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null && !activity.isFinishing && !activity.isDestroyed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onAdClosed() // continuar después de cerrar el anuncio
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onAdClosed() // continuar igual si falla
                }

                override fun onAdShowedFullScreenContent() {
                    // Opcional: podés loguear o pausar música/juego
                }
            }

            try {
                ad.show(activity)
            } catch (e: Exception) {
                // Previene errores tipo IllegalArgumentException o WindowLeaked
                interstitialAd = null
                loadInterstitial(activity)
                onAdClosed()
            }

        } else {
            // Si no hay anuncio cargado o la activity está cerrándose, continuar igual
            onAdClosed()
            loadInterstitial(activity)
        }
    }


    // 👉 Mostrar rewarded
    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit) {
        val ad = rewardedAd
        if (ad != null && !activity.isFinishing && !activity.isDestroyed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewarded(activity)
                    onRewardEarned() // continuar igual si falla
                }
            }
            ad.show(activity) { _: RewardItem ->
                onRewardEarned()
            }
        } else {
            onRewardEarned()
            loadRewarded(activity)
        }
    }

}
