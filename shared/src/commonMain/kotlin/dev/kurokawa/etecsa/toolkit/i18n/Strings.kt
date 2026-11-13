package dev.kurokawa.etecsa.toolkit.i18n

/** Full EN + ES string tables (Ernest: both languages complete). ETECSA's own
 *  service/plan names stay in Spanish in both locales, like the original. */
data class Strings(
    val appName: String, val searchHint: String, val settings: String,
    val home: String, val messages: String, val calls: String, val network: String,
    val sim1: String, val sim2: String, val simForQueries: String,
    val balance: String, val consult: String, val consulting: String,
    val categories: String, val coverageAlarm: String, val coverageAlarmDesc: String,
    val coverageMonitor: String, val sendOnCoverage: String,
    val history: String, val edit: String, val run: String, val dial: String,
    val result: String, val close: String, val confirm: String, val cancel: String,
    val amount: String, val number: String, val pin: String, val send: String,
    val plans: String, val buy: String, val price: String,
    val appearance: String, val language: String, val darkTheme: String,
    val about: String, val aboutBody: String, val version: String,
    val noPermission: String, val empty: String, val allServices: String,
    val antifraudOk: String, val antifraudWarn: String,
    val speedTest: String, val download: String, val upload: String, val ping: String,
    val start: String, val stop: String, val active: String, val inactive: String,
    val backup: String, val restore: String, val update: String,
    val languageName: String, val systemDefault: String,
)

val EnStrings = Strings(
    appName = "ETECSA Toolkit", searchHint = "Search services, plans, contacts…",
    settings = "Settings", home = "Home", messages = "Messages", calls = "Calls", network = "Network",
    sim1 = "SIM 1", sim2 = "SIM 2", simForQueries = "SIM used for queries",
    balance = "Main balance", consult = "Check", consulting = "Asking ETECSA…",
    categories = "Services", coverageAlarm = "Coverage-back alarm",
    coverageAlarmDesc = "Sound an alarm when cellular coverage returns",
    coverageMonitor = "Coverage monitor", sendOnCoverage = "Send when coverage returns",
    history = "History", edit = "Edit", run = "Run USSD", dial = "Call",
    result = "ETECSA reply", close = "Close", confirm = "Confirm", cancel = "Cancel",
    amount = "Amount (CUP)", number = "Number", pin = "Transfer PIN", send = "Send",
    plans = "Plans", buy = "Buy", price = "Price",
    appearance = "Appearance", language = "Language", darkTheme = "Dark theme",
    about = "About", aboutBody = "A clean-room Material 3 Expressive toolkit for ETECSA/Cubacel lines. Every feature is free — no Pro, no trial. USSD codes and prices are ETECSA's published facts.",
    version = "Version", noPermission = "Permission missing — grant it in system Settings.",
    empty = "Nothing here yet", allServices = "All services",
    antifraudOk = "Sender matches the official catalogue", antifraudWarn = "Possible impersonation — verify before trusting",
    speedTest = "Speed test", download = "Download", upload = "Upload", ping = "Ping",
    start = "Start", stop = "Stop", active = "Active", inactive = "Inactive",
    backup = "Encrypted backup", restore = "Restore backup", update = "Check for updates",
    languageName = "English", systemDefault = "System",
)

val EsStrings = Strings(
    appName = "ETECSA Toolkit", searchHint = "Busca servicios, planes, contactos…",
    settings = "Ajustes", home = "Inicio", messages = "Mensajes", calls = "Llamadas", network = "Red",
    sim1 = "SIM 1", sim2 = "SIM 2", simForQueries = "SIM para las consultas",
    balance = "Saldo principal", consult = "Consultar", consulting = "Consultando a ETECSA…",
    categories = "Servicios", coverageAlarm = "Alarma de cobertura",
    coverageAlarmDesc = "Suena una alarma cuando vuelve la cobertura celular",
    coverageMonitor = "Monitor de cobertura", sendOnCoverage = "Enviar cuando vuelva la cobertura",
    history = "Historial", edit = "Editar", run = "Ejecutar USSD", dial = "Llamar",
    result = "Respuesta de ETECSA", close = "Cerrar", confirm = "Confirmar", cancel = "Cancelar",
    amount = "Importe (CUP)", number = "Número", pin = "PIN de transferencia", send = "Enviar",
    plans = "Planes", buy = "Comprar", price = "Precio",
    appearance = "Apariencia", language = "Idioma", darkTheme = "Tema oscuro",
    about = "Acerca de", aboutBody = "Herramientas Material 3 Expressive para líneas ETECSA/Cubacel, reescritas desde cero. Todas las funciones son gratis — sin Pro ni prueba. Los códigos USSD y precios son datos públicos de ETECSA.",
    version = "Versión", noPermission = "Falta un permiso — concédelo en los Ajustes del sistema.",
    empty = "Nada por aquí todavía", allServices = "Todos los servicios",
    antifraudOk = "El remitente coincide con el catálogo oficial", antifraudWarn = "Posible suplantación — verifica antes de confiar",
    speedTest = "Prueba de velocidad", download = "Descarga", upload = "Subida", ping = "Ping",
    start = "Iniciar", stop = "Detener", active = "Activo", inactive = "Inactivo",
    backup = "Copia cifrada", restore = "Restaurar copia", update = "Buscar actualizaciones",
    languageName = "Español", systemDefault = "Sistema",
)

fun stringsFor(lang: String): Strings = if (lang == "es") EsStrings else EnStrings
