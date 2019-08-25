package com.groundzero.qapital.ui.details

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.groundzero.qapital.data.cache.Cache
import com.groundzero.qapital.data.persistence.details.DetailsDao
import com.groundzero.qapital.data.remote.details.Detail
import com.groundzero.qapital.data.remote.details.Details
import com.groundzero.qapital.data.remote.details.DetailsRepository
import com.groundzero.qapital.data.response.Response
import com.groundzero.qapital.utils.secondsInDay
import com.groundzero.qapital.utils.secondsPassed
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class DetailsViewModel(
    private val detailsRepository: DetailsRepository,
    private val detailsDao: DetailsDao
) : ViewModel(), Cache<Details> {

    override fun getCachedData(id: Int?): Details? {
        return detailsDao.getDetails(id!!)
    }

    private var disposable = CompositeDisposable()
    private val details = MutableLiveData<Response<Detail>>()
    private val weekEarnings = MutableLiveData<Float>()
    private val totalEarnings = MutableLiveData<Float>()

    fun getDetails(goalId: Int): LiveData<Response<Detail>> {
        details.value = Response.loading()
        val detailsObserver: Single<Details> = detailsRepository.getDetails(goalId)
        disposable.add(getDetailsFeed(goalId, detailsObserver))
        disposable.add(getWeeklyEarningsObserver(detailsObserver))
        disposable.add(getTotalEarningsObserver(detailsObserver))
        return details
    }

    private fun getDetailsFeed(goalId: Int, detailsObserver: Single<Details>): Disposable {
        return detailsObserver
            .subscribeOn(Schedulers.io())
            .doOnError { e -> Log.e("errors", e.message) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response -> setAndCacheFetchedData(goalId, response) },
                { throwable -> setCachedData(goalId, throwable) }
            )
    }

    private fun getWeeklyEarningsObserver(detailsObserver: Single<Details>): Disposable {
        return detailsObserver
            .map { details: Details ->
                details.details
                    .filter { detail: Detail -> detail.timestamp.secondsPassed() < secondsInDay * 7 }
                    .map { detail -> detail.amount }
                    .sum()
            }
            .subscribeOn(Schedulers.io())
            .doOnError { e -> Log.e("errors", e.message) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { weekEarnings -> this.weekEarnings.value = weekEarnings },
                { this.weekEarnings.value = 0.0f }
            )
    }

    private fun getTotalEarningsObserver(detailsObserver: Single<Details>): Disposable {
        return detailsObserver
            .map { t: Details ->
                t.details
                    .map { detail -> detail.amount }
                    .sum()
            }
            .subscribeOn(Schedulers.io())
            .doOnError { e -> Log.e("errors", e.message) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { totalEarnings -> onSuccessTotalEarnings(totalEarnings) },
                { onErrorTotalEarnings() }
            )
    }

    fun setAndCacheFetchedData(goalId: Int, response: Details) {
        details.value = Response.success(response.details)
        cacheData(response.apply { id = goalId })
    }

    fun setCachedData(goalId: Int, throwable: Throwable?) {
        val cachedGoals: Details? = getCachedData(goalId)
        if (cachedGoals != null) details.value = Response.success(cachedGoals.details)
        else details.value = Response.error(throwable!!)
    }

    override fun cacheData(t: Details) {
        detailsDao.addDetail(t)
    }


    /**
     * Earnings text and progress bar are being animated.
     */
    private fun onSuccessTotalEarnings(totalEarnings: Float) {
        Thread {
            var earningsIterator = 0.0f
            for (x in 0..totalEarnings.toInt()) {
                Thread.sleep(30)
                this.totalEarnings.postValue(earningsIterator++)
            }
        }.start()
    }

    private fun onErrorTotalEarnings() {
        this.totalEarnings.value = 0.0f
    }

    fun getTotalEarningsProgression(totalEarnings: Float, targetEarnings: Float): Int {
        return ((totalEarnings / targetEarnings) * 100).toInt()
    }

    fun getWeekEarnings(): LiveData<Float> {
        return weekEarnings
    }

    fun getTotalEarnings(): LiveData<Float> {
        return totalEarnings
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}