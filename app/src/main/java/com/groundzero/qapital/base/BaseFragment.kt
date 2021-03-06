package com.groundzero.qapital.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.groundzero.qapital.application.CustomApplication
import com.groundzero.qapital.ui.common.ViewModelFactory
import com.groundzero.qapital.ui.goal.GoalViewModel
import javax.inject.Inject

open class BaseFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    lateinit var goalViewModel: GoalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as CustomApplication).getApplicationComponent().inject(this)
        goalViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(GoalViewModel::class.java)
    }

    fun adjustedRecyclerView(recyclerView: RecyclerView): RecyclerView {
        recyclerView.setHasFixedSize(true)
        val linearLayout = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayout
        return recyclerView
    }

    val activityCallback: MainActivityCallback get() = activity as MainActivityCallback
}