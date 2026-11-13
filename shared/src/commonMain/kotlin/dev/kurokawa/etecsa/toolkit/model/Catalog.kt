package dev.kurokawa.etecsa.toolkit.model

/** Service catalogue — mirrors the original app's own home list (extracted
 *  from RoCla USSD 5.4.3, MainActivity service table) with its exact USSD
 *  codes, regrouped into categories (Ernest). Titles stay in Spanish:
 *  they are ETECSA's service names. */
enum class Category(val en: String, val es: String) {
    QUERIES("Queries", "Consultas"),
    PLANS("Plans & purchases", "Planes y compras"),
    MONEY("Transfers & top-ups", "Transferencias y recargas"),
    MESSAGING("Messaging", "Mensajería"),
    CALLS("Calls", "Llamadas"),
    NAUTA("Nauta", "Nauta"),
    ONLINE("ETECSA online services", "Servicios ETECSA en línea"),
    NETWORK("Network", "Red"),
    TOOLS("Tools & protection", "Herramientas y protección"),
}

data class ServiceItem(
    val id: String, val category: Category,
    val titleEs: String, val descEs: String,
    /** A USSD code, a "tel:" number, or a screen id prefixed with "screen:". */
    val action: String,
    val inputLabel: String? = null,
    val inputIsCall: Boolean = false,
)

val Services = listOf(
    // Consultas — codes verbatim from the original
    ServiceItem("saldo", Category.QUERIES, "Saldo", "Saldo principal, recursos y vigencia.", "*222#"),
    ServiceItem("datos", Category.QUERIES, "Datos", "Consulta los datos disponibles y su vigencia.", "*222*328#"),
    ServiceItem("bonos", Category.QUERIES, "Bonos", "Bonos promocionales y planes asociados.", "*222*266#"),
    ServiceItem("minutos", Category.QUERIES, "Consultar min", "Consulta minutos de voz y vigencia.", "*222*869#"),
    ServiceItem("smsq", Category.QUERIES, "Consultar SMS", "Consulta SMS disponibles y vigencia.", "*222*767#"),
    ServiceItem("pospago", Category.QUERIES, "Saldo pospago", "Consulta líneas pospago o institucionales.", "*111#"),
    ServiceItem("disponibilidad", Category.QUERIES, "Disponibilidad datos", "Comprueba disponibilidad del servicio de datos.", "*222*468#"),
    ServiceItem("cuando_recargar", Category.QUERIES, "Cuándo recargar", "Recargas nacionales, límite y próxima fecha.", "*222*732#"),
    // Planes
    ServiceItem("comprar", Category.PLANS, "Comprar plan", "Elige un plan y cómpralo.", "screen:plans"),
    ServiceItem("amigos", Category.PLANS, "Plan Amigos", "Consulta y gestiona tu Plan Amigos.", "screen:friends"),
    ServiceItem("tarifa", Category.PLANS, "Tarifa por consumo", "Activa o desactiva la tarifa por consumo.", "*133*5#"),
    // Dinero
    ServiceItem("transferir", Category.MONEY, "Transferir saldo", "Número, PIN e importe en un solo paso.", "screen:transfer"),
    ServiceItem("adelanta", Category.MONEY, "Adelanta Saldo", "Solicita un adelanto de 25 o 50 CUP.", "screen:advance"),
    ServiceItem("recarga_tarjeta", Category.MONEY, "Recargar con tarjeta", "Introduce el número de una tarjeta de recarga.", "*662*{input}#", "Número de tarjeta"),
    ServiceItem("recarga_llamada", Category.MONEY, "Recargar por llamada", "Sistema automatizado de recargas.", "tel:*666"),
    ServiceItem("cambiar_pin", Category.MONEY, "Cambiar PIN de transferencia", "Modifica tu PIN mediante Cubacel.", "*234*3*1#"),
    ServiceItem("cards", Category.MONEY, "Tarjetas", "Guarda tus tarjetas de recarga de forma segura.", "screen:cards"),
    // Mensajería
    ServiceItem("mensajes", Category.MESSAGING, "Mensajes SMS", "Bandeja, envío y mensajes programados.", "screen:messages"),
    ServiceItem("smsc", Category.MESSAGING, "Centro de mensajes (SMSC)", "Consulta o edita el SMSC de tu SIM.", "screen:smsc"),
    // Llamadas
    ServiceItem("llamada_normal", Category.CALLS, "Llamada normal", "Llama directamente a cualquier número.", "screen:call"),
    ServiceItem("cobrar", Category.CALLS, "Llamar con 99", "Llama a otro móvil Cubacel y paga quien recibe.", "*99{input}", "Número de teléfono", true),
    ServiceItem("privada", Category.CALLS, "Llamada privada", "Oculta tu identificador para esta llamada.", "#31#{input}", "Número de teléfono", true),
    ServiceItem("call_blocker", Category.CALLS, "Bloqueador de llamadas", "Bloquea, silencia y excluye llamadas según tus reglas.", "screen:callblocker"),
    ServiceItem("imei", Category.CALLS, "Mostrar IMEI", "Código estándar para mostrar el IMEI.", "*#06#"),
    // Nauta
    ServiceItem("nauta", Category.NAUTA, "Nauta", "Conecta y controla tu sesión Wi-Fi de ETECSA.", "screen:nauta"),
    ServiceItem("nauta_mailbox", Category.NAUTA, "Buzón Nauta", "Consulta el espacio usado de tu correo Nauta.", "screen:nautamail"),
    // En línea
    ServiceItem("etecsa_services", Category.ONLINE, "Servicios ETECSA", "Sincroniza tus recursos mediante Internet.", "screen:online"),
    ServiceItem("numeros_utiles", Category.ONLINE, "Números útiles", "Emergencias y servicios públicos de Cuba.", "screen:useful"),
    // Red
    ServiceItem("lte_monitor", Category.NETWORK, "Monitor de red", "Te avisa — con alarma — cuando vuelve la cobertura celular.", "screen:network"),
    ServiceItem("traffic_monitor", Category.NETWORK, "Monitor de velocidad", "Indicador de velocidad y consumo de datos.", "screen:network"),
    ServiceItem("speed", Category.NETWORK, "Prueba de velocidad", "Mide descarga, subida, ping y jitter (Cloudflare).", "screen:speed"),
    ServiceItem("firewall", Category.NETWORK, "Cortafuegos", "Controla el acceso a la red de tus aplicaciones.", "screen:firewall"),
    ServiceItem("forzar_lte", Category.NETWORK, "Forzar LTE", "Abre la información de radio (RadioInfo).", "screen:forcelte"),
    ServiceItem("monitor_servicios", Category.NETWORK, "Radar de servicios", "Estado de plataformas y conectividad en Cuba.", "screen:online"),
    // Herramientas
    ServiceItem("antifraud", Category.TOOLS, "Protect — Antifraude", "Protección contra fraudes en SMS, llamadas y pagos.", "screen:antifraud"),
    ServiceItem("backup", Category.TOOLS, "Copia de seguridad", "Copia cifrada de historial, tarjetas y ajustes.", "screen:backup"),
)

