#!/usr/bin/env python3
"""Offline project-integrity checks. This is NOT Kotlin compilation or a unit-test runner."""
from pathlib import Path
import hashlib
import re
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parent.parent
android = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
permissions = {item.attrib[android + "name"] for item in manifest.findall("uses-permission")}
assert permissions == {"android.permission.POST_NOTIFICATIONS"}, permissions
assert not manifest.findall("application/service"), "Unexpected Android service"
assert not manifest.findall("application/receiver"), "Unexpected manifest receiver"
assert manifest.find("application").attrib[android + "allowBackup"] == "false"
xmls = list((root / "app/src/main").rglob("*.xml"))
for file in xmls:
    ET.parse(file)
print(f"PASS: {len(xmls)} Android XML files parse; only posting permission; no service/listener or manifest receiver; backup disabled")

wrapper = root / "gradle/wrapper/gradle-wrapper.jar"
expected = "2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"
assert hashlib.sha256(wrapper.read_bytes()).hexdigest() == expected
with zipfile.ZipFile(wrapper) as jar:
    assert jar.testzip() is None
    assert "org/gradle/wrapper/GradleWrapperMain.class" in jar.namelist()
properties = (root / "gradle/wrapper/gradle-wrapper.properties").read_text()
assert "distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6" in properties
assert "gradle-8.11.1-bin.zip" in properties
print("PASS: Gradle wrapper JAR checksum, ZIP integrity, entrypoint and distribution checksum pin")

for source in (root / "app/src").rglob("*.kt"):
    content = source.read_text()
    assert "package com.watchdoc.prototype" in content, source
    assert not re.search(r"\b(TODO|NotImplementedError)\b", content), source
    assert not re.search(r"android\.bluetooth|com\.google\.android\.wearable", content), source
tests = list((root / "app/src/test").rglob("*.kt"))
instrumented = list((root / "app/src/androidTest").rglob("*.kt"))
print(f"PASS: Kotlin source files present with package namespace and no unfinished-code markers or Bluetooth/Wear imports")
print(f"INFO: {sum(len(re.findall(r'@Test\b', f.read_text())) for f in tests)} JVM tests and {sum(len(re.findall(r'@Test\b', f.read_text())) for f in instrumented)} instrumented test authored; NOT executed by this script")
