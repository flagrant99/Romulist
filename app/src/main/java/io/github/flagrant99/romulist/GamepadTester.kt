package io.github.flagrant99.romulist

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.*

data class DeviceInfo(
    val id: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val sources: Int
)

data class EventLogEntry(
    val timestamp: String,
    val text: String
)

@Composable
fun GamepadTester(
    selectedDevice: DeviceInfo?,
    onDeviceSelect: (DeviceInfo?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
) {
    val context = LocalContext.current
    val inputManager = remember { context.getSystemService(Context.INPUT_SERVICE) as InputManager }

    var devices by remember {
        mutableStateOf(getConnectedDevices(inputManager))
    }

    DisposableEffect(inputManager) {
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                devices = getConnectedDevices(inputManager)
            }
            override fun onInputDeviceRemoved(deviceId: Int) {
                devices = getConnectedDevices(inputManager)
                if (selectedDevice?.id == deviceId) {
                    onDeviceSelect(null)
                }
            }
            override fun onInputDeviceChanged(deviceId: Int) {
                devices = getConnectedDevices(inputManager)
            }
        }
        inputManager.registerInputDeviceListener(listener, null)
        onDispose {
            inputManager.unregisterInputDeviceListener(listener)
        }
    }

    if (selectedDevice != null) {
        GamepadDetailScreen(
            device = selectedDevice,
            onBack = onBack,
            modifier = modifier,
            swapAB = swapAB
        )
    } else {
        GamepadDeviceList(
            devices = devices,
            onDeviceSelect = { device -> onDeviceSelect(device) },
            onBack = onBack,
            modifier = modifier,
            swapAB = swapAB
        )
    }
}

