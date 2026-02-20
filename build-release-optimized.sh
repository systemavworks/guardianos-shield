#!/bin/bash
# Script optimizado para compilar release en hardware limitado
# Aspire E5-571G: 8GB RAM + HDD lento

echo "🚀 Iniciando build release optimizado..."
echo "📊 Recursos del sistema:"
free -h | head -2

# 1. Limpiar builds anteriores
echo ""
echo "🧹 Limpiando builds anteriores..."
./gradlew clean --no-daemon

# 2. Verificar memoria disponible
available_ram=$(free -m | awk 'NR==2 {print $7}')
echo ""
echo "💾 RAM disponible: ${available_ram}MB"

if [ $available_ram -lt 3000 ]; then
    echo "⚠️  Advertencia: RAM disponible baja (<3GB)"
    echo "    Considera cerrar otras aplicaciones"
    read -p "¿Continuar? (s/n): " confirm
    if [ "$confirm" != "s" ]; then
        echo "❌ Build cancelado"
        exit 1
    fi
fi

# 3. Build release
echo ""
echo "🔨 Compilando release (esto puede tardar 5-10 minutos)..."
echo "    ⏳ Monitoreando memoria... (Ctrl+C para cancelar)"
echo ""

# Monitor de memoria en background
(while true; do 
    used=$(free -m | awk 'NR==2 {printf "%.0f", ($3/$2)*100}')
    echo -ne "    RAM: ${used}%\r"
    sleep 2
done) &
monitor_pid=$!

# Ejecutar build con restricciones
./gradlew assembleRelease \
    --no-daemon \
    --max-workers=1 \
    --console=plain

build_result=$?

# Detener monitor
kill $monitor_pid 2>/dev/null

# 4. Resultado
echo ""
if [ $build_result -eq 0 ]; then
    apk_path="app/build/outputs/apk/release/guardianos-shield-v1.1.0-release.apk"
    if [ -f "$apk_path" ]; then
        size=$(du -h "$apk_path" | cut -f1)
        echo "✅ Build completado exitosamente!"
        echo "📦 APK: $apk_path"
        echo "📏 Tamaño: $size"
    else
        echo "✅ Build completado, buscando APK..."
        find app/build/outputs/apk/release/ -name "*.apk" -exec ls -lh {} \;
    fi
else
    echo "❌ Build falló con código: $build_result"
    echo "💡 Intenta:"
    echo "   - Cerrar otras aplicaciones"
    echo "   - Aumentar swap: sudo swapon --show"
    exit 1
fi
