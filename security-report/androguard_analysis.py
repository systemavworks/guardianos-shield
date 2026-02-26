#!/usr/bin/env python3
"""
Análisis de seguridad con Androguard — Guardianos Shield v1.1.0
"""
import sys
import os

APK_PATH = "app/build/outputs/apk/release/guardianos-shield-v1.1.0-release.apk"
OUTPUT   = "security-report/05_androguard.txt"

try:
    from androguard.misc import AnalyzeAPK
    from androguard.core.apk import APK
except ImportError as e:
    print(f"ERROR importando androguard: {e}")
    sys.exit(1)

lines = []
def log(msg=""):
    print(msg)
    lines.append(msg)

log("=" * 70)
log("  ANDROGUARD — Análisis estático APK release")
log("=" * 70)

# Carga APK
log("\n[*] Cargando APK...")
a, d, dx = AnalyzeAPK(APK_PATH)
log(f"  Paquete     : {a.get_package()}")
log(f"  Versión     : {a.get_androidversion_name()} (code={a.get_androidversion_code()})")
log(f"  minSdk      : {a.get_min_sdk_version()}")
log(f"  targetSdk   : {a.get_target_sdk_version()}")

# --- 5a. Permisos y análisis de uso ---
log("\n## 5a. PERMISOS DECLARADOS")
for perm in sorted(a.get_permissions()):
    log(f"  {perm}")

log("\n## 5b. PERMISOS PELIGROSOS (Dangerous/Signature)")
dangerous_perms = [
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.QUERY_ALL_PACKAGES",
    "android.permission.KILL_BACKGROUND_PROCESSES",
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    "android.permission.PACKAGE_USAGE_STATS",
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.READ_PHONE_STATE",
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.READ_EXTERNAL_STORAGE",
]
app_perms = set(a.get_permissions())
for p in dangerous_perms:
    if p in app_perms:
        log(f"  [!] {p}")

# --- 5c. Activities, Services, Receivers exportados ---
log("\n## 5c. COMPONENTES EXPORTADOS")
log("  Activities exportadas:")
for act in a.get_activities():
    log(f"    {act}")

log("  Services exportados:")
for svc in a.get_services():
    log(f"    {svc}")

log("  Receivers exportados:")
for recv in a.get_receivers():
    log(f"    {recv}")

log("  Providers:")
for prov in a.get_providers():
    log(f"    {prov}")

# --- 5d. Strings sospechosas (crypto, hardcoded) ---
log("\n## 5d. STRINGS RELEVANTES EN BYTECODE")
suspicious_patterns = [
    "password", "passwd", "secret", "api_key", "apikey",
    "private_key", "BEGIN RSA", "BEGIN PRIVATE", "AES", "DES",
    "MD5", "SHA1", "ECB", "BASE64", "/sdcard/", "http://",
]

pkg_prefix = "com/guardianos/shield"
found_strings = set()
for cls in dx.get_classes():
    cls_name = cls.orig_name
    if not (cls_name.startswith("com/guardianos") or cls_name.startswith("Lcom/guardianos")):
        continue
    for method in cls.get_methods():
        try:
            for _, inst in method.get_method().get_instructions():
                pass
        except:
            pass
    # Buscar strings en el análisis
    for field in cls.get_fields():
        for _, val in field.get_xref_read():
            pass

# Alternativa: buscar strings en todos los métodos de clases propias
log("  [Buscando strings en clases com.guardianos.shield...]")
count = 0
for cls in dx.get_classes():
    cls_name = str(cls.orig_name)
    if "guardianos/shield" not in cls_name and "guardianos\\shield" not in cls_name:
        continue
    for method in cls.get_methods():
        try:
            m = method.get_method()
            if m is None:
                continue
            code = m.get_code()
            if code is None:
                continue
            for inst in code.get_bc().get_instructions():
                inst_str = str(inst)
                for pattern in suspicious_patterns:
                    if pattern.lower() in inst_str.lower():
                        if inst_str not in found_strings:
                            found_strings.add(inst_str)
                            log(f"  [!] {cls_name} -> {inst_str[:120]}")
                            count += 1
        except Exception:
            pass
if count == 0:
    log("  ✅ No se encontraron strings sospechosas en classes propias")

# --- 5e. Análisis de métodos crypto y TLS ---
log("\n## 5e. USO DE APIs CRYPTO Y TLS")
crypto_classes = [
    "Ljavax/crypto/Cipher",
    "Ljavax/crypto/SecretKeyFactory",
    "Ljava/security/MessageDigest",
    "Ljavax/net/ssl/SSLContext",
    "Ljavax/net/ssl/TrustManager",
    "Ljavax/net/ssl/X509TrustManager",
    "Ljavax/net/ssl/HostnameVerifier",
    "Lcom/google/crypto/tink",
]
for cls in dx.get_classes():
    cls_name = str(cls.orig_name)
    for crypto_cls in crypto_classes:
        if crypto_cls.replace("L","").replace(";","") in cls_name:
            usages = list(cls.get_xref_from())
            if usages:
                # Solo mostrar si una clase propia lo usa
                for ref_cls, ref_meth, _ in usages:
                    ref_name = str(ref_cls.orig_name)
                    if "guardianos" in ref_name:
                        log(f"  {ref_name} usa {cls_name}")
                        break

# --- 5f. Obfuscación R8 ---
log("\n## 5f. ESTADO DE OBFUSCACIÓN R8/ProGuard")
total_classes = len(list(dx.get_classes()))
obfuscated = sum(1 for c in dx.get_classes()
                 if len(str(c.orig_name).split("/")[-1].replace(";","")) <= 3)
pct = obfuscated * 100 // total_classes if total_classes > 0 else 0
log(f"  Total clases en DEX : {total_classes}")
log(f"  Clases obfuscadas   : {obfuscated} ({pct}%)")
log(f"  R8 activo           : {'✅ SÍ' if pct > 20 else '❌ NO'}")

# --- 5g. Resumen de intents actions ---
log("\n## 5g. INTENT ACTIONS DETECTADAS EN BYTECODE")
intent_actions = set()
for cls in dx.get_classes():
    for method in cls.get_methods():
        try:
            m = method.get_method()
            if m is None: continue
            code = m.get_code()
            if code is None: continue
            for inst in code.get_bc().get_instructions():
                s = str(inst)
                if "android.intent.action" in s or "com.guardianos" in s:
                    val = s.split('"')[1] if '"' in s else s
                    intent_actions.add(val[:100])
        except:
            pass
for action in sorted(intent_actions)[:25]:
    log(f"  {action}")

log("\n" + "=" * 70)
log("  Análisis androguard completado")
log("=" * 70)

with open(OUTPUT, "w") as f:
    f.write("\n".join(lines))

print(f"\n[✅] Informe guardado en {OUTPUT}")
