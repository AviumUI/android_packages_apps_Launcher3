#!/usr/bin/env python3

import sqlite3
import re
import os
import sys
import shutil

DIR = "db_files"
AUTO_FILE = DIR + "/launcher.db"
LAUNCHER3_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_XML_PATH = os.path.join(LAUNCHER3_ROOT, "res/xml/default_workspace_5x5.xml")

CONTAINER_DESKTOP = -100
CONTAINER_HOTSEAT = -101


def usage():
    print("python3 export_layout.py [grid_size]")
    print()
    print("  python3 export_layout.py")
    print("  python3 export_layout.py 5x5")


def make_dir():
    shutil.rmtree(DIR, True)
    os.makedirs(DIR)


def adb_root_remount():
    os.system("adb root")
    os.system("adb remount")


def pull_file(fn):
    rv = os.system("adb pull /data/data/com.android.launcher3/databases/launcher.db " + fn)
    if rv != 0:
        print("adb pull error")
        sys.exit(1)
    print("adb pull success: " + fn)


def parse_intent(intent_str):
    if not intent_str:
        return None, None

    comp_match = re.search(r'component=([^/]+)/\.?([^;]+)', intent_str)
    if comp_match:
        package = comp_match.group(1)
        class_name = comp_match.group(2)
        if not class_name.startswith('.'):
            if not class_name.startswith(package):
                class_name = package + '.' + class_name
        else:
            class_name = package + class_name
        return package, class_name

    pkg_match = re.search(r'package=([^;]+)', intent_str)
    package = pkg_match.group(1) if pkg_match else None

    return package, None


def generate_xml(data):
    xml_content = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Copyright (C) 2009 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->

<favorites xmlns:launcher="http://schemas.android.com/apk/res-auto/com.android.launcher3">

'''

    xml_content += '    <!-- Hotseat -->\n'
    hotseat_items = [d for d in data if d.get('container') == CONTAINER_HOTSEAT]
    hotseat_items.sort(key=lambda x: x.get('screen', 0))

    for d in hotseat_items:
        screen = d.get('screen', 0)
        intent = d.get('intent', '')
        package, class_name = parse_intent(intent)

        if package and class_name:
            xml_content += f'''    <favorite
        launcher:container="-101"
        launcher:screen="{screen}"
        launcher:x="{screen}"
        launcher:y="0"
        launcher:packageName="{package}"
        launcher:className="{class_name}" />\n\n'''
    xml_content += '    <!-- Screen 0 -->\n'
    desktop_items = [d for d in data if d.get('container') == CONTAINER_DESKTOP]
    desktop_items.sort(key=lambda x: (x.get('cellY', 0), x.get('cellX', 0)))

    for d in desktop_items:
        screen = d.get('screen', 0)
        cellX = d.get('cellX', 0)
        cellY = d.get('cellY', 0)
        spanX = d.get('spanX', 1)
        spanY = d.get('spanY', 1)
        itemType = d.get('itemType', 0)
        intent = d.get('intent', '')
        appWidgetProvider = d.get('appWidgetProvider', '')

        if itemType == 4 or itemType == 6: 
            if appWidgetProvider and '/' in appWidgetProvider:
                parts = appWidgetProvider.split('/')
                package = parts[0]
                class_name = parts[1] if len(parts) > 1 else ''
                if class_name.startswith('.'):
                    class_name = package + class_name
                xml_content += f'''    <appwidget
        launcher:packageName="{package}"
        launcher:className="{class_name}"
        launcher:screen="{screen}"
        launcher:x="{cellX}"
        launcher:y="{cellY}"
        launcher:spanX="{spanX}"
        launcher:spanY="{spanY}" />\n\n'''
        else:  # App
            package, class_name = parse_intent(intent)
            if package and class_name:
                xml_content += f'''    <favorite
        launcher:screen="{screen}"
        launcher:x="{cellX}"
        launcher:y="{cellY}"
        launcher:packageName="{package}"
        launcher:className="{class_name}" />\n\n'''

    xml_content += '</favorites>\n'
    return xml_content


def process_db(db_file):
    conn = sqlite3.connect(db_file)
    c = conn.cursor()
    c.execute('SELECT * FROM favorites')
    columns = [d[0] for d in c.description]
    rows = c.fetchall()
    data = [dict(zip(columns, row)) for row in rows]

    print(f"found {len(data)} items")
    hotseat_count = len([d for d in data if d.get('container') == CONTAINER_HOTSEAT])
    desktop_count = len([d for d in data if d.get('container') == CONTAINER_DESKTOP])
    print(f"  - Hotseat: {hotseat_count} items")
    print(f"  - Desktop: {desktop_count} items")

    xml_content = generate_xml(data)
    return xml_content


def main(argv):
    grid_size = "5x5"
    if len(argv) > 1:
        if argv[1] in ['-h', '--help', 'help']:
            usage()
            sys.exit(0)
        grid_size = argv[1]

    print(f"=== Export workspace ({grid_size}) ===\n")

    make_dir()

    adb_root_remount()
    pull_file(AUTO_FILE)
    xml_content = process_db(AUTO_FILE)
    temp_xml = os.path.join(DIR, f"generated_workspace_{grid_size}.xml")
    with open(temp_xml, 'w', encoding='utf-8') as f:
        f.write(xml_content)
    print(f"\nGenerated temp file: {temp_xml}")

    target_xml = os.path.join(LAUNCHER3_ROOT, f"res/xml/default_workspace_{grid_size}.xml")
    shutil.copy(temp_xml, target_xml)
    print(f"Updated: {target_xml}")

    print("\n=== Layout summary ===")
    conn = sqlite3.connect(AUTO_FILE)
    c = conn.cursor()
    c.execute('SELECT title, intent, container, screen, cellX, cellY, itemType FROM favorites ORDER BY container, screen, cellY, cellX')
    for row in c.fetchall():
        title, intent, container, screen, x, y, item_type = row
        item_type_str = {0: 'app', 1: 'shortcut', 2: 'folder', 4: 'widget', 6: 'widget'}.get(item_type, 'unknown')
        if container == CONTAINER_HOTSEAT:
            print(f"Hotseat[{screen}]: {title} ({item_type_str})")
        else:
            print(f"Screen {screen} ({x},{y}): {title} ({item_type_str})")



    print("\n=== Completed ===")

if __name__ == "__main__":
    main(sys.argv)