data class Plan(val id: String, val category: String, val title: String, val detail: String, val price: String, val code: String)

/** The original's public catalogue (plans.json v1, verified 2026-11-13). */
val Plans = listOf(
    Plan("data_45", "data", "4.5 GB", "Sin min ni SMS · 300 MB .cu", "240 CUP", "*133*1*4*1*1#"),
    Plan("data_2", "data", "2 GB", "15 min · 20 SMS · 300 MB .cu", "120 CUP", "*133*1*4*2*1#"),
    Plan("data_4", "data", "4 GB", "35 min · 40 SMS · 300 MB .cu", "240 CUP", "*133*1*4*3*1#"),
    Plan("data_6", "data", "6 GB", "60 min · 70 SMS · 300 MB .cu", "360 CUP", "*133*1*4*4*1#"),
    Plan("data_lte", "data", "Datos LTE", "Paquete de datos solo LTE", "—", "*133*1*1*2#"),
    Plan("data_todus", "data", "1 GB toDus", "Solo toDus y Correo Nauta · 35 días", "25 CUP", "*133*1*2*1#"),
    Plan("data_daily", "data", "200 MB · Bolsa diaria", "Todas las redes · 24 horas", "25 CUP", "*133*1*3*1#"),
    Plan("voice_5", "voice", "5 min", "Voz nacional · 35 días", "37.50 CUP", "*133*3*1*1#"),
    Plan("voice_10", "voice", "10 min", "Voz nacional · 35 días", "72.50 CUP", "*133*3*2*1#"),
    Plan("voice_15", "voice", "15 min", "Voz nacional · 35 días", "105 CUP", "*133*3*3*1#"),
    Plan("voice_25", "voice", "25 min", "Voz nacional · 35 días", "162.50 CUP", "*133*3*4*1#"),
    Plan("voice_40", "voice", "40 min", "Voz nacional · 35 días", "250 CUP", "*133*3*5*1#"),
    Plan("sms_20", "sms", "20 SMS", "Mensajes nacionales · 35 días", "15 CUP", "*133*2*1*1#"),
    Plan("sms_50", "sms", "50 SMS", "Mensajes nacionales · 35 días", "30 CUP", "*133*2*2*1#"),
    Plan("sms_90", "sms", "90 SMS", "Mensajes nacionales · 35 días", "50 CUP", "*133*2*3*1#"),
    Plan("sms_120", "sms", "120 SMS", "Mensajes nacionales · 35 días", "60 CUP", "*133*2*4*1#"),
)

/** Official sender catalogue for Protect (public facts about ETECSA brands). */
val OfficialSenders = setOf(
    "ETECSA", "Cubacel", "PAGOxMOVIL", "PagoXMovil", "ENZONA", "383272", "2822235",
    "7246966845", "369662", "CUBACEL", "Nauta", "NAUTA", "Transfermovil", "TRANSFERMOVIL",
)

val UsefulNumbers = listOf(
    "106" to "Policía / Police", "105" to "Bomberos / Firefighters",
    "104" to "Ambulancias / Ambulance (SIUM)", "107" to "Guardacostas / Coast guard",
    "18888" to "ETECSA — atención / support", "*2266" to "Cubacel — atención / support",
)
