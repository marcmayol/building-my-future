package com.marc.gymplan100.plataforma

import androidx.compose.runtime.Composable
import com.marc.gymplan100.data.IosContext
import com.marc.gymplan100.data.PlatformContext
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

/** El idioma con el que se pinta la app: el elegido en Ajustes o el del iPhone. */
var localeApp: NSLocale = NSLocale.currentLocale

actual fun separadorDecimal(): Char =
    NSNumberFormatter().apply { locale = localeApp }.decimalSeparator.firstOrNull() ?: '.'

actual fun separadorMiles(): Char =
    NSNumberFormatter().apply { locale = localeApp }.groupingSeparator.firstOrNull() ?: ','

actual fun formatearFecha(millis: Long, patron: String): String {
    val f = NSDateFormatter()
    f.locale = localeApp
    f.dateFormat = patron
    return f.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
}

@Composable
actual fun platformContext(): PlatformContext = IosContext

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
actual fun anchoPantallaDp(): Int {
    val ventana = androidx.compose.ui.platform.LocalWindowInfo.current
    val densidad = androidx.compose.ui.platform.LocalDensity.current
    return with(densidad) { ventana.containerSize.width.toDp().value.toInt() }
}
