#!/bin/bash
# Script de testing rápido para GuardianOS Shield
# Verifica compatibilidad Android 12-15+

echo "╔════════════════════════════════════════════════════════════╗"
echo "║  GuardianOS Shield - Testing Compatibilidad Android       ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Verificar que hay un dispositivo conectado
if ! adb devices | grep -q "device$"; then
    echo "❌ No hay dispositivos conectados"
    echo "Conecta un dispositivo Android via USB o inicia un emulador"
    exit 1
fi

# Obtener información del dispositivo
ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
API_LEVEL=$(adb shell getprop ro.build.version.sdk)
MANUFACTURER=$(adb shell getprop ro.product.manufacturer)
MODEL=$(adb shell getprop ro.product.model)
BRAND=$(adb shell getprop ro.product.brand)

echo "📱 Dispositivo Detectado:"
echo "   • Android: $ANDROID_VERSION (API $API_LEVEL)"
echo "   • Fabricante: $MANUFACTURER"
echo "   • Modelo: $MODEL"
echo "   • Marca: $BRAND"
echo ""

# Verificar compatibilidad
if [ "$API_LEVEL" -lt 31 ]; then
    echo "⚠️  ADVERTENCIA: Android API $API_LEVEL < 31 (Android 12)"
    echo "   La app requiere Android 12+ para funcionar correctamente"
    exit 1
elif [ "$API_LEVEL" -ge 31 ] && [ "$API_LEVEL" -le 34 ]; then
    echo "✅ Android $API_LEVEL: Compatibilidad COMPLETA"
elif [ "$API_LEVEL" -ge 35 ]; then
    echo "✅ Android $API_LEVEL (Android 15+): Compatibilidad con mejoras específicas"
fi

echo ""
echo "────────────────────────────────────────────────────────────"
echo "🧪 Iniciando Tests Automáticos"
echo "────────────────────────────────────────────────────────────"
echo ""

# Limpiar logs
adb logcat -c

# Test 1: Verificar app instalada
echo "1️⃣  Verificando instalación..."
if adb shell pm list packages | grep -q "com.guardianos.shield"; then
    echo "   ✅ App instalada"
    APP_VERSION=$(adb shell dumpsys package com.guardianos.shield | grep versionName | head -1 | awk '{print $1}')
    echo "   📦 $APP_VERSION"
else
    echo "   ❌ App NO instalada"
    echo "   Ejecuta: ./gradlew installDebug"
    exit 1
fi
echo ""

# Test 2: Verificar permisos críticos
echo "2️⃣  Verificando permisos..."

# VPN
if adb shell dumpsys package com.guardianos.shield | grep -q "android.permission.BIND_VPN_SERVICE"; then
    echo "   ✅ Permiso VPN declarado"
else
    echo "   ❌ Permiso VPN NO declarado"
fi

# UsageStats
if adb shell appops get com.guardianos.shield PACKAGE_USAGE_STATS | grep -q "allow"; then
    echo "   ✅ Permiso UsageStats concedido"
else
    echo "   ⚠️  Permiso UsageStats NO concedido (configurar manualmente)"
fi

# Notificaciones (Android 13+)
if [ "$API_LEVEL" -ge 33 ]; then
    if adb shell dumpsys notification | grep "com.guardianos.shield" | grep -q "granted=true"; then
        echo "   ✅ Permiso Notificaciones concedido (Android 13+)"
    else
        echo "   ⚠️  Permiso Notificaciones NO concedido (Android 13+)"
    fi
fi
echo ""

# Test 3: Iniciar app y capturar logs
echo "3️⃣  Iniciando app y capturando logs de arranque..."
adb shell am start -n com.guardianos.shield/.MainActivity > /dev/null 2>&1
sleep 3

echo "   📋 Logs de inicialización:"
adb logcat -d | grep -E "MainActivity.*Información del Sistema" -A 6 | tail -7
echo ""

# Test 4: Verificar servicios en ejecución
echo "4️⃣  Verificando servicios en background..."
if adb shell dumpsys activity services | grep -q "DnsFilterService"; then
    echo "   ✅ DnsFilterService detectado"
else
    echo "   ℹ️  DnsFilterService no activo (normal si VPN no está activado)"
fi

if adb shell dumpsys activity services | grep -q "AppMonitorService"; then
    echo "   ✅ AppMonitorService detectado"
else
    echo "   ℹ️  AppMonitorService no activo (activar monitoreo en app)"
fi
echo ""

# Test 5: Test específico por versión Android
echo "5️⃣  Tests específicos para Android $API_LEVEL..."

if [ "$API_LEVEL" -ge 35 ]; then
    echo "   🧪 Android 15+: Verificando configuración VPN non-blocking..."
    adb logcat -d | grep "GuardianVPN.*non-blocking" | tail -1
    
    if adb logcat -d | grep -q "non-blocking configurado"; then
        echo "   ✅ Modo non-blocking configurado correctamente"
    else
        echo "   ⚠️  No se detectó configuración non-blocking (activar VPN en app)"
    fi
fi

if [ "$API_LEVEL" -ge 33 ]; then
    echo "   🧪 Android 13+: Verificando manejo de notificaciones..."
    NOTIF_CHANNELS=$(adb shell dumpsys notification | grep "com.guardianos.shield" | grep -c "NotificationChannel")
    echo "   📢 Canales de notificación creados: $NOTIF_CHANNELS"
fi
echo ""

# Test 6: Verificar logs de errores
echo "6️⃣  Buscando errores y warnings..."
ERROR_COUNT=$(adb logcat -d | grep -E "GuardianVPN.*❌|MainActivity.*ERROR|FATAL" | wc -l)
if [ "$ERROR_COUNT" -eq 0 ]; then
    echo "   ✅ Sin errores detectados"
else
    echo "   ⚠️  $ERROR_COUNT errores/warnings encontrados:"
    adb logcat -d | grep -E "GuardianVPN.*❌|MainActivity.*ERROR" | tail -5
fi
echo ""

# Resumen final
echo "────────────────────────────────────────────────────────────"
echo "📊 RESUMEN DE TESTING"
echo "────────────────────────────────────────────────────────────"

# Determinar estado general
OVERALL_STATUS="✅ PASS"
if [ "$API_LEVEL" -lt 31 ]; then
    OVERALL_STATUS="❌ FAIL - Android < 12"
elif [ "$ERROR_COUNT" -gt 5 ]; then
    OVERALL_STATUS="⚠️  WARNINGS - Revisar logs"
fi

echo "Estado general: $OVERALL_STATUS"
echo ""
echo "📝 Próximos pasos:"
echo "   1. Si aparecen warnings de permisos, configurarlos manualmente en app"
echo "   2. Activar VPN (Modo Avanzado) y verificar conectividad"
echo "   3. Probar redirección de navegadores externos"
echo "   4. Configurar horarios y verificar bloqueo fuera de horario"
echo ""
echo "🔍 Ver logs en tiempo real:"
echo "   adb logcat -c && adb logcat | grep -E 'GuardianVPN|UsageStatsMonitor|SafeBrowser'"
echo ""
echo "╚════════════════════════════════════════════════════════════╝"
