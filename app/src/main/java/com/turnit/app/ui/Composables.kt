package com.turnit.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turnit.app.models.ModelOption
import kotlinx.coroutines.launch

object QX {
    val VoidBlack      = Color(0xFF0B0E14)
    val QuantumTeal    = Color(0xFF008080)
    val PurpleGlow     = Color(0xFF7B4FBF)
    val NeonRed        = Color(0xFFF87171)
    val GlassFill      = Color(0x1AFFFFFF)
    val GlassBorder    = Color(0x33FFFFFF)
    val TextPrimary    = Color(0xFFF0F4FF)
    val TextMuted      = Color(0xFF8A9BB5)
}

@Composable
fun NebulaBackground(content: @Composable () -> Unit) {
    val nebulaBrush = Brush.radialGradient(
        colors = listOf(QX.PurpleGlow.copy(alpha = 0.15f), QX.QuantumTeal.copy(alpha = 0.05f), QX.VoidBlack),
        center = Offset(0.7f, 0.2f),
        radius = 1500f
    )
    Box(Modifier.fillMaxSize().background(nebulaBrush)) { content() }
}

@Composable
fun TurnItTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(background = QX.VoidBlack), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: Painter,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).border(1.dp, QX.GlassBorder, RoundedCornerShape(24.dp)),
        placeholder = { Text(label, color = QX.TextMuted) },
        leadingIcon = { Icon(icon, null, tint = QX.QuantumTeal, modifier = Modifier.size(20.dp)) },
        singleLine = true,
        textStyle = TextStyle(color = QX.TextPrimary, fontSize = 16.sp),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = QX.VoidBlack.copy(0.5f),
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun LoginScreen(onLoginClick: (String, String) -> Unit, onSignupClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    NebulaBackground {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(100.dp).background(QX.GlassFill, CircleShape).border(1.dp, QX.QuantumTeal, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = QX.TextMuted)
            }
            Text("TurnIt-Native", modifier = Modifier.padding(top = 16.dp, bottom = 48.dp), color = QX.QuantumTeal, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

            QXTextField(username, { username = it }, "Username", androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_search))
            QXTextField(password, { password = it }, "Password", androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_secure), visualTransformation = PasswordVisualTransformation())

            Button(onClick = { onLoginClick(username, password) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = QX.QuantumTeal), shape = RoundedCornerShape(25.dp)) {
                Text("LOGIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            TextButton(onClick = onSignupClick) { Text("Create Account / Signup", color = QX.TextMuted) }
        }
    }
}

@Composable
fun SignupScreen(onSignupClick: (String, String, String) -> Unit, onLoginClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    NebulaBackground {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Create Account", modifier = Modifier.padding(bottom = 48.dp), color = QX.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)

            QXTextField(username, { username = it }, "Username", androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_search))
            QXTextField(email, { email = it }, "Email", androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_dialog_email))
            QXTextField(password, { password = it }, "Password", androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_secure), visualTransformation = PasswordVisualTransformation())

            Button(onClick = { onSignupClick(username, email, password) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = QX.QuantumTeal), shape = RoundedCornerShape(25.dp)) {
                Text("Signup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            TextButton(onClick = onLoginClick) { Text("Back to Login", color = QX.TextMuted) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnItMainScreen(
    messages: List<Pair<String, Int>>,
    selectedModel: ModelOption,
    onModelChange: (ModelOption) -> Unit,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showModelMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = QX.VoidBlack) {
                Text("TurnIt History", modifier = Modifier.padding(16.dp), color = QX.QuantumTeal, style = MaterialTheme.typography.titleLarge)
                NavigationDrawerItem(label = { Text("New Chat") }, selected = false, onClick = { onNewChat(); scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            containerColor = QX.VoidBlack,
            topBar = {
                TopAppBar(
                    title = { Text("TurnIt", color = QX.QuantumTeal, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = QX.QuantumTeal) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QX.VoidBlack)
                )
            },
            bottomBar = { NeonInputBar(onSend = onSend) }
        ) { pad ->
            NebulaBackground {
                Box(Modifier.padding(pad).fillMaxSize()) {
                    ChatList(messages)

                    Column(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 8.dp), horizontalAlignment = Alignment.End) {
                        if (showModelMenu) {
                            Surface(Modifier.heightIn(max = 300.dp).width(200.dp).padding(bottom = 8.dp), color = QX.VoidBlack.copy(0.9f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, QX.GlassBorder)) {
                                LazyColumn {
                                    items(QX_MODELS) { model ->
                                        DropdownMenuItem(text = { Text(model.displayName, color = if(model == selectedModel) QX.QuantumTeal else QX.TextPrimary) }, onClick = { onModelChange(model); showModelMenu = false })
                                    }
                                }
                            }
                        }
                        FloatingActionButton(onClick = { showModelMenu = !showModelMenu }, containerColor = QX.QuantumTeal.copy(0.2f), contentColor = QX.QuantumTeal, shape = CircleShape, modifier = Modifier.border(1.dp, QX.QuantumTeal, CircleShape)) {
                            Text(selectedModel.shortLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatList(messages: List<Pair<String, Int>>) {
    val state = rememberLazyListState()
    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(bottom = 70.dp)) {
        items(messages) { msg ->
            val isUser = msg.second == MSG_USER
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), contentAlignment = if(isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                Surface(color = if(isUser) QX.QuantumTeal.copy(0.2f) else QX.GlassFill, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, QX.GlassBorder)) {
                    Text(msg.first, modifier = Modifier.padding(12.dp), color = QX.TextPrimary)
                }
            }
        }
    }
}

@Composable
fun NeonInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.weight(1f).background(QX.GlassFill, CircleShape).border(1.dp, QX.QuantumTeal, CircleShape).padding(horizontal = 8.dp),
            placeholder = { Text("Message...") },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
        IconButton(onClick = { onSend(text); text = "" }) { 
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = QX.QuantumTeal) 
        }
    }
}
