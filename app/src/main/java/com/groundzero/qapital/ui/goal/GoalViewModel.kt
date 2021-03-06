package com.groundzero.qapital.ui.goal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.groundzero.qapital.data.cache.Cache
import com.groundzero.qapital.data.persistence.goal.GoalDao
import com.groundzero.qapital.data.remote.goal.Goal
import com.groundzero.qapital.data.remote.goal.GoalRepository
import com.groundzero.qapital.data.remote.goal.Goals
import com.groundzero.qapital.data.response.Response
import com.groundzero.qapital.utils.NetworkUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class GoalViewModel(
    private val goalRepository: GoalRepository,
    private val goalDao: GoalDao,
    private val networkUtils: NetworkUtils
) : ViewModel(), Cache<Goals> {

    private val goals = MutableLiveData<Response<Goal>>()
    private val selectedGoal = MutableLiveData<Goal>()
    private lateinit var disposable: Disposable

    fun getRemoteGoals(): LiveData<Response<Goal>> {

                goals.value = Response.loading()

                if (networkUtils.isNetworkConnected()) {
                    disposable = goalRepository.getGoals()
                        .subscribeOn(Schedulers.io())
                        .doOnError { e -> Log.e("errors", e.message) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { response -> setRemoteLiveDataAndCacheData(response) },
                            { throwable -> setCachedLiveData(throwable) }
                        )
                } else {
                    setCachedLiveData(Throwable())
        }
        return goals
    }

    fun setRemoteLiveDataAndCacheData(response: Goals) {
        goals.value = Response.success(response.savingsGoals)
        cacheData(response)
    }

    fun setCachedLiveData(throwable: Throwable?) {
        val cachedGoals: Goals? = getCachedData(null)
        if (cachedGoals != null) goals.value = Response.success(cachedGoals.savingsGoals)
        else goals.value = Response.error(throwable!!)
    }

    override fun getCachedData(id: Int?): Goals? {
        return goalDao.getGoals()
    }

    override fun cacheData(goals: Goals) {
        goalDao.addGoals(goals)
    }

    fun setSelectedGoalLiveData(goal: Goal) {
        selectedGoal.value = goal
    }

    fun getSelectedGoalLiveData(): LiveData<Goal> = selectedGoal


    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}