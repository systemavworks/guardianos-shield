#!/usr/bin/env bash
# =============================================================================
# monitor-adb.sh — Monitorización completa de Guardianos Shield via ADB/logcat
#
# USO:
#   ./scripts/monitor-adb.sh                 → Monitor en tiempo real (todo)
#   ./scripts/monitor-adb.sh vpn             → Solo logs VPN/DNS
#   ./scripts/monitor-adb.sh crashes         → Solo crashes y ANRs
#   ./scripts/monitor-adb.sh monitor         → Solo AppMonitor/UsageStats
#   ./scripts/monitor-adb.sh browser         → Solo SafeBrowser/bloqueos
#   ./scripts/monitor-adb.sh install         → Instalar APK release ES y lanzar
#   ./scripts/monitor-adb.sh install-fresh   → Desinstalar + instalar + lanzar
#   ./scripts/monitor-adb.sh snapshot        → Captura de pantalla al PC
#   ./scripts/monitor-adb.sh report          → Guardar log completo en archivo
#   ./scripts/monitor-adb.sh permisos        → Mostrar permisos concedidos/denegados
#   ./scripts/monitor-adb.sh servicios       → Estado foreground services
#   ./scripts/monitor-adb.sh red             → Verificar DNS CleanBrowsing activo
# =============================================================================
set -euo pipefail

ADB=/home/victor/Android/Sdk/platform-tools/adb
APK=/home/victor/guardianos-shield/app/build/outputs/apk/langEs/release/guardianos-shield-v1.1.0-es-release.apk
PKG=com.guardianos.shield
LOGS_DIR=/home/victor/guardianos-shield/scripts/logs

BOLD='\033[1m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
RED='\033[0;31m'; CYAN='\033[0;36m'; MAGENTA='\033[0;35m'; NC='\033[0m'

log_ok()  { echo -e "${GREEN}✓${NC} $*"; }
log_warn(){ echo -e "${YELLOW}⚠${NC}  $*"; }
log_err() { echo -e "${RED}✗${NC}  $*"; }
sep()     { echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; }

check_device() {
    local d
    d=$($ADB devices | grep -v "List of" | grep "device$" | awk '{print $1}' | head -1)
    if [[ -z "$d" ]]; then
        log_err "No hay dispositivo conectado. Conecta el OPPO y activa depuración USB."
        exit 1
    fi
    local modelo fab android sdk
    fab=$($ADB shell getprop ro.product.manufacturer | tr -d '\r')
    modelo=$($ADB shell getprop ro.product.model | tr -d '\r')
    android=$($ADB shell getprop ro.build.version.release | tr -d '\r')
    sdk=$($ADB shell getprop ro.build.version.sdk | tr -d '\r')
    echo -e "${BOLD}Dispositivo:${NC} $fab $modelo  |  Android $android (API $sdk)  |  Serial: $d"
}

# =============================================================================
cmd_install() {
    local fresh="${1:-no}"
    sep; echo -e "${BOLD}INSTALACIÓN APK release langEs${NC}"; sep
    check_device

    [[ -f "$APK" ]] || { log_err "APK no encontrado: $APK\nEjecuta: ./gradlew assembleLangEsRelease"; exit 1; }
    log_ok "APK: $(du -h "$APK" | cut -f1)  →  $APK"

    if [[ "$fresh" == "fresh" ]]; then
        echo "Desinstalando versión anterior..."
        $ADB uninstall "$PKG" 2>/dev/null && log_ok "Desinstalado" || log_warn "No había instalación previa"
    fi

    echo "Instalando..."
    $ADB install -r "$APK" && log_ok "Instalado correctamente"

    echo "Lanzando app..."
    $ADB shell am start -n "$PKG/.MainActivity"
    log_ok "App lanzada. Revisa el dispositivo."
}

# =============================================================================
cmd_monitor() {
    local FILTRO="${1:-todo}"
    sep
    echo -e "${BOLD}MONITOR TIEMPO REAL${NC} — Filtro: ${CYAN}$FILTRO${NC}  (Ctrl+C para detener)"
    sep
    check_device
    $ADB logcat -c

    case "$FILTRO" in
        vpn|dns)
            echo -e "${YELLOW}Monitorizando: VPN · DNS · CleanBrowsing${NC}\n"
            $ADB logcat -v time 2>/dev/null | grep --line-buffered \
                -E "GuardianVPN|DnsFilter|VPN|Filtro DNS|CleanBrowsing|185\.228\.|VpnService|tun0"
            ;;
        crashes)
            echo -e "${RED}Monitorizando: CRASHES · ANRs · ERRORES FATALES${NC}\n"
            $ADB logcat -v time 2>/dev/null | grep --line-buffered \
                -E "FATAL EXCEPTION|AndroidRuntime|ANR|Application Not Responding|$PKG"
            ;;
        monitor|usage)
            echo -e "${YELLOW}Monitorizando: AppMonitor · UsageStats · Redirecciones${NC}\n"
            $ADB logcat -v time 2>/dev/null | grep --line-buffered \
                -E "UsageStats|AppMonitor|foreground|redirección|bloqueado|SensitiveApp"
            ;;
        browser|webview)
            echo -e "${MAGENTA}Monitorizando: SafeBrowser · Bloqueos · URLs${NC}\n"
            $ADB logcat -v time 2>/dev/null | grep --line-buffered \
                -E "SafeBrowser|WebView|bloqueado|ADULT|GAMBLING|VIOLENCE|SOCIAL_MEDIA|shouldOverride"
            ;;
        todo|*)
            echo -e "${GREEN}Monitorizando: TODO (VPN + Monitor + Browser + Crashes)${NC}\n"
            $ADB logcat -v time 2>/dev/null | grep --line-buffered \
                -E "GuardianVPN|DnsFilter|UsageStats|AppMonitor|SafeBrowser|GuardianosApp|FATAL EXCEPTION|AndroidRuntime|ANR|$PKG"
            ;;
    esac
}

# =============================================================================
cmd_red() {
    sep; echo -e "${BOLD}VERIFICACIÓN DNS CleanBrowsing${NC}"; sep
    check_device

    echo -e "\n${CYAN}▸ Servidores DNS activos en el dispositivo:${NC}"
    $ADB shell dumpsys connectivity 2>/dev/null | grep -i "dns\|DNS" | head -15

    echo -e "\n${CYAN}▸ Interfaces de red (buscar tun0 = VPN activa):${NC}"
    $ADB shell ip addr show 2>/dev/null | grep -E "tun0|UP|inet " | head -20

    echo -e "\n${CYAN}▸ Logs VPN recientes:${NC}"
    $ADB logcat -d -v time 2>/dev/null | grep -E "GuardianVPN|DnsFilter|185\.228|tun0|VPN" | tail -20

    echo -e "\n${CYAN}▸ Test DNS desde el dispositivo (debería resolverse si VPN activa):${NC}"
    $ADB shell nslookup google.com 2>/dev/null || \
    $ADB shell getprop | grep -i dns
}

# =============================================================================
cmd_permisos() {
    sep; echo -e "${BOLD}PERMISOS DE LA APP${NC}"; sep
    check_device

    echo -e "\n${CYAN}▸ Permisos concedidos:${NC}"
    $ADB shell dumpsys package "$PKG" 2>/dev/null | grep -A1 "granted=true" | grep "permission\." | head -30

    echo -e "\n${CYAN}▸ Permisos denegados:${NC}"
    $ADB shell dumpsys package "$PKG" 2>/dev/null | grep -A1 "granted=false" | grep "permission\." | head -20

    echo -e "\n${CYAN}▸ UsageStats (CRÍTICO para AppMonitor):${NC}"
    $ADB shell appops get "$PKG" GET_USAGE_STATS 2>/dev/null

    echo -e "\n${CYAN}▸ VPN preparada:${NC}"
    $ADB shell dumpsys vpn 2>/dev/null | grep -i "package\|state\|interface" | head -10

    echo -e "\n${CYAN}▸ Batería (optimización):${NC}"
    $ADB shell dumpsys deviceidle whitelist 2>/dev/null | grep "$PKG" || \
        log_warn "$PKG no está en whitelist de batería → puede ser matado en background"
}

# =============================================================================
cmd_servicios() {
    sep; echo -e "${BOLD}ESTADO FOREGROUND SERVICES${NC}"; sep
    check_device

    echo -e "\n${CYAN}▸ Procesos activos de la app:${NC}"
    $ADB shell ps -ef 2>/dev/null | grep "$PKG" | grep -v grep

    echo -e "\n${CYAN}▸ Foreground Services:${NC}"
    $ADB shell dumpsys activity services "$PKG" 2>/dev/null | grep -E "ServiceRecord|isForeground|Running|startForeground" | head -20

    echo -e "\n${CYAN}▸ Notificaciones activas (foreground service notification):${NC}"
    $ADB shell dumpsys notification 2>/dev/null | grep -A3 "$PKG" | head -30

    echo -e "\n${CYAN}▸ WorkManager jobs:${NC}"
    $ADB shell dumpsys jobscheduler 2>/dev/null | grep "$PKG" | head -20
}

