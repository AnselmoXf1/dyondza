package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel
import kotlinx.coroutines.delay

enum class AuthScreenState {
    SPLASH, LOGIN, REGISTER, RECOVERY, OTP
}

@Composable
fun AuthNavigator(
    viewModel: FocusViewModel,
    onAuthSuccess: () -> Unit
) {
    var currentState by remember { mutableStateOf(AuthScreenState.SPLASH) }
    var tempEmail by remember { mutableStateOf("") }
    var tempName by remember { mutableStateOf("") }
    var tempProvince by remember { mutableStateOf("Maputo") }
    var tempSchool by remember { mutableStateOf("Escola Secundária Josina Machel") }
    var tempFocusArea by remember { mutableStateOf("Ciências e Tecnologia") }

    AnimatedContent(
        targetState = currentState,
        transitionSpec = {
            fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
        },
        label = "AuthNavigation"
    ) { state ->
        when (state) {
            AuthScreenState.SPLASH -> {
                SplashScreen(
                    onFinished = {
                        currentState = AuthScreenState.LOGIN
                    }
                )
            }
            AuthScreenState.LOGIN -> {
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = {
                        currentState = AuthScreenState.REGISTER
                    },
                    onNavigateToRecovery = {
                        currentState = AuthScreenState.RECOVERY
                    },
                    onLoginSuccess = onAuthSuccess
                )
            }
            AuthScreenState.REGISTER -> {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSubmitted = { name, email, province, school, focusArea ->
                        tempName = name
                        tempEmail = email
                        tempProvince = province
                        tempSchool = school
                        tempFocusArea = focusArea
                        currentState = AuthScreenState.OTP
                    },
                    onNavigateToLogin = {
                        currentState = AuthScreenState.LOGIN
                    }
                )
            }
            AuthScreenState.OTP -> {
                OtpScreen(
                    email = tempEmail,
                    onOtpVerified = {
                        viewModel.register(tempName, tempEmail, tempProvince, tempSchool, tempFocusArea)
                        onAuthSuccess()
                    },
                    onNavigateBack = {
                        currentState = AuthScreenState.REGISTER
                    }
                )
            }
            AuthScreenState.RECOVERY -> {
                RecoveryScreen(
                    onNavigateBack = {
                        currentState = AuthScreenState.LOGIN
                    }
                )
            }
        }
    }
}

