package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FocusViewModel,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    val context = LocalContext.current

    // Observe Profile Data State Flow
    val studentName by viewModel.studentName.collectAsState()
    val studentProvince by viewModel.studentProvince.collectAsState()
    val studentSchool by viewModel.studentSchool.collectAsState()
    val studentFocusArea by viewModel.studentFocusArea.collectAsState()

    // Observe Settings State Flow
    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
    val isDndModeEnabled by viewModel.isDndModeEnabled.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()

    // Internal Editing States
    var editName by remember { mutableStateOf(studentName) }
    var editSchool by remember { mutableStateOf(studentSchool) }
    var editProvince by remember { mutableStateOf(studentProvince) }
    var editFocusArea by remember { mutableStateOf(studentFocusArea) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var focusAreaExpanded by remember { mutableStateOf(false) }
    var showSaveSuccessAlert by remember { mutableStateOf(false) }

    val provincesList = listOf("Maputo", "Gaza", "Inhambane", "Sofala", "Manica", "Tete", "Zambézia", "Nampula", "Cabo Delgado", "Niassa")
    val focusAreasList = listOf(
        "Ciências e Tecnologia",
        "Ciências da Saúde",
        "Ciências Humanas e Letras",
        "Ciências Econômicas e Sociais",
        "Artes e Design"
    )

    // Synchronize local states when state flows change
    LaunchedEffect(studentName, studentSchool, studentProvince, studentFocusArea) {
        editName = studentName
        editSchool = studentSchool
        editProvince = studentProvince
        editFocusArea = studentFocusArea
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SCREEN HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = LightText
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Definições do Aluno",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Text(
                    text = "Configure seu perfil acadêmico e regras de foco",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 1: PERSONAL INFORMATION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dados Pessoais",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name TextField
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nome Completo", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_name_field"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Province Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = provinceExpanded,
                    onExpandedChange = { provinceExpanded = !provinceExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editProvince,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Província", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("settings_province_dropdown"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = provinceExpanded,
                        onDismissRequest = { provinceExpanded = false },
                        modifier = Modifier.background(SlateSurface)
                    ) {
                        provincesList.forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov, color = LightText) },
                                onClick = {
                                    editProvince = prov
                                    provinceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // School Input
                OutlinedTextField(
                    value = editSchool,
                    onValueChange = { editSchool = it },
                    label = { Text("Sua Escola", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_school_field"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Focus Area Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = focusAreaExpanded,
                    onExpandedChange = { focusAreaExpanded = !focusAreaExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editFocusArea,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sua Área de Dedicação (Competição)", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = focusAreaExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("settings_focus_area_dropdown"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = focusAreaExpanded,
                        onDismissRequest = { focusAreaExpanded = false },
                        modifier = Modifier.background(SlateSurface)
                    ) {
                        focusAreasList.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area, color = LightText) },
                                onClick = {
                                    editFocusArea = area
                                    focusAreaExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (editName.isEmpty() || editSchool.isEmpty()) {
                            Toast.makeText(context, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateProfileInfo(editName, editProvince, editSchool, editFocusArea)
                        showSaveSuccessAlert = true
                        Toast.makeText(context, "Perfil acadêmico atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_save_profile_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Salvar Informações", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 2: FUNCTIONAL FOCUS RULES ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configuração de Foco",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lembretes Inteligentes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Text(
                            text = "Receber alertas para iniciar sessões diárias",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { viewModel.updateSettings(it, isDndModeEnabled, dailyGoalMinutes) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryIndigo,
                            uncheckedThumbColor = MutedText,
                            uncheckedTrackColor = GlassBorder
                        )
                    )
                }

                Divider(color = GlassBorder, modifier = Modifier.padding(vertical = 12.dp))

                // DND App Blocker Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bloqueio Rigoroso (DND)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Text(
                            text = "Impedir abertura de redes sociais durante o foco",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                    Switch(
                        checked = isDndModeEnabled,
                        onCheckedChange = { viewModel.updateSettings(isNotificationEnabled, it, dailyGoalMinutes) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryIndigo,
                            uncheckedThumbColor = MutedText,
                            uncheckedTrackColor = GlassBorder
                        )
                    )
                }

                Divider(color = GlassBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Daily Goal Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Meta Diária de Estudo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Text(
                            text = "$dailyGoalMinutes minutos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = TertiaryMint
                        )
                    }
                    Text(
                        text = "Quantidade ideal de estudo recomendada por nossa IA Coach",
                        fontSize = 11.sp,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Slider(
                        value = dailyGoalMinutes.toFloat(),
                        onValueChange = { viewModel.updateSettings(isNotificationEnabled, isDndModeEnabled, it.toInt()) },
                        valueRange = 15f..120f,
                        steps = 6, // 15, 30, 45, 60, 75, 90, 105, 120 (intervalos de 15)
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryIndigo,
                            activeTrackColor = PrimaryIndigo,
                            inactiveTrackColor = GlassBorder
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 3: SYSTEM AUDIT & CLOUD STATS (Young CEO Vision) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TertiaryMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auditoria e Segurança de Nuvem",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Servidor do Cloud Ranking", fontSize = 11.sp, color = MutedText)
                        Text("Ativo (Africa-Maputo v2)", fontSize = 11.sp, color = TertiaryMint, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Prevenção de Fraude de XP", fontSize = 11.sp, color = MutedText)
                        Text("Ativado (Server-Validated)", fontSize = 11.sp, color = TertiaryMint, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cache SQLite (Room Database)", fontSize = 11.sp, color = MutedText)
                        Text("128 KB (Criptografado)", fontSize = 11.sp, color = LightText)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sincronização Ativa", fontSize = 11.sp, color = MutedText)
                        Text("100% Offline-First", fontSize = 11.sp, color = LightText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- LOGOUT BUTTON ---
        Button(
            onClick = onLogoutClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("settings_logout_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)) // Red Color for safety Logout
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sair da Conta do Estudante",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