# =============================================================================
cmd_snapshot() {
    sep; echo -e "${BOLD}CAPTURA DE PANTALLA${NC}"; sep
    check_device
    mkdir -p "$LOGS_DIR"
    local FECHA
    FECHA=$(date +"%Y%m%d_%H%M%S")
    local DEST="$LOGS_DIR/screenshot_$FECHA.png"
    $ADB shell screencap -p /sdcard/screen_tmp.png
    $ADB pull /sdcard/screen_tmp.png "$DEST"
    $ADB shell rm /sdcard/screen_tmp.png
    log_ok "Screenshot guardado en: $DEST"
    xdg-open "$DEST" 2>/dev/null || true
}

# =============================================================================
cmd_report() {
    sep; echo -e "${BOLD}GUARDANDO REPORTE COMPLETO DE LOGS${NC}"; sep
    check_device
    mkdir -p "$LOGS_DIR"
    local FECHA
    FECHA=$(date +"%Y%m%d_%H%M%S")
    local DEST="$LOGS_DIR/logcat_$FECHA.txt"

    {
        echo "=== REPORTE GUARDIANOS SHIELD ==="
        echo "Fecha: $(date)"
        echo "Dispositivo: $($ADB shell getprop ro.product.manufacturer | tr -d '\r') $($ADB shell getprop ro.product.model | tr -d '\r')"
        echo "Android: $($ADB shell getprop ro.build.version.release | tr -d '\r') (API $($ADB shell getprop ro.build.version.sdk | tr -d '\r'))"
        echo ""
        echo "=== LOGCAT COMPLETO ==="
        $ADB logcat -d -v time 2>/dev/null
    } > "$DEST"

    log_ok "Reporte guardado: $DEST  ($(du -h "$DEST" | cut -f1))"
}

# =============================================================================
cmd_help() {
    cat <<EOF

  ${BOLD}monitor-adb.sh${NC} — Monitorización Guardianos Shield sin Firebase

  ${CYAN}Instalación y arranque:${NC}
    install          Instalar APK release ES en el dispositivo y lanzar
    install-fresh    Desinstalar + instalar limpio + lanzar

  ${CYAN}Monitorización en tiempo real (Ctrl+C para detener):${NC}
    (sin args)       Todo: VPN + Monitor + Browser + Crashes
    vpn              Solo logs VPN y DNS CleanBrowsing
    crashes          Solo crashes, ANRs y errores fatales
    monitor          Solo AppMonitor y UsageStats (detección apps)
    browser          Solo SafeBrowser y bloqueos de URL

  ${CYAN}Diagnóstico:${NC}
    red              Verificar DNS CleanBrowsing activo en el dispositivo
    permisos         Ver permisos concedidos/denegados de la app
    servicios        Estado de foreground services y WorkManager
    snapshot         Captura de pantalla del dispositivo al PC
    report           Guardar log completo en scripts/logs/

  ${CYAN}Tags de log a vigilar:${NC}
    GuardianVPN      → Inicio/parada VPN, DNS 185.228.168.168/169
    UsageStats       → Apps detectadas en foreground, redirecciones
    SafeBrowser      → Bloqueos WebView, validaciones URL
    FATAL EXCEPTION  → Crashes de la app

EOF
}

# =============================================================================
CMD="${1:-todo}"
case "$CMD" in
    install)       cmd_install ;;
    install-fresh) cmd_install fresh ;;
    vpn|dns)       cmd_monitor vpn ;;
    crashes)       cmd_monitor crashes ;;
    monitor|usage) cmd_monitor monitor ;;
    browser|webview) cmd_monitor browser ;;
    red)           cmd_red ;;
    permisos)      cmd_permisos ;;
    servicios)     cmd_servicios ;;
    snapshot)      cmd_snapshot ;;
    report)        cmd_report ;;
    help|--help)   cmd_help ;;
    todo|*)        cmd_monitor todo ;;
esac
