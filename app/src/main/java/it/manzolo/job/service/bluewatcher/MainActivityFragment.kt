package it.manzolo.job.service.bluewatcher

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main.*

/**
 * A placeholder fragment containing a simple view.
 */
class MainActivityFragment : Fragment() {

    companion object {
        val TAG: String = MainActivityFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(it.manzolo.job.service.bluewatcher.R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.setOnClickListener {

            startJobService(1)
        }

    }

    private fun startJobService(i: Int) {

        Log.d(it.manzolo.job.service.bluewatcher.MainActivityFragment.Companion.TAG, "startJobService")

        it.manzolo.job.service.bluewatcher.App.Companion.scheduleJobService(activity as Context)

    }
}
