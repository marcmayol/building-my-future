package com.marc.gymplan100.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.marc.gymplan100.data.PlatformContext
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// La app pone Locale.getDefault() con el idioma elegido (AppLanguage.wrap).
actual fun separadorDecimal(): Char = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator

actual fun separadorMiles(): Char = DecimalFormatSymbols.getInstance(Locale.getDefault()).groupingSeparator

actual fun formatearFecha(millis: Long, patron: String): String =
    SimpleDateFormat(patron, Locale.getDefault()).format(Date(millis))

@Composable
actual fun platformContext(): PlatformContext = LocalContext.current

@Composable
actual fun anchoPantallaDp(): Int = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