@Composable
fun DyondzaLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(130.dp)
            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Glowing aura behind the book
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryIndigo.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )

        Canvas(modifier = Modifier.size(80.dp).testTag("app_canvas_logo")) {
            val width = size.width
            val height = size.height

            // 1. Draw stylized pages of an open book
            val path = androidx.compose.ui.graphics.Path().apply {
                // Left Page
                moveTo(width * 0.5f, height * 0.75f)
                cubicTo(width * 0.3f, height * 0.8f, width * 0.1f, height * 0.65f, width * 0.1f, height * 0.35f)
                lineTo(width * 0.1f, height * 0.25f)
                cubicTo(width * 0.1f, height * 0.55f, width * 0.3f, height * 0.68f, width * 0.5f, height * 0.63f)

                // Right Page
                cubicTo(width * 0.7f, height * 0.68f, width * 0.9f, height * 0.55f, width * 0.9f, height * 0.25f)
                lineTo(width * 0.9f, height * 0.35f)
                cubicTo(width * 0.9f, height * 0.65f, width * 0.7f, height * 0.8f, width * 0.5f, height * 0.75f)
                close()
            }
            drawPath(
                path = path,
                color = SlateSurface
            )
            drawPath(
                path = path,
                color = PrimaryIndigo,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // 2. Draw sprout leaf representing learning growth (Zen Sage style)
            val leafPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(width * 0.5f, height * 0.58f)
                cubicTo(width * 0.32f, height * 0.42f, width * 0.32f, height * 0.18f, width * 0.5f, height * 0.12f)
                cubicTo(width * 0.68f, height * 0.18f, width * 0.68f, height * 0.42f, width * 0.5f, height * 0.58f)
                close()
            }
            drawPath(
                path = leafPath,
                color = TertiaryMint
            )
            drawPath(
                path = leafPath,
                color = Color.White.copy(alpha = 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DyondzaLogo()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DYONDZA",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryIndigo,
                letterSpacing = 4.sp
            )

            Text(
                text = "Estudo Zen & Foco Inabalável",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = PrimaryIndigo,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }

        Text(
            text = "Made for Mozambique Students",
            fontSize = 11.sp,
            color = MutedText.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: FocusViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            DyondzaLogo()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bem-vindo de volta!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LightText,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Mantenha o seu foco e alcance o topo nacional",
                fontSize = 13.sp,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error display card
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22EF5350)),
                    border = BorderStroke(1.dp, Color(0x55EF5350))
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("E-mail do Aluno") },
                placeholder = { Text("exemplo@estudante.co.mz") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MutedText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = PrimaryIndigo,
                    unfocusedLabelColor = MutedText,
                    focusedTextColor = LightText,
                    unfocusedTextColor = LightText
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MutedText) },
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Ocultar" else "Mostrar",
                            color = PrimaryIndigo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = PrimaryIndigo,
                    unfocusedLabelColor = MutedText,
                    focusedTextColor = LightText,
                    unfocusedTextColor = LightText
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot Password Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Esqueceu a senha?",
                    fontSize = 12.sp,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToRecovery() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    if (email.isEmpty() || password.isEmpty()) {
                        errorMessage = "Por favor, preencha todos os campos."
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Insira um endereço de e-mail válido."
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "A senha deve conter no mínimo 6 caracteres."
                        return@Button
                    }

                    isAuthenticating = true
                    // Simular Login Seguro no Firebase
                    viewModel.login(
                        email = email,
                        name = email.substringBefore("@").replace(".", " ").capitalizeWords(),
                        province = "Maputo",
                        school = "Escola Secundária Josina Machel"
                    )
                    isAuthenticating = false
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_submit_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = !isAuthenticating
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Entrar na Arena",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign Up link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Não tem uma conta?", color = MutedText, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cadastre-se",
                    color = TertiaryMint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: FocusViewModel,
    onRegisterSubmitted: (String, String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("Maputo") }
    var school by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var focusArea by remember { mutableStateOf("Ciências e Tecnologia") }
    var focusAreaExpanded by remember { mutableStateOf(false) }

    val provincesList = listOf("Maputo", "Gaza", "Inhambane", "Sofala", "Manica", "Tete", "Zambézia", "Nampula", "Cabo Delgado", "Niassa")
    val focusAreasList = listOf(
        "Ciências e Tecnologia",
        "Ciências da Saúde",
        "Ciências Humanas e Letras",
        "Ciências Econômicas e Sociais",
        "Artes e Design"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Criar Conta",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "Cadastre-se na rede nacional de estudos de Moçambique",
                fontSize = 12.sp,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22EF5350)),
                    border = BorderStroke(1.dp, Color(0x55EF5350))
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("Nome Completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MutedText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = LightText,
                    unfocusedTextColor = LightText
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("E-mail Escolar") },
                placeholder = { Text("aluno@escola.ac.mz") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MutedText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_email_input"),
                shape = RoundedCornerShape(12.dp),
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
                    value = province,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Província") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MutedText) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("register_province_dropdown"),
                    shape = RoundedCornerShape(12.dp),
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
                                province = prov
                                provinceExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            // School Input with Recommendations
            val allSchoolsList by viewModel.allSchools.collectAsState()
            val schoolRecommendations = remember(school, allSchoolsList) {
                if (school.isEmpty()) {
                    allSchoolsList.take(3).map { it.name }
                } else {
                    allSchoolsList.filter { it.name.contains(school, ignoreCase = true) && !it.name.equals(school, ignoreCase = true) }.take(3).map { it.name }
                }
            }

            OutlinedTextField(
                value = school,
                onValueChange = { school = it; errorMessage = null },
                label = { Text("Nome da Escola") },
                placeholder = { Text("Ex: Escola Secundária Josina Machel") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = MutedText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_school_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = LightText,
                    unfocusedTextColor = LightText
                ),
                singleLine = true
            )

            if (schoolRecommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Sugestões de Escolas (clique para preencher):",
                        fontSize = 11.sp,
                        color = TertiaryMint,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        schoolRecommendations.forEach { recommendedSchool ->
                            SuggestionChip(
                                onClick = { 
                                    school = recommendedSchool
                                    errorMessage = null
                                },
                                label = { 
                                    Text(
                                        recommendedSchool, 
                                        color = LightText,
                                        fontSize = 11.sp
                                    ) 
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = SlateSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Focus Area Dropdown Selector
            ExposedDropdownMenuBox(
                expanded = focusAreaExpanded,
                onExpandedChange = { focusAreaExpanded = !focusAreaExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = focusArea,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Área de Dedicação (Sua Competição)") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = MutedText) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = focusAreaExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("register_focus_area_dropdown"),
                    shape = RoundedCornerShape(12.dp),
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
                                focusArea = area
                                focusAreaExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Senha Secreta") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MutedText) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_password_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = LightText,
                    unfocusedTextColor = LightText
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            Button(
                onClick = {
                    if (name.isEmpty() || email.isEmpty() || school.isEmpty() || password.isEmpty()) {
                        errorMessage = "Por favor, preencha todos os campos."
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Por favor, insira um e-mail válido."
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "A senha precisa ter no mínimo 6 caracteres."
                        return@Button
                    }

                    // Avançar para tela de OTP
                    onRegisterSubmitted(name, email, province, school, focusArea)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("register_submit_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(
                    text = "Criar Conta & Receber OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Login link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Já tem cadastro?", color = MutedText, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Faça Login",
                    color = TertiaryMint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}

@Composable
fun OtpScreen(
    email: String,
    onOtpVerified: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timerSeconds by remember { mutableStateOf(59) }

    LaunchedEffect(Unit) {
        while (timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Verificação de Segurança",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )

            Text(
                text = "Código OTP enviado com sucesso para:\n$email",
                fontSize = 13.sp,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFEF5350),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Segmented OTP Input Display (Mocked style but fully interactive)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val digit = otpCode.getOrNull(i)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateSurface)
                            .border(
                                width = if (otpCode.length == i) 2.dp else 1.dp,
                                color = if (otpCode.length == i) PrimaryIndigo else GlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = LightText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive Virtual Keyboard for clean OTP flow
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val numRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Limpar", "0", "Apagar")
                )

                numRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlateSurface.copy(alpha = 0.7f))
                                    .clickable {
                                        errorMessage = null
                                        if (char == "Apagar") {
                                            if (otpCode.isNotEmpty()) otpCode = otpCode.dropLast(1)
                                        } else if (char == "Limpar") {
                                            otpCode = ""
                                        } else {
                                            if (otpCode.length < 4) {
                                                otpCode += char
                                            }
                                        }
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Verify Button
            Button(
                onClick = {
                    if (otpCode.length < 4) {
                        errorMessage = "O código deve conter 4 dígitos."
                        return@Button
                    }
                    // Simulamos que qualquer código de 4 dígitos (como 1234 ou similar) é aceito para o protótipo
                    onOtpVerified()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("otp_verify_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TertiaryMint)
            ) {
                Text(
                    text = "Validar Registro",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBg
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (timerSeconds > 0) {
                Text(
                    text = "Reenviar código em ${timerSeconds}s",
                    fontSize = 12.sp,
                    color = MutedText
                )
            } else {
                Text(
                    text = "Reenviar Código OTP",
                    fontSize = 12.sp,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        timerSeconds = 59
                        otpCode = ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Voltar",
                fontSize = 13.sp,
                color = MutedText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onNavigateBack() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun RecoveryScreen(
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recuperar Conta",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )

            Text(
                text = "Enviaremos as instruções de alteração de senha segura para o seu e-mail.",
                fontSize = 13.sp,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (codeSent) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, PrimaryIndigo)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "E-mail de Recuperação Enviado!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Por favor, verifique a sua caixa de entrada de $email. Siga as instruções para criar uma nova senha secreta.",
                            fontSize = 12.sp,
                            color = MutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Seu E-mail") },
                    placeholder = { Text("aluno@escola.co.mz") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MutedText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isEmpty()) {
                            errorMessage = "Por favor, preencha o e-mail."
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            errorMessage = "Insira um endereço de e-mail válido."
                            return@Button
                        }
                        codeSent = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("recovery_submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text(
                        text = "Solicitar Nova Senha",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Voltar ao Login",
                fontSize = 13.sp,
                color = MutedText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onNavigateBack() }
                    .padding(8.dp)
            )
        }
    }
}

// Helper Extension for String Capitalization
fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
