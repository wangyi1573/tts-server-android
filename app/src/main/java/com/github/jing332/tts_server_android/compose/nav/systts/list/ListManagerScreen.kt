package com.github.jing332.tts_server_android.compose.nav.systts.list

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.LocalDrawerState
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.asAppCompatActivity
import com.github.jing332.tts_server_android.compose.nav.NavRoutes
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.nav.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.nav.systts.ConfigDeleteDialog
import com.github.jing332.tts_server_android.compose.nav.systts.edit.quick.QuickEditBottomSheet
import com.github.jing332.tts_server_android.compose.navigateSingleTop
import com.github.jing332.tts_server_android.compose.widgets.TextFieldDialog
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.data.appDb
import com.github.jing332.tts_server_android.data.entities.systts.GroupWithSystemTts
import com.github.jing332.tts_server_android.data.entities.systts.SystemTts
import com.github.jing332.tts_server_android.data.entities.systts.SystemTtsGroup
import com.github.jing332.tts_server_android.model.speech.tts.BgmTTS
import com.github.jing332.tts_server_android.model.speech.tts.ITextToSpeechEngine
import com.github.jing332.tts_server_android.model.speech.tts.LocalTTS
import com.github.jing332.tts_server_android.model.speech.tts.MsTTS
import com.github.jing332.tts_server_android.model.speech.tts.PluginTTS
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.ui.systts.ConfigExportBottomSheetFragment
import com.github.jing332.tts_server_android.ui.systts.base.QuickEditBottomSheet
import com.github.jing332.tts_server_android.ui.systts.edit.BaseParamsEditView
import com.github.jing332.tts_server_android.ui.systts.edit.BaseTtsEditActivity
import com.github.jing332.tts_server_android.ui.systts.edit.http.HttpTtsEditActivity
import com.github.jing332.tts_server_android.ui.systts.list.GroupModel
import com.github.jing332.tts_server_android.ui.systts.list.ItemModel
import com.github.jing332.tts_server_android.utils.clone
import com.github.jing332.tts_server_android.utils.longToast
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ListManagerScreen(vm: ListManagerViewModel = viewModel()) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context.asAppCompatActivity()
    val view = LocalView.current
    val drawerState = LocalDrawerState.current

    val systtsEditor =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<SystemTts>(BaseTtsEditActivity.KEY_DATA)?.let {
                appDb.systemTtsDao.insertTts(it)
                if (it.isEnabled) SystemTtsService.notifyUpdateConfig()
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun displayGroupExport(model: GroupModel) {
        val subList = (model.itemSublist as List<ItemModel>).map { it.data }
        val obj =
            GroupWithSystemTts(group = model.data, list = subList)
        val fragment = ConfigExportBottomSheetFragment(
            { AppConst.jsonBuilder.encodeToString(obj) },
            { "ttsrv-${model.name}.json" }
        )
        fragment.show(activity.supportFragmentManager, ConfigExportBottomSheetFragment.TAG)
    }

    var showQuickEdit by remember { mutableStateOf<SystemTts?>(null) }
    if (showQuickEdit != null) {
        QuickEditBottomSheet(
            onDismissRequest = {
                appDb.systemTtsDao.updateTts(showQuickEdit!!)
                showQuickEdit = null
            },
            systts = showQuickEdit!!,
            onSysttsChange = {
                showQuickEdit = it
            }
        )
    }


    fun displayQuickEditBottomSheet(systts: SystemTts) {
        // 修改数据要clone，不然对比时数据相同导致UI不更新
        systts.clone<SystemTts>()?.let { clonedData ->
            val paramsEdit = clonedData.tts.getParamsEditView(context)
            val quickEdit = QuickEditBottomSheet(
                clonedData,
                paramsEdit.second,
                paramsEdit.first as BaseParamsEditView<*, ITextToSpeechEngine>
            ) {
                appDb.systemTtsDao.updateTts(clonedData)
                if (clonedData.isEnabled) SystemTtsService.notifyUpdateConfig()
                true
            }
            quickEdit.show(
                activity.supportFragmentManager,
                QuickEditBottomSheet.TAG
            )
        }
    }


    fun navigateToEdit(systts: SystemTts) {
        navController.navigateSingleTop(
            NavRoutes.TtsEdit.id,
            Bundle().apply {
                putParcelable(NavRoutes.TtsEdit.DATA, systts)
            }
        )
    }

    fun startTtsEditor(cls: Class<*>, data: SystemTts? = null, isCopy: Boolean = false) {
        val intent = Intent(context, cls)
        if (data != null) {
            intent.putExtra(
                BaseTtsEditActivity.KEY_DATA,
                data.clone<SystemTts>()!!
                    .run {
                        if (isCopy)
                            copy(
                                id = System.currentTimeMillis(),
                                displayName = "",
                                isEnabled = false
                            )
                        else this
                    }
            )
        }
        systtsEditor.launch(intent)
    }

    var hasShownTip by rememberSaveable { mutableStateOf(false) }

    fun switchSpeechTarget(systts: SystemTts) {
        if (!hasShownTip) {
            hasShownTip = true
            context.longToast(R.string.systts_drag_tip_msg)
        }

        val model = systts.copy()
        if (model.speechRule.target == SpeechTarget.BGM) return

        if (model.speechRule.target == SpeechTarget.CUSTOM_TAG)
            appDb.speechRule.getByRuleId(model.speechRule.tagRuleId)?.let {
                val keys = it.tags.keys.toList()
                val idx = keys.indexOf(model.speechRule.tag)

                val newTag = keys.getOrNull(idx + 1)
                if (newTag == null) {
                    model.speechRule.target = SpeechTarget.ALL
                } else {
                    model.speechRule.tag = newTag
                }
            }
        else {
            appDb.speechRule.getByRuleId(model.speechRule.tagRuleId)?.let {
                model.speechRule.target = SpeechTarget.CUSTOM_TAG
                model.speechRule.tag = it.tags.keys.first()
            }
        }

        appDb.systemTtsDao.updateTts(model)
        if (model.isEnabled) SystemTtsService.notifyUpdateConfig()
    }

    var deleteTts by remember { mutableStateOf<SystemTts?>(null) }
    if (deleteTts != null) {
        ConfigDeleteDialog(
            onDismissRequest = { deleteTts = null },
            name = deleteTts?.displayName ?: ""
        ) {
            appDb.systemTtsDao.deleteTts(deleteTts!!)
            deleteTts = null
        }
    }

    var groupAudioParamsDialog by remember { mutableStateOf<SystemTtsGroup?>(null) }
    if (groupAudioParamsDialog != null) {
        GroupAudioParamsDialog(
            onDismissRequest = { groupAudioParamsDialog = null },
            params = groupAudioParamsDialog!!.audioParams,
            onConfirm = {
                appDb.systemTtsDao.updateGroup(
                    groupAudioParamsDialog!!.copy(audioParams = it)
                )

                groupAudioParamsDialog = null
            }
        )
    }

    val flow = remember { appDb.systemTtsDao.getFlowAllGroupWithTts().conflate() }
    val models by flow.collectAsState(initial = emptyList())

    val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
        if (from.key is String && to.key is String) {
            val fromKey = from.key as String
            val toKey = to.key as String

            if (fromKey.startsWith("g_") && toKey.startsWith("g_")) {
                val list = models.map { it.group }.toMutableList()

                val srcId = fromKey.substring(2).toLong()
                val src = list.find { it.id == srcId } ?: return@rememberReorderableLazyListState

                val targetId = toKey.substring(2).toLong()
                val target =
                    list.find { it.id == targetId } ?: return@rememberReorderableLazyListState

                val srcIndex = list.indexOfFirst { it.id == src.id }
                if (srcIndex == -1) return@rememberReorderableLazyListState
                val targetIndex = list.indexOfFirst { it.id == target.id }
                if (targetIndex == -1) return@rememberReorderableLazyListState

                list.removeAt(srcIndex)
                list.add(targetIndex, src)
                list.forEachIndexed { index, systemTtsGroup ->
                    appDb.systemTtsDao.updateGroup(systemTtsGroup.copy(order = index))
                }
            }
            return@rememberReorderableLazyListState
        }

        if (from.key is Long && to.key is Long) {
            val src =
                appDb.systemTtsDao.getTts(from.key as Long)
                    ?: return@rememberReorderableLazyListState
            val target =
                appDb.systemTtsDao.getTts(to.key as Long) ?: return@rememberReorderableLazyListState

            val g = appDb.systemTtsDao.getGroup(src.groupId)
            val list =
                appDb.systemTtsDao.getTtsListByGroupId(src.groupId).toMutableList()

            val srcIndex = list.indexOfFirst { it.id == src.id }
            if (srcIndex == -1) return@rememberReorderableLazyListState
            val targetIndex = list.indexOfFirst { it.id == target.id }
            if (targetIndex == -1) return@rememberReorderableLazyListState

            println("fromIndex; ${srcIndex}, toIndex $targetIndex")

            println(list.joinToString { it.displayName.toString() })
            list.removeAt(srcIndex)
            list.add(targetIndex, src)
            println(list.joinToString { it.displayName.toString() })
            list.forEachIndexed { index, systemTts ->
                appDb.systemTtsDao.updateTts(systemTts.copy(order = index))
            }
        }

    })

    var addGroupDialog by remember { mutableStateOf(false) }
    if (addGroupDialog) {
        var name by remember { mutableStateOf("") }
        TextFieldDialog(
            title = stringResource(id = R.string.add_group),
            text = name,
            onTextChange = { name = it },
            onDismissRequest = { addGroupDialog = false }) {
            addGroupDialog = false
            appDb.systemTtsDao.insertGroup(SystemTtsGroup(name = name))
        }
    }

    var addPluginDialog by remember { mutableStateOf(false) }
    if (addPluginDialog) {
        PluginSelectionDialog(onDismissRequest = { addPluginDialog = false }) {
            navigateToEdit(SystemTts(tts = PluginTTS(pluginId = it.pluginId)))
        }
    }

    var showAuditionDialog by remember { mutableStateOf<SystemTts?>(null) }
    if (showAuditionDialog != null)
        AuditionDialog(systts = showAuditionDialog!!) {
            showAuditionDialog = null
        }

    var showOptions by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NavTopAppBar(
                drawerState = drawerState,
                title = {
                    Text(stringResource(id = R.string.system_tts))
                }, actions = {
                    var showAddMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        showAddMenu = true
                    }) {
                        Icon(Icons.Default.Add, stringResource(id = R.string.add_config))

                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false }) {

                            @Composable
                            fun MenuItem(
                                icon: @Composable () -> Unit,
                                @StringRes title: Int,
                                onClick: () -> Unit
                            ) {
                                DropdownMenuItem(text = {
                                    Text(stringResource(id = title))
                                }, onClick = {
                                    showAddMenu = false
                                    onClick()
                                }, leadingIcon = icon)
                            }

                            MenuItem(
                                icon = { Icon(Icons.Default.PlaylistAdd, null) },
                                title = R.string.systts_add_internal_tts
                            ) {
                                navigateToEdit(SystemTts(tts = MsTTS()))
                            }

                            MenuItem(
                                icon = { Icon(Icons.Default.PhoneAndroid, null) },
                                title = R.string.add_local_tts
                            ) {
                                navigateToEdit(SystemTts(tts = LocalTTS()))
                            }

                            MenuItem(
                                icon = { Icon(Icons.Default.Http, null) },
                                title = R.string.systts_add_custom_tts
                            ) {
                                startTtsEditor(HttpTtsEditActivity::class.java)
                            }

                            MenuItem(
                                icon = { Icon(Icons.Default.Javascript, null) },
                                title = R.string.systts_add_plugin_tts
                            ) {
                                addPluginDialog = true
                            }

                            MenuItem(
                                icon = { Icon(Icons.Default.Audiotrack, null) },
                                title = R.string.add_bgm_tts
                            ) {
                                navigateToEdit(SystemTts(tts = BgmTTS()))
                            }

                            MenuItem(
                                icon = { Icon(Icons.Default.AddCard, null) },
                                title = R.string.add_group
                            ) {
                                addGroupDialog = true
                            }
                        }
                    }

                    IconButton(onClick = {
                        showOptions = true
                    }) {
                        Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))
                        MenuMoreOptions(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false },
                            vm = vm
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(Modifier.padding(top = paddingValues.calculateTopPadding())) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .reorderable(state = reorderState),
                state = reorderState.listState
            ) {
                models.forEachIndexed { index, groupWithSystemTts ->
                    val g = groupWithSystemTts.group
                    val checkState =
                        when (groupWithSystemTts.list.filter { it.isEnabled }.size) {
                            0 -> ToggleableState.Off           // 全未选
                            groupWithSystemTts.list.size -> ToggleableState.On   // 全选
                            else -> ToggleableState.Indeterminate    // 部分选
                        }
                    val key = "g_${g.id}"
                    stickyHeader(key = key) {
                        ShadowReorderableItem(reorderableState = reorderState, key = key) {
                            Group(
                                modifier = Modifier.detectReorderAfterLongPress(reorderState),
                                name = g.name,
                                isExpanded = g.isExpanded,
                                toggleableState = checkState,
                                onCheckedChange = {
                                    appDb.systemTtsDao.updateTts(*groupWithSystemTts.list.map { tts ->
                                        tts.copy(isEnabled = checkState != ToggleableState.On)
                                    }.toTypedArray())
                                },
                                onClick = {
                                    appDb.systemTtsDao.updateGroup(g.copy(isExpanded = !g.isExpanded))
                                },
                                onDelete = {
                                    appDb.systemTtsDao.deleteGroup(g)
                                },
                                onRename = {
                                    appDb.systemTtsDao.updateGroup(g.copy(name = it))
                                },
                                onCopy = {
                                    scope.launch {
                                        val group = g.copy(id = System.currentTimeMillis(),
                                            name = it.ifBlank { context.getString(R.string.unnamed) }
                                        )
                                        appDb.systemTtsDao.insertGroup(group)
                                        appDb.systemTtsDao.getTtsByGroup(g.id)
                                            .forEachIndexed { index, tts ->
                                                appDb.systemTtsDao.insertTts(
                                                    tts.copy(
                                                        id = System.currentTimeMillis() + index,
                                                        groupId = group.id
                                                    )
                                                )
                                            }
                                    }
                                },
                                onEditAudioParams = {
                                    groupAudioParamsDialog = g
                                }
                            )
                        }
                    }

                    if (g.isExpanded) {
                        itemsIndexed(groupWithSystemTts.list.sortedBy { it.order },
                            key = { _, v -> v.id }) { index, item ->
                            ShadowReorderableItem(reorderableState = reorderState, key = item.id) {
                                Item(
                                    reorderState = reorderState,
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                                    name = item.displayName ?: "",
                                    speechTarget = item.speechRule.tagName,
                                    type = item.tts.getType(),
                                    enabled = item.isEnabled,
                                    onEnabledChange = {
                                        appDb.systemTtsDao.updateTts(item.copy(isEnabled = it))
                                    },
                                    desc = item.tts.getDescription(),
                                    params = item.tts.getBottomContent(),
                                    onClick = { showQuickEdit = item },
                                    onLongClick = { switchSpeechTarget(item) },
                                    onCopy = {
                                        startTtsEditor(
                                            item.tts.getEditActivity(),
                                            item,
                                            isCopy = true
                                        )
                                    },
                                    onDelete = { deleteTts = item },
                                    onEdit = {
                                        navController.navigateSingleTop(
                                            NavRoutes.TtsEdit.id,
                                            Bundle().apply {
                                                putParcelable(NavRoutes.TtsEdit.DATA, item)
                                            }
                                        )
                                    },
                                    onAudition = { showAuditionDialog = item }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}
