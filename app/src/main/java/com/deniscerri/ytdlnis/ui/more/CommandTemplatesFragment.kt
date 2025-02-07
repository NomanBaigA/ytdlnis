package com.deniscerri.ytdlnis.ui.more

import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deniscerri.ytdlnis.MainActivity
import com.deniscerri.ytdlnis.R
import com.deniscerri.ytdlnis.adapter.TemplatesAdapter
import com.deniscerri.ytdlnis.database.models.CommandTemplate
import com.deniscerri.ytdlnis.database.viewmodel.CommandTemplateViewModel
import com.deniscerri.ytdlnis.util.FileUtil
import com.deniscerri.ytdlnis.util.UiUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CommandTemplatesFragment : Fragment(), TemplatesAdapter.OnItemClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var templatesAdapter: TemplatesAdapter
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var commandTemplateViewModel: CommandTemplateViewModel
    private lateinit var templatesList: List<CommandTemplate>
    private lateinit var noResults: RelativeLayout
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = activity as MainActivity
        return inflater.inflate(R.layout.fragment_command_templates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        templatesList = listOf()

        topAppBar = view.findViewById(R.id.logs_toolbar)
        topAppBar.setNavigationOnClickListener { mainActivity.onBackPressedDispatcher.onBackPressed() }
        noResults = view.findViewById(R.id.no_results)

        templatesAdapter =
            TemplatesAdapter(
                this,
                mainActivity
            )
        recyclerView = view.findViewById(R.id.template_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = templatesAdapter
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (preferences.getBoolean("swipe_gestures", true)){
            val itemTouchHelper = ItemTouchHelper(simpleCallback)
            itemTouchHelper.attachToRecyclerView(recyclerView)
        }


        commandTemplateViewModel = ViewModelProvider(this)[CommandTemplateViewModel::class.java]
        commandTemplateViewModel.items.observe(viewLifecycleOwner) {
            if (it.isEmpty()) noResults.visibility = View.VISIBLE
            else noResults.visibility = View.GONE
            templatesList = it
            templatesAdapter.submitList(it)
        }
        initMenu()
        initChips()
    }

    private fun initMenu() {
        topAppBar.setOnMenuItemClickListener { m: MenuItem ->
            val itemId = m.itemId
            if (itemId == R.id.export_clipboard) {
                lifecycleScope.launch{
                    withContext(Dispatchers.IO){
                        commandTemplateViewModel.exportToClipboard()
                    }
                    Snackbar.make(recyclerView, getString(R.string.copied_to_clipboard), Snackbar.LENGTH_LONG).show()
                }
            }else if (itemId == R.id.import_clipboard){
                lifecycleScope.launch{
                    withContext(Dispatchers.IO){
                        val count = commandTemplateViewModel.importFromClipboard()
                        mainActivity.runOnUiThread{
                            Snackbar.make(recyclerView, "${getString(R.string.items_imported)} (${count})", Snackbar.LENGTH_LONG).show()
                        }
                    }

                }
            }
            true
        }
    }

    private fun initChips() {
        val new = view?.findViewById<Chip>(R.id.newTemplate)
        new?.setOnClickListener {
            UiUtil.showCommandTemplateCreationOrUpdatingSheet(null,mainActivity, this, commandTemplateViewModel) {}
        }
        val shortcuts = view?.findViewById<Chip>(R.id.shortcuts)
        shortcuts?.setOnClickListener {
            UiUtil.showShortcutsSheet(mainActivity,this, commandTemplateViewModel)
        }
    }

    companion object {
        private const val TAG = "DownloadLogActivity"
    }

    override fun onItemClick(commandTemplate: CommandTemplate, index: Int) {
        UiUtil.showCommandTemplateCreationOrUpdatingSheet(commandTemplate,mainActivity, this, commandTemplateViewModel) {
            templatesAdapter.notifyItemChanged(index)
        }

    }

    override fun onSelected(commandTemplate: CommandTemplate) {
    }

    override fun onDelete(commandTemplate: CommandTemplate) {
        val deleteDialog = MaterialAlertDialogBuilder(requireContext())
        deleteDialog.setTitle(getString(R.string.you_are_going_to_delete) + " \"" + commandTemplate.title + "\"!")
        deleteDialog.setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
        deleteDialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
            commandTemplateViewModel.delete(commandTemplate)
        }
        deleteDialog.show()
    }

    private var simpleCallback: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT ) {
            override fun onMove(recyclerView: RecyclerView,viewHolder: RecyclerView.ViewHolder,target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        val deletedItem = templatesList[position]
                        templatesAdapter.notifyItemChanged(position)
                        onDelete(deletedItem)
                    }

                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    context,
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                    .addSwipeLeftBackgroundColor(Color.RED)
                    .addSwipeLeftActionIcon(R.drawable.baseline_delete_24)
                    .create()
                    .decorate()
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }
}