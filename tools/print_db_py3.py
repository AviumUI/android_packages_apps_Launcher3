#!/usr/bin/env python3

import html
import codecs
import os
import pprint
import re
import shutil
import sys
import sqlite3

SCREENS = 0
COLUMNS = 4
ROWS = 4
HOTSEAT_SIZE = 4
CELL_SIZE = 110

CONTAINER_DESKTOP = -100
CONTAINER_HOTSEAT = -101

DIR = "db_files"
AUTO_FILE = DIR + "/launcher.db"
INDEX_FILE = DIR + "/index.html"

def usage():
  print("usage: print_db_py3.py launcher.db <4x4|5x5|5x6|...> -- prints a launcher.db with")
  print("       the specified grid size (rows x cols)")
  print("usage: print_db_py3.py <4x4|5x5|5x6|...> -- adb pulls a launcher.db from a device")
  print("       and prints it with the specified grid size (rows x cols)")
  print()
  print("The dump will be created in a directory called db_files in cwd.")
  print("This script will delete any db_files directory you have now")


def make_dir():
  shutil.rmtree(DIR, True)
  os.makedirs(DIR)

def adb_root_remount():
  os.system("adb root")
  os.system("adb remount")

def pull_file(fn):
  print("pull_file: " + fn)
  rv = os.system("adb pull"
    + " /data/data/com.android.launcher3/databases/launcher.db"
    + " " + fn);
  if rv != 0:
    print("adb pull failed")
    sys.exit(1)

def get_favorites(conn):
  c = conn.cursor()
  c.execute("SELECT * FROM favorites")
  columns = [d[0] for d in c.description]
  rows = []
  for row in c:
    rows.append(row)
  return columns,rows

def get_screens(conn):
  c = conn.cursor()
  c.execute("SELECT * FROM workspaceScreens")
  columns = [d[0] for d in c.description]
  rows = []
  for row in c:
    rows.append(row)
  return columns,rows

def print_intent(out, id, i, cell):
  if cell:
    out.write("""<span class="intent" title="%s">shortcut</span>""" % (
        html.escape(cell, True)
      ))


def print_icon(out, id, i, cell):
  if cell:
    icon_fn = "icon_%d.png" % id
    out.write("""<img style="width: 3em; height: 3em;" src="%s">""" % ( icon_fn ))
    f = open(DIR + "/" + icon_fn, "wb")
    f.write(cell)
    f.close()

def print_icon_type(out, id, i, cell):
  if cell == 0:
    out.write("Application (%d)" % cell)
  elif cell == 1:
    out.write("Shortcut (%d)" % cell)
  elif cell == 2:
    out.write("Folder (%d)" % cell)
  elif cell == 4:
    out.write("Widget (%d)" % cell)
  elif cell:
    out.write("%d" % cell)

def print_cell(out, id, i, cell):
  if not cell is None:
    out.write(html.escape(str(cell)))

FUNCTIONS = {
  "intent": print_intent,
  "icon": print_icon,
  "iconType": print_icon_type
}

def render_cell_info(out, cell, occupied):
  if cell is None:
    out.write("    <td width=%d height=%d></td>\n" %
        (CELL_SIZE, CELL_SIZE))
  elif cell == occupied:
    pass
  else:
    cellX = cell["cellX"]
    cellY = cell["cellY"]
    spanX = cell["spanX"]
    spanY = cell["spanY"]
    intent = cell.get("intent", "")
    if intent:
      title = "title=\"%s\"" % html.escape(intent, True)
    else:
      title = ""
    out.write(("    <td colspan=%d rowspan=%d width=%d height=%d"
        + " bgcolor=#dddddd align=center valign=middle %s>") % (
          spanX, spanY,
          (CELL_SIZE*spanX), (CELL_SIZE*spanY),
          title))
    itemType = cell["itemType"]
    if itemType == 0:
      out.write("""<img style="width: 4em; height: 4em;" src="icon_%d.png">\n""" % ( cell["_id"] ))
      out.write("<br/>\n")
      out.write(html.escape(str(cell.get("title", ""))) + " <br/><i>(app)</i>")
    elif itemType == 1:
      out.write("""<img style="width: 4em; height: 4em;" src="icon_%d.png">\n""" % ( cell["_id"] ))
      out.write("<br/>\n")
      out.write(html.escape(str(cell.get("title", ""))) + " <br/><i>(shortcut)</i>")
    elif itemType == 2:
      out.write("""<i>folder</i>""")
    elif itemType == 4:
      out.write("<i>widget %d</i><br/>\n" % cell.get("appWidgetId", 0))
    else:
      out.write("<b>unknown type: %d</b>" % itemType)
    out.write("</td>\n")

def render_screen_info(out, screen):
  out.write("<tr>")
  out.write("<td>%s</td>" % (screen["_id"]))
  out.write("<td>%s</td>" % (screen["screenRank"]))
  out.write("</tr>")

def process_file(fn):
  global SCREENS, COLUMNS, ROWS, HOTSEAT_SIZE
  print("process_file: " + fn)
  conn = sqlite3.connect(fn)
  columns,rows = get_favorites(conn)
  
  # Try to get screens, but handle if table doesn't exist
  try:
    screenCols, screenRows = get_screens(conn)
    screenData = [dict(zip(screenCols, screenRow)) for screenRow in screenRows]
  except:
    screenCols, screenRows = [], []
    screenData = []

  data = [dict(zip(columns,row)) for row in rows]

  # Calculate the proper number of screens, columns, and rows in this db
  screensIdMap = []
  hotseatIdMap = []
  HOTSEAT_SIZE = 0
  for d in data:
    if d.get("spanX") is None:
      d["spanX"] = 1
    if d.get("spanY") is None:
      d["spanY"] = 1
    if d.get("container") == CONTAINER_DESKTOP:
      if d.get("screen") not in screensIdMap:
        screensIdMap.append(d["screen"])
      COLUMNS = max(COLUMNS, d.get("cellX", 0) + d.get("spanX", 1))
      ROWS = max(ROWS, d.get("cellY", 0) + d.get("spanY", 1))
    elif d.get("container") == CONTAINER_HOTSEAT:
      hotseatIdMap.append(d.get("screen", 0))
      HOTSEAT_SIZE = max(HOTSEAT_SIZE, d.get("screen", 0) + 1)
  SCREENS = len(screensIdMap)

  out = codecs.open(INDEX_FILE, encoding="utf-8", mode="w")
  out.write("""<html>
<head>
<style type="text/css">
.intent {
  font-style: italic;
}
</style>
</head>
<body>
""")

  # Data table
  out.write("<b>Favorites table</b><br/>\n")
  out.write("""<html>
<table border=1 cellspacing=0 cellpadding=4>
<tr>
""")
  print_functions = []
  for col in columns:
    print_functions.append(FUNCTIONS.get(col, print_cell))
  for i in range(0,len(columns)):
    col = columns[i]
    out.write("""  <th>%s</th>
""" % ( col ))
  out.write("""
</tr>
""")

  for row in rows:
    out.write("""<tr>
""")
    for i in range(0,len(row)):
      cell = row[i]
      # row[0] is always _id
      out.write("""  <td>""")
      print_functions[i](out, row[0], row, cell)
      out.write("""</td>
""")
    out.write("""</tr>
""")
  out.write("""</table>
""")

  # Screens
  out.write("<br/><b>Screens</b><br/>\n")
  out.write("<table class=layout border=1 cellspacing=0 cellpadding=4>\n")
  out.write("<tr><td>Screen ID</td><td>Rank</td></tr>\n")
  for screen in screenData:
    render_screen_info(out, screen)
  out.write("</table>\n")

  # Hotseat
  hotseat = []
  for i in range(0, HOTSEAT_SIZE):
    hotseat.append(None)
  for row in data:
    if row.get("container") != CONTAINER_HOTSEAT:
      continue
    screen = row.get("screen", 0)
    hotseat[screen] = row
  out.write("<br/><b>Hotseat</b><br/>\n")
  out.write("<table class=layout border=1 cellspacing=0 cellpadding=4>\n")
  for cell in hotseat:
    render_cell_info(out, cell, None)
  out.write("</table>\n")

  # Pages
  screens = []
  for i in range(0,SCREENS):
    screen = []
    for j in range(0,ROWS):
      m = []
      for k in range(0,COLUMNS):
        m.append(None)
      screen.append(m)
    screens.append(screen)
  occupied = "occupied"
  for row in data:
    # desktop
    if row.get("container") != CONTAINER_DESKTOP:
      continue
    screen_idx = screensIdMap.index(row.get("screen", 0))
    screen = screens[screen_idx]
    cellX = row.get("cellX", 0)
    cellY = row.get("cellY", 0)
    spanX = row.get("spanX", 1)
    spanY = row.get("spanY", 1)
    for j in range(cellY, cellY+spanY):
      for k in range(cellX, cellX+spanX):
        if j < ROWS and k < COLUMNS:
          screen[j][k] = occupied
    screen[cellY][cellX] = row
  i=0
  for screen in screens:
    out.write("<br/><b>Screen %d</b><br/>\n" % i)
    out.write("<table class=layout border=1 cellspacing=0 cellpadding=4>\n")
    for m in screen:
      out.write("  <tr>\n")
      for cell in m:
        render_cell_info(out, cell, occupied)
      out.write("</tr>\n")
    out.write("</table>\n")
    i=i+1

  out.write("""
</body>
</html>
""")

  out.close()
  
  print("\n========== 桌面布局信息 ==========\n")
  print("Hotseat:")
  for i, item in enumerate(hotseat):
    if item:
      title = item.get("title", "Unknown")
      intent = item.get("intent", "")
      print("  [%d] %s" % (i, title))
  
  print("\n桌面屏幕:")
  screen_idx = 0
  for row in data:
    if row.get("container") == CONTAINER_DESKTOP:
      screen = row.get("screen", 0)
      cellX = row.get("cellX", 0)
      cellY = row.get("cellY", 0)
      spanX = row.get("spanX", 1)
      spanY = row.get("spanY", 1)
      itemType = row.get("itemType", 0)
      title = row.get("title", "")
      packageName = row.get("packageName", "")
      className = row.get("className", "")
      appWidgetId = row.get("appWidgetId", "")
      
      type_str = {0: "app", 1: "shortcut", 2: "folder", 4: "widget"}.get(itemType, "unknown")
      
      print("  Screen %d, Pos (%d,%d), Span (%dx%d), Type: %s" % (screen, cellX, cellY, spanX, spanY, type_str))
      if title:
        print("    Title: %s" % title)
      if packageName:
        print("    Package: %s" % packageName)
      if className:
        print("    Class: %s" % className)
      if appWidgetId:
        print("    WidgetId: %s" % appWidgetId)
      print()

def updateDeviceClassConstants(str):
  global SCREENS, COLUMNS, ROWS, HOTSEAT_SIZE
  match = re.search(r"(\d+)x(\d+)", str)
  if match:
    COLUMNS = int(match.group(1))
    ROWS = int(match.group(2))
    HOTSEAT_SIZE = 2 * int(COLUMNS / 2)
    return True
  return False

def main(argv):
  if len(argv) == 1 or (len(argv) == 2 and updateDeviceClassConstants(argv[1])):
    make_dir()
    adb_root_remount()
    pull_file(AUTO_FILE)
    process_file(AUTO_FILE)
  elif len(argv) == 2 or (len(argv) == 3 and updateDeviceClassConstants(argv[2])):
    make_dir()
    process_file(argv[1])
  else:
    usage()

if __name__=="__main__":
  main(sys.argv)
