package com.deniscerri.ytdlnis.ui.more.terminal

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.deniscerri.ytdlnis.R
import com.deniscerri.ytdlnis.adapter.TerminalDownloadsAdapter
import com.deniscerri.ytdlnis.database.models.TerminalItem
import com.deniscerri.ytdlnis.database.viewmodel.TerminalViewModel
import com.deniscerri.ytdlnis.util.UiUtil.enableFastScroll
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class TerminalDownloadsListFragment : Fragment(), TerminalDownloadsAdapter.OnItemClickListener {
    private var topAppBar: MaterialToolbar? = null
    private lateinit var terminalViewModel: TerminalViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        terminalViewModel = ViewModelProvider(this)[TerminalViewModel::class.java]
        return inflater.inflate(R.layout.fragment_terminal_download_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            val recycler = view.findViewById<RecyclerView>(R.id.terminal_recycler)
            val adapter = TerminalDownloadsAdapter(this@TerminalDownloadsListFragment, requireActivity())
            recycler.adapter = adapter
            recycler.enableFastScroll()
            recycler.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(R.integer.grid_size))
            topAppBar = requireActivity().findViewById(R.id.custom_command_toolbar)
            topAppBar!!.setNavigationOnClickListener { requireActivity().finish() }
            topAppBar?.menu?.forEach { it.isVisible = false }
            topAppBar?.menu?.get(0)?.isVisible = true

            topAppBar?.setOnMenuItemClickListener { m: MenuItem ->
                when(m.itemId){
                    R.id.add -> {
                        findNavController().navigate(R.id.terminalFragment)
                    }
                }
                true
            }

            terminalViewModel.getTerminals().collectLatest {
                adapter.submitList(it)
                if (it.isEmpty()){
                    findNavController().navigate(R.id.terminalFragment)
                }
            }
        }

        WorkManager.getInstance(requireContext())
            .getWorkInfosByTagLiveData("terminal")
            .observe(viewLifecycleOwner){ list ->
                list.forEach {work ->
                    if (work == null) return@forEach
                    val id = work.progress.getInt("id", 0)
                    if(id == 0) return@forEach

                    val progress = work.progress.getInt("progress", 0)
                    val output = work.progress.getString("output")

                    val progressBar = view.findViewWithTag<LinearProgressIndicator>("$id##progress")
                    val outputText = view.findViewWithTag<TextView>("$id##output")

                    requireActivity().runOnUiThread {
                        kotlin.runCatching {
                            progressBar?.setProgressCompat(progress, true)
                            outputText?.text = output
                        }
                    }
                }
            }
    }
    override fun onCancelClick(itemID: Long) {
        terminalViewModel.cancelTerminalDownload(itemID)
    }

    override fun onCardClick(item: TerminalItem) {
        val bundle = Bundle()
        bundle.putLong("id", item.id)
        findNavController().navigate(R.id.terminalFragment, bundle)
    }
}