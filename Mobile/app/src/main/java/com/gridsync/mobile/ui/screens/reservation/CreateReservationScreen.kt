package com.gridsync.mobile.ui.screens.reservation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.data.remote.dto.AvailableBookingSlotDto
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid500
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid800
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme
import com.gridsync.mobile.ui.theme.OutfitFontFamily
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900
import com.gridsync.mobile.ui.util.defaultVisitEndLocal
import com.gridsync.mobile.ui.util.defaultVisitStartLocal
import com.gridsync.mobile.ui.util.validateVisitWithinStationHours
import com.gridsync.mobile.ui.util.localDateTimeToIsoUtc
import kotlinx.coroutines.launch

private enum class ReservationTypeUi(val apiValue: String, val label: String, val hint: String) {
    Charging("Charging", "Charging", "Deposit energy into a battery"),
    DropOff("DropOff", "Drop-off", "Withdraw stored energy"),
}

private enum class BookingStep(val index: Int, val title: String, val subtitle: String) {
    Station(0, "Choose station", "Pick a nearby microgrid node"),
    Battery(1, "Choose battery", "Select a battery at this station"),
    Details(2, "Booking details", "Type, energy amount, and visit window"),
    Review(3, "Review & submit", "Confirm before creating your reservation"),
}

private data class StationOption(
    val id: String,
    val name: String,
    val batteryCount: Int,
    val chargeAvail: Double,
    val dropOffAvail: Double,
)

@Composable
fun CreateReservationScreen(
    initialStationId: String? = null,
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as GridSyncApp
    val scope = rememberCoroutineScope()

    var slots by remember { mutableStateOf<List<AvailableBookingSlotDto>>(emptyList()) }
    var loadingSlots by remember { mutableStateOf(true) }
    var slotsError by remember { mutableStateOf<String?>(null) }

    var step by remember { mutableIntStateOf(BookingStep.Station.index) }
    var stationId by remember { mutableStateOf("") }
    var batteryId by remember { mutableStateOf("") }
    var reservationType by remember { mutableStateOf(ReservationTypeUi.Charging) }
    var energyKwh by remember { mutableStateOf("") }
    var visitStart by remember { mutableStateOf(defaultVisitStartLocal()) }
    var visitEnd by remember { mutableStateOf(defaultVisitEndLocal()) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var createdCode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun applyInitialStation(options: List<StationOption>) {
        val match = initialStationId?.takeIf { id -> options.any { it.id == id } }
        if (match != null) {
            stationId = match
            step = BookingStep.Battery.index
        }
    }

    fun reloadSlots() {
        scope.launch {
            loadingSlots = true
            slotsError = null
            try {
                slots = app.reservationRepository.getAvailableSlots()
                val options = groupStations(slots)
                if (stationId.isBlank()) {
                    applyInitialStation(options)
                } else if (options.none { it.id == stationId }) {
                    stationId = ""
                    batteryId = ""
                    step = BookingStep.Station.index
                }
            } catch (e: Exception) {
                slotsError = e.message ?: "Could not load available batteries"
                slots = emptyList()
            } finally {
                loadingSlots = false
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadSlots()
    }

    val stations = remember(slots) { groupStations(slots) }
    val currentStep = BookingStep.entries[step]
    val selectedStation = stations.find { it.id == stationId }
    val batteries = remember(slots, stationId) {
        slots.filter { it.stationId == stationId }.sortedBy { it.batteryIndex }
    }
    val selectedBattery = batteries.find { it.id == batteryId }
    val availableForType = selectedBattery?.let {
        if (reservationType == ReservationTypeUi.DropOff) it.availableDropOffKwh
        else it.availableChargingKwh
    } ?: 0.0

    fun goBackStep() {
        error = null
        if (step == BookingStep.Station.index) {
            onBack()
        } else {
            step -= 1
        }
    }

    fun validateCurrentStep(): Boolean {
        error = null
        return when (currentStep) {
            BookingStep.Station -> {
                if (stationId.isBlank()) {
                    error = "Select a station to continue"
                    false
                } else true
            }
            BookingStep.Battery -> {
                if (batteryId.isBlank()) {
                    error = "Select a battery to continue"
                    false
                } else true
            }
            BookingStep.Details -> {
                val energy = energyKwh.toDoubleOrNull()
                when {
                    energy == null || energy <= 0 -> {
                        error = "Enter a valid energy amount (kWh)"
                        false
                    }
                    energy > availableForType -> {
                        error =
                            "Requested ${"%.2f".format(energy)} kWh exceeds available ${"%.2f".format(availableForType)} kWh"
                        false
                    }
                    visitStart.isBlank() || visitEnd.isBlank() -> {
                        error = "Visit start and end are required"
                        false
                    }
                    else -> {
                        try {
                            localDateTimeToIsoUtc(visitStart)
                            localDateTimeToIsoUtc(visitEnd)
                            val battery = selectedBattery
                            if (battery != null) {
                                validateVisitWithinStationHours(
                                    visitStartLocal = visitStart,
                                    visitEndLocal = visitEnd,
                                    openTime = battery.openTime.ifBlank { "08:00" },
                                    closeTime = battery.closeTime.ifBlank { "18:00" },
                                    workingDays = battery.workingDays,
                                )
                            }
                            true
                        } catch (e: IllegalArgumentException) {
                            error = e.message
                            false
                        }
                    }
                }
            }
            BookingStep.Review -> true
        }
    }

    fun goNext() {
        if (!validateCurrentStep()) return
        if (step < BookingStep.Review.index) {
            step += 1
        }
    }

    fun submit() {
        if (!validateCurrentStep()) return
        val slot = selectedBattery ?: run {
            error = "Select a battery to continue"
            return
        }
        val energy = energyKwh.toDoubleOrNull() ?: return
        loading = true
        error = null
        scope.launch {
            try {
                val created = app.reservationRepository.create(
                    slotId = slot.id,
                    reservationType = reservationType.apiValue,
                    energyKwh = energy,
                    slotStartIso = localDateTimeToIsoUtc(visitStart),
                    slotEndIso = localDateTimeToIsoUtc(visitEnd),
                )
                createdCode = created.reservationCode.ifBlank { null }
                success = true
            } catch (e: Exception) {
                error = e.message ?: "Could not create reservation"
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        WizardTopBar(
            step = currentStep,
            onClose = onBack,
        )

        if (success) {
            SuccessPanel(
                stationName = selectedStation?.name.orEmpty(),
                reservationCode = createdCode,
                onDone = onSuccess,
                onCreateAnother = {
                    success = false
                    createdCode = null
                    step = if (initialStationId != null && stations.any { it.id == initialStationId }) {
                        BookingStep.Battery.index
                    } else {
                        BookingStep.Station.index
                    }
                    batteryId = ""
                    energyKwh = ""
                    visitStart = defaultVisitStartLocal()
                    visitEnd = defaultVisitEndLocal()
                    error = null
                    reloadSlots()
                },
            )
            return
        }

        StepProgressBar(currentIndex = step)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loadingSlots) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Grid700,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading available batteries…",
                        color = Slate600,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (slotsError != null) {
                Text(
                    text = slotsError!!,
                    color = ErrorRed800,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRed50)
                        .border(1.dp, ErrorRed200, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                )
                OutlinedButton(
                    onClick = { reloadSlots() },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Slate300),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                ) {
                    Text("Retry", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = ErrorRed800,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRed50)
                        .border(1.dp, ErrorRed200, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                )
            }

            if (!loadingSlots && slotsError == null) {
                when (currentStep) {
                    BookingStep.Station -> StepStationContent(
                        stations = stations,
                        stationId = stationId,
                        onSelect = {
                            stationId = it
                            batteryId = ""
                            energyKwh = ""
                            error = null
                        },
                    )
                    BookingStep.Battery -> StepBatteryContent(
                        stationName = selectedStation?.name,
                        batteries = batteries,
                        batteryId = batteryId,
                        onSelect = {
                            batteryId = it
                            error = null
                        },
                    )
                    BookingStep.Details -> StepDetailsContent(
                        reservationType = reservationType,
                        onTypeChange = {
                            reservationType = it
                            error = null
                        },
                        energyKwh = energyKwh,
                        onEnergyChange = {
                            energyKwh = it
                            error = null
                        },
                        availableForType = availableForType,
                        batterySelected = batteryId.isNotBlank(),
                        visitStart = visitStart,
                        onVisitStartChange = {
                            visitStart = it
                            error = null
                        },
                        visitEnd = visitEnd,
                        onVisitEndChange = {
                            visitEnd = it
                            error = null
                        },
                        openTime = selectedBattery?.openTime.orEmpty().ifBlank { "08:00" },
                        closeTime = selectedBattery?.closeTime.orEmpty().ifBlank { "18:00" },
                        workingDays = selectedBattery?.workingDays.orEmpty(),
                    )
                    BookingStep.Review -> StepReviewContent(
                        stationName = selectedStation?.name,
                        battery = selectedBattery,
                        reservationType = reservationType,
                        energyKwh = energyKwh,
                        visitStart = visitStart,
                        visitEnd = visitEnd,
                    )
                }
            }
        }

        WizardFooter(
            step = currentStep,
            loading = loading,
            primaryEnabled = !loadingSlots && slotsError == null && when (currentStep) {
                BookingStep.Station -> stationId.isNotBlank()
                BookingStep.Battery -> batteryId.isNotBlank()
                BookingStep.Details -> energyKwh.isNotBlank()
                BookingStep.Review -> true
            },
            onBack = ::goBackStep,
            onPrimary = {
                if (currentStep == BookingStep.Review) submit() else goNext()
            },
        )
    }
}

private fun groupStations(slots: List<AvailableBookingSlotDto>): List<StationOption> {
    val byId = linkedMapOf<String, StationOption>()
    for (slot in slots) {
        if (slot.stationId.isBlank()) continue
        val existing = byId[slot.stationId]
        if (existing != null) {
            byId[slot.stationId] = existing.copy(
                batteryCount = existing.batteryCount + 1,
                chargeAvail = existing.chargeAvail + slot.availableChargingKwh,
                dropOffAvail = existing.dropOffAvail + slot.availableDropOffKwh,
            )
        } else {
            byId[slot.stationId] = StationOption(
                id = slot.stationId,
                name = slot.stationName.ifBlank { "Unknown station" },
                batteryCount = 1,
                chargeAvail = slot.availableChargingKwh,
                dropOffAvail = slot.availableDropOffKwh,
            )
        }
    }
    return byId.values.sortedBy { it.name.lowercase() }
}

@Composable
private fun WizardTopBar(
    step: BookingStep,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Book energy",
                color = Grid900,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Close",
                color = Grid700,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Step ${step.index + 1} of ${BookingStep.entries.size} · ${step.subtitle}",
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StepProgressBar(currentIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookingStep.entries.forEachIndexed { index, bookingStep ->
            val reached = index <= currentIndex
            val completed = index < currentIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (reached) Grid700 else Color.White)
                        .border(1.dp, if (reached) Grid700 else Slate300, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (completed) "✓" else "${index + 1}",
                        color = if (reached) Color.White else Slate600,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookingStep.title.split(" ").last(),
                    color = if (reached) Grid800 else Slate600,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            if (index < BookingStep.entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (index < currentIndex) Grid500 else Slate300)
                )
            }
        }
    }
    HorizontalDivider(
        color = Color(0xFFE2E8F0),
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun WizardFooter(
    step: BookingStep,
    loading: Boolean,
    primaryEnabled: Boolean,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = when (step) {
        BookingStep.Review -> if (loading) "Creating…" else "Submit reservation"
        else -> "Continue"
    }
    val backLabel = when (step) {
        BookingStep.Station -> "Cancel"
        else -> "Back"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(BorderStroke(1.dp, Grid100))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Slate300),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
        ) {
            Text(backLabel, style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = onPrimary,
            enabled = !loading && primaryEnabled,
            modifier = Modifier
                .weight(1.5f)
                .height(46.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Grid700,
                contentColor = Color.White,
                disabledContainerColor = Grid700.copy(alpha = 0.45f),
            ),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StepStationContent(
    stations: List<StationOption>,
    stationId: String,
    onSelect: (String) -> Unit,
) {
    StepTitle(BookingStep.Station.title)
    if (stations.isEmpty()) {
        EmptyHint("No stations currently have bookable batteries.")
    } else {
        stations.forEach { station ->
            StationPickCard(
                station = station,
                selected = station.id == stationId,
                onClick = { onSelect(station.id) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepBatteryContent(
    stationName: String?,
    batteries: List<AvailableBookingSlotDto>,
    batteryId: String,
    onSelect: (String) -> Unit,
) {
    StepTitle(BookingStep.Battery.title)
    if (stationName != null) {
        Text(
            text = stationName,
            color = Grid700,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
    if (stationName == null) {
        EmptyHint("Go back and select a station first.")
    } else if (batteries.isEmpty()) {
        EmptyHint("No batteries available at this station.")
    } else {
        batteries.forEach { battery ->
            BatteryPickCard(
                battery = battery,
                selected = battery.id == batteryId,
                onClick = { onSelect(battery.id) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepDetailsContent(
    reservationType: ReservationTypeUi,
    onTypeChange: (ReservationTypeUi) -> Unit,
    energyKwh: String,
    onEnergyChange: (String) -> Unit,
    availableForType: Double,
    batterySelected: Boolean,
    visitStart: String,
    onVisitStartChange: (String) -> Unit,
    visitEnd: String,
    onVisitEndChange: (String) -> Unit,
    openTime: String = "08:00",
    closeTime: String = "18:00",
    workingDays: List<String> = emptyList(),
) {
    StepTitle(BookingStep.Details.title)
    val hoursLabel = "$openTime–$closeTime"
    val daysLabel = if (workingDays.isEmpty()) {
        "station working days"
    } else {
        workingDays.joinToString(", ")
    }

    Text(
        text = "TYPE",
        color = Slate700,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ReservationTypeUi.entries.forEach { type ->
            TypeChip(
                type = type,
                selected = reservationType == type,
                onClick = { onTypeChange(type) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Grid600,
        unfocusedBorderColor = Slate300,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = Grid700,
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
        disabledBorderColor = Slate300,
        disabledContainerColor = Color(0xFFF8FAFC),
        disabledTextColor = Slate600,
    )

    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(
        if (batterySelected) {
            "Energy (kWh) — up to ${"%.2f".format(availableForType)} available"
        } else {
            "Energy (kWh)"
        }
    )
    OutlinedTextField(
        value = energyKwh,
        onValueChange = onEnergyChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = batterySelected,
        placeholder = { Text("e.g. 5", color = Slate600.copy(alpha = 0.7f)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
        shape = RoundedCornerShape(8.dp),
        colors = fieldColors,
    )

    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel("Visit start")
    OutlinedTextField(
        value = visitStart,
        onValueChange = onVisitStartChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("YYYY-MM-DD HH:mm", color = Slate600.copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(8.dp),
        colors = fieldColors,
    )

    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel("Visit end")
    OutlinedTextField(
        value = visitEnd,
        onValueChange = onVisitEndChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("YYYY-MM-DD HH:mm", color = Slate600.copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(8.dp),
        colors = fieldColors,
    )

    Text(
        text = "Visit must fall on $daysLabel between $hoursLabel. " +
            "Bookings must be within 7 days. Updates/cancels need ≥ 12 hours’ notice.",
        color = Slate600,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun StepReviewContent(
    stationName: String?,
    battery: AvailableBookingSlotDto?,
    reservationType: ReservationTypeUi,
    energyKwh: String,
    visitStart: String,
    visitEnd: String,
) {
    StepTitle(BookingStep.Review.title)
    Text(
        text = "Check everything looks right, then submit. Status will start as Pending.",
        color = Slate600,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        ReviewRow("Station", stationName ?: "—")
        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 10.dp))
        ReviewRow("Battery", battery?.let { "#${it.batteryIndex}" } ?: "—")
        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 10.dp))
        ReviewRow("Type", reservationType.label)
        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 10.dp))
        ReviewRow("Energy", "$energyKwh kWh")
        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 10.dp))
        ReviewRow("Visit start", visitStart)
        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 10.dp))
        ReviewRow("Visit end", visitEnd)
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Slate600, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = Slate900,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 12.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        color = Grid900,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun SuccessPanel(
    stationName: String,
    reservationCode: String?,
    onDone: () -> Unit,
    onCreateAnother: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Grid100, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Reservation submitted",
                color = Grid900,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildString {
                    append("Your booking at $stationName is Pending until a Grid Operator approves it.")
                    if (!reservationCode.isNullOrBlank()) {
                        append(" Code: $reservationCode.")
                    }
                    append(" QR appears after approval.")
                },
                color = Slate700,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Grid700,
                    contentColor = Color.White,
                ),
            ) {
                Text("View my bookings", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCreateAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Slate300),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
            ) {
                Text("Create another", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        color = Slate600,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Slate300), RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
    )
}

@Composable
private fun StationPickCard(
    station: StationOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Grid50 else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Grid600 else Grid100,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = Grid900,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${station.batteryCount} batteries available",
                    color = Slate600,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (selected) {
                Text(
                    text = "SELECTED",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Grid700)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Charge ${"%.1f".format(station.chargeAvail)} kWh",
                color = Slate700,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Drop-off ${"%.1f".format(station.dropOffAvail)} kWh",
                color = Slate700,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BatteryPickCard(
    battery: AvailableBookingSlotDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val fillPct = if (battery.capacityKwh > 0) {
        ((battery.actualEnergyKwh / battery.capacityKwh) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Grid50 else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Grid600 else Grid100,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Battery #${battery.batteryIndex}",
                color = Grid900,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            if (selected) {
                Text(
                    text = "SELECTED",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Grid700)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Stored", color = Slate600, style = MaterialTheme.typography.bodySmall)
            Text(
                "${battery.actualEnergyKwh} / ${battery.capacityKwh} kWh",
                color = Slate700,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillPct / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Grid600)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Grid100, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text("Charge avail", color = Slate600, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${battery.availableChargingKwh} kWh",
                        color = Grid800,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Grid100, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text("Drop-off avail", color = Slate600, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${battery.availableDropOffKwh} kWh",
                        color = Grid800,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(
    type: ReservationTypeUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Grid50 else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Grid600 else Grid100,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = type.label,
            color = Grid900,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = type.hint,
            color = Slate600,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Slate700,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CreateReservationPreview() {
    GridSyncMobileTheme {
        // Preview requires GridSyncApp — UI shell only in editor.
        Box(Modifier.fillMaxSize().background(Grid50)) {
            Text(
                "CreateReservationScreen",
                modifier = Modifier.align(Alignment.Center),
                color = Grid900,
            )
        }
    }
}