@Composable
fun GamepadDeviceList(
    devices: List<DeviceInfo>,
    onDeviceSelect: (DeviceInfo) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
) {
    val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
    val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

    var showAllDevices by remember { mutableStateOf(false) }

    val nonGamepadKeywords = remember {
        listOf("virtual", "gpio-keys", "pmic_pwrkey", "pmic_resin", "sec_touchscreen", "sec_touchscreen2", "sec_touchpad")
    }

    val filteredDevices = remember(devices, showAllDevices) {
        if (showAllDevices) {
            devices
        } else {
            devices.filter { device ->
                val lowerName = device.name.lowercase(Locale.getDefault())
                nonGamepadKeywords.none { lowerName.contains(it) }
            }
        }
    }

    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(filteredDevices) {
        if (filteredDevices.isNotEmpty()) {
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "CONNECTED GAMEPADS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.7f), blurRadius = 16f)
                    ),
                    color = Color.Green
                )
                Text(
                    text = "Total Devices: ${filteredDevices.size} (Select device and press A/Enter for Controls)",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    color = Color.Green.copy(alpha = 0.5f)
                )
            }
        }

        // Show All Devices Checkbox
        item {
            var isCheckboxFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isCheckboxFocused = it.isFocused }
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    showAllDevices = !showAllDevices
                                    true
                                }
                                backKey -> {
                                    onBack()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .background(if (isCheckboxFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { showAllDevices = !showAllDevices }
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Checkbox(
                    checked = showAllDevices,
                    onCheckedChange = { showAllDevices = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.Green,
                        uncheckedColor = Color.Green.copy(alpha = 0.7f),
                        checkmarkColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show all devices",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                color = Color.Green.copy(alpha = 0.3f)
            )
        }

        if (filteredDevices.isEmpty()) {
            item {
                Text(
                    text = if (showAllDevices) "No input devices detected." else "No gamepads detected. Connect a gamepad via Bluetooth or USB (or check 'Show all devices').",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Yellow,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            itemsIndexed(filteredDevices) { index, device ->
                var isFocused by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(if (index == 0) firstItemFocusRequester else remember { FocusRequester() })
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    launchKey,
                                    KeyEvent.KEYCODE_DPAD_CENTER,
                                    KeyEvent.KEYCODE_ENTER -> {
                                        onDeviceSelect(device)
                                        true
                                    }
                                    backKey -> {
                                        onBack()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .background(if (isFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onDeviceSelect(device) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(
                                color = Color.Green.copy(alpha = 0.5f),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        ),
                        color = Color.Green
                    )
                    Text(
                        text = "ID: ${device.id} | Vendor: ${device.vendorId} | Product: ${device.productId}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(Color.Green.copy(alpha = 0.3f), blurRadius = 4f)
                        ),
                        color = Color.Green.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun GamepadDetailScreen(
    device: DeviceInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
) {
    // Currently held down buttons
    val pressedKeys = remember { mutableStateMapOf<Int, Boolean>() }
    // Persistent press counter for tested controls
    val testedKeys = remember { mutableStateMapOf<Int, Int>() }

    // State for axes
    val axisValues = remember { mutableStateMapOf<Int, Float>() }
    val testedAxes = remember { mutableStateMapOf<Int, Boolean>() }

    // Event log
    val eventLogs = remember { mutableStateListOf<EventLogEntry>() }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    fun logEvent(text: String) {
        val time = dateFormat.format(Date())
        if (eventLogs.size > 50) {
            eventLogs.removeAt(eventLogs.size - 1)
        }
        eventLogs.add(0, EventLogEntry(time, text))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // AndroidView container to capture all global key and motion events without UI focus navigation
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                object : FrameLayout(ctx) {
                    init {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()
                    }

                    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                        val keyCode = event.keyCode
                        val action = event.action
                        val keyName = KeyEvent.keyCodeToString(keyCode) ?: "KEYCODE_$keyCode"

                        when (action) {
                            KeyEvent.ACTION_DOWN -> {
                                if (event.repeatCount == 0) {
                                    pressedKeys[keyCode] = true
                                    testedKeys[keyCode] = (testedKeys[keyCode] ?: 0) + 1
                                    logEvent("Button DOWN: $keyName (code $keyCode)")
                                }
                            }
                            KeyEvent.ACTION_UP -> {
                                pressedKeys[keyCode] = false
                                logEvent("Button UP: $keyName (code $keyCode)")
                            }
                        }

                        // Consume all gamepad key events on Controls & Status screen to prevent UI navigation interference
                        return true
                    }

                    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
                        val axes = listOf(
                            Pair("Left Stick X", MotionEvent.AXIS_X),
                            Pair("Left Stick Y", MotionEvent.AXIS_Y),
                            Pair("Right Stick X", MotionEvent.AXIS_Z),
                            Pair("Right Stick Y", MotionEvent.AXIS_RZ),
                            Pair("Hat X (D-Pad X)", MotionEvent.AXIS_HAT_X),
                            Pair("Hat Y (D-Pad Y)", MotionEvent.AXIS_HAT_Y),
                            Pair("Left Trigger", MotionEvent.AXIS_LTRIGGER),
                            Pair("Right Trigger", MotionEvent.AXIS_RTRIGGER),
                            Pair("Gas", MotionEvent.AXIS_GAS),
                            Pair("Brake", MotionEvent.AXIS_BRAKE)
                        )

                        for ((name, axis) in axes) {
                            val value = event.getAxisValue(axis)
                            val prevVal = axisValues[axis] ?: 0f
                            if (Math.abs(value) > 0.05f) {
                                axisValues[axis] = value
                                testedAxes[axis] = true
                                if (Math.abs(value - prevVal) > 0.1f) {
                                    logEvent("Axis $name: ${String.format(Locale.getDefault(), "%.2f", value)}")
                                }
                            } else {
                                if (axisValues.containsKey(axis) && axisValues[axis] != 0f) {
                                    axisValues[axis] = 0f
                                }
                            }
                        }
                        return true
                    }
                }
            },
            update = { view ->
                view.requestFocus()
            }
        )

        // Overlay Compose UI displaying gamepad status
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "CONTROLS & STATUS",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(Color.Green.copy(alpha = 0.7f), blurRadius = 16f)
                        ),
                        color = Color.Green
                    )
                    Text(
                        text = "Device: ${device.name} (ID: ${device.id})",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Green.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Press any button or move sticks to test. (Use bottom Nav Bar BACK to exit)",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Yellow.copy(alpha = 0.8f)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                        color = Color.Green.copy(alpha = 0.5f)
                    )
                }
            }

            // Buttons Grid / List
            item {
                Text(
                    text = "BUTTONS",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val buttonItems = listOf(
                    Triple("A / Cross", KeyEvent.KEYCODE_BUTTON_A, "A"),
                    Triple("B / Circle", KeyEvent.KEYCODE_BUTTON_B, "B"),
                    Triple("X / Square", KeyEvent.KEYCODE_BUTTON_X, "X"),
                    Triple("Y / Triangle", KeyEvent.KEYCODE_BUTTON_Y, "Y"),
                    Triple("LB / L1", KeyEvent.KEYCODE_BUTTON_L1, "L1"),
                    Triple("RB / R1", KeyEvent.KEYCODE_BUTTON_R1, "R1"),
                    Triple("LT / L2", KeyEvent.KEYCODE_BUTTON_L2, "L2"),
                    Triple("RT / R2", KeyEvent.KEYCODE_BUTTON_R2, "R2"),
                    Triple("Select", KeyEvent.KEYCODE_BUTTON_SELECT, "SELECT"),
                    Triple("Start", KeyEvent.KEYCODE_BUTTON_START, "START"),
                    Triple("L3 (Thumb)", KeyEvent.KEYCODE_BUTTON_THUMBL, "L3"),
                    Triple("R3 (Thumb)", KeyEvent.KEYCODE_BUTTON_THUMBR, "R3"),
                    Triple("D-Pad Up", KeyEvent.KEYCODE_DPAD_UP, "DPAD_UP"),
                    Triple("D-Pad Down", KeyEvent.KEYCODE_DPAD_DOWN, "DPAD_DOWN"),
                    Triple("D-Pad Left", KeyEvent.KEYCODE_DPAD_LEFT, "DPAD_LEFT"),
                    Triple("D-Pad Right", KeyEvent.KEYCODE_DPAD_RIGHT, "DPAD_RIGHT")
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    buttonItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (label, keyCode, _) ->
                                val isPressed = pressedKeys[keyCode] == true
                                val pressCount = testedKeys[keyCode] ?: 0

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isPressed) Color.Green
                                            else if (pressCount > 0) Color.Green.copy(alpha = 0.35f)
                                            else Color.Green.copy(alpha = 0.12f)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                            color = if (isPressed) Color.Black else Color.Green
                                        )
                                        Text(
                                            text = if (isPressed) "● PRESSED" else if (pressCount > 0) "✓ TESTED ($pressCount)" else "○ IDLE",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = if (isPressed) Color.Black else if (pressCount > 0) Color.Green else Color.Green.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Analog Axes
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ANALOG STICKS & TRIGGERS",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val axesInfo = listOf(
                    Pair("Left Stick X (Axis X)", MotionEvent.AXIS_X),
                    Pair("Left Stick Y (Axis Y)", MotionEvent.AXIS_Y),
                    Pair("Right Stick X (Axis Z)", MotionEvent.AXIS_Z),
                    Pair("Right Stick Y (Axis RZ)", MotionEvent.AXIS_RZ),
                    Pair("Hat X (D-Pad X)", MotionEvent.AXIS_HAT_X),
                    Pair("Hat Y (D-Pad Y)", MotionEvent.AXIS_HAT_Y),
                    Pair("Left Trigger (L2)", MotionEvent.AXIS_LTRIGGER),
                    Pair("Right Trigger (R2)", MotionEvent.AXIS_RTRIGGER)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    axesInfo.forEach { (label, axis) ->
                        val value = axisValues[axis] ?: 0f
                        val isTested = testedAxes[axis] == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(
                                    if (Math.abs(value) > 0.05f) Color.Green.copy(alpha = 0.4f)
                                    else if (isTested) Color.Green.copy(alpha = 0.2f)
                                    else Color.Green.copy(alpha = 0.1f)
                                )
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Green
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", value) + if (isTested) " ✓" else "",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (Math.abs(value) > 0.05f) Color.Yellow else Color.Green
                            )
                        }
                    }
                }
            }

            // Event Log
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "REAL-TIME EVENT LOG",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(eventLogs) { log ->
                Text(
                    text = "[${log.timestamp}] ${log.text}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

fun getConnectedDevices(inputManager: InputManager): List<DeviceInfo> {
    val deviceIds = inputManager.inputDeviceIds
    val list = mutableListOf<DeviceInfo>()
    for (id in deviceIds) {
        val device = inputManager.getInputDevice(id)
        if (device != null) {
            val sources = device.sources
            // Filter for gamepads, joysticks, or keyboards with gamepad capabilities
            if ((sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) ||
                (sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD)) {
                list.add(
                    DeviceInfo(
                        id = device.id,
                        name = device.name,
                        vendorId = device.vendorId,
                        productId = device.productId,
                        sources = sources
                    )
                )
            }
        }
    }
    return list
}
