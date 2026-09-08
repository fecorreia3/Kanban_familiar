package com.example.teste_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Color(0xFF2563EB))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KanbanHomeScreen()
                }
            }
        }
    }
}

// Modelo de Dados da Tarefa
data class Task(
    val id: String,
    val title: String,
    var status: String, // 'todo', 'doing', 'done'
    val isBlocked: Boolean,
    val blockReasonKey: String? = null
)

// Estrutura de Configuração do Bloqueio
data class BlockConfig(
    val label: String,
    val color: Color,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanHomeScreen() {
    // Lista mutável de blocos disponíveis (permite adicionar novos dinamicamente)
    val blockInfoMap = remember {
        mutableStateMapOf(
            "money" to BlockConfig("Falta grana", Color(0xFFD32F2F), Icons.Default.AttachMoney),
            "material" to BlockConfig("Falta material", Color(0xFFE65100), Icons.Default.ShoppingCart),
            "knowledge" to BlockConfig("Não sei fazer", Color(0xFF7B1FA2), Icons.Default.Psychology),
            "time" to BlockConfig("Falta tempo", Color(0xFF1976D2), Icons.Default.AccessTime)
        )
    }

    val tasks = remember {
        mutableStateListOf(
            Task("1", "Pintar a parede da sala", "todo", true, "money"),
            Task("2", "Trocar a lâmpada do corredor", "todo", false),
            Task("3", "Consertar a torneira da pia", "doing", true, "material"),
            Task("4", "Montar a sapateira nova", "done", false)
        )
    }

    var showAddModal by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showCustomBlockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏡 Kanban Board Familiar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Tarefa")
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KanbanColumn("A Fazer", "todo", Color(0xFFF1F5F9), tasks, blockInfoMap) { task ->
                taskToEdit = task
            }
            KanbanColumn("Fazendo", "doing", Color(0xFFFEF3C7), tasks, blockInfoMap) { task ->
                taskToEdit = task
            }
            KanbanColumn("Feito", "done", Color(0xFFDCFCE7), tasks, blockInfoMap) { task ->
                taskToEdit = task
            }
        }

        // Modal de Adicionar Tarefa
        if (showAddModal) {
            TaskFormModal(
                titleHeader = "Nova Tarefa na Casa",
                initialTitle = "",
                initialIsBlocked = false,
                initialReason = blockInfoMap.keys.firstOrNull(),
                blockInfoMap = blockInfoMap,
                onDismiss = { showAddModal = false },
                onSave = { title, isBlocked, reason ->
                    tasks.add(
                        Task(
                            id = System.currentTimeMillis().toString(),
                            title = title,
                            status = "todo",
                            isBlocked = isBlocked,
                            blockReasonKey = if (isBlocked) reason else null
                        )
                    )
                    showAddModal = false
                },
                onAddNewBlockClick = { showCustomBlockDialog = true }
            )
        }

        // Modal de Editar / Excluir Tarefa existente
        if (taskToEdit != null) {
            val currentTask = taskToEdit!!
            TaskFormModal(
                titleHeader = "Editar Tarefa",
                initialTitle = currentTask.title,
                initialIsBlocked = currentTask.isBlocked,
                initialReason = currentTask.blockReasonKey ?: blockInfoMap.keys.firstOrNull(),
                blockInfoMap = blockInfoMap,
                isEditing = true,
                onDismiss = { taskToEdit = null },
                onSave = { title, isBlocked, reason ->
                    val index = tasks.indexOfFirst { it.id == currentTask.id }
                    if (index != -1) {
                        tasks[index] = currentTask.copy(
                            title = title,
                            isBlocked = isBlocked,
                            blockReasonKey = if (isBlocked) reason else null
                        )
                    }
                    taskToEdit = null
                },
                onDelete = {
                    tasks.removeIf { it.id == currentTask.id }
                    taskToEdit = null
                },
                onAddNewBlockClick = { showCustomBlockDialog = true }
            )
        }

        // Diálogo simples para criar um Novo Bloqueio personalizado
        if (showCustomBlockDialog) {
            var newBlockName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCustomBlockDialog = false },
                title = { Text("Criar Novo Bloqueio") },
                text = {
                    OutlinedTextField(
                        value = newBlockName,
                        onValueChange = { newBlockName = it },
                        label = { Text("Nome do impedimento (ex: Falta autorização)") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newBlockName.isNotBlank()) {
                            val key = newBlockName.lowercase().replace(" ", "_")
                            blockInfoMap[key] = BlockConfig(
                                label = newBlockName,
                                color = Color(0xFF0D9488), // Cor padrão para novos bloqueios
                                icon = Icons.Default.Warning
                            )
                            showCustomBlockDialog = false
                        }
                    }) {
                        Text("Adicionar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomBlockDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun KanbanColumn(
    title: String,
    statusKey: String,
    bgColor: Color,
    tasks: List<Task>,
    blockInfoMap: Map<String, BlockConfig>,
    onTaskClick: (Task) -> Unit
) {
    val columnTasks = tasks.filter { it.status == statusKey }

    Card(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = CircleShape,
                    color = Color.LightGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(columnTasks.size.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(columnTasks) { task ->
                    TaskCard(task, blockInfoMap, onTaskClick)
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    blockInfoMap: Map<String, BlockConfig>,
    onTaskClick: (Task) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task) }, // Clicar em qualquer lugar abre a edição
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }

            if (task.isBlocked && task.blockReasonKey != null) {
                val block = blockInfoMap[task.blockReasonKey]
                if (block != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = block.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.8.dp, block.color)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(block.icon, contentDescription = null, tint = block.color, modifier = Modifier.size(14.dp))
                            Text(block.label, color = block.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormModal(
    titleHeader: String,
    initialTitle: String,
    initialIsBlocked: Boolean,
    initialReason: String?,
    blockInfoMap: Map<String, BlockConfig>,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddNewBlockClick: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var isBlocked by remember { mutableStateOf(initialIsBlocked) }
    var selectedReason by remember { mutableStateOf(initialReason) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titleHeader, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (isEditing && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Tarefa", tint = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("O que precisa ser feito?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Está bloqueada?", fontWeight = FontWeight.Medium)
                    Text("Marque se há algum impedimento", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(checked = isBlocked, onCheckedChange = { isBlocked = it })
            }

            if (isBlocked) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Motivo do Bloqueio:", fontWeight = FontWeight.Medium)
                    TextButton(onClick = onAddNewBlockClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Bloqueio", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Exibe os chips de bloqueios disponíveis dinamicamente
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    blockInfoMap.forEach { (key, info) ->
                        FilterChip(
                            selected = selectedReason == key,
                            onClick = { selectedReason = key },
                            label = { Text(info.label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, isBlocked, selectedReason)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(if (isEditing) "Salvar Alterações" else "Adicionar Tarefa", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}