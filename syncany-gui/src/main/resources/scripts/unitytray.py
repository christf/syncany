#!/usr/bin/python
#
# Syncany Linux Native Functions
# Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
# author : Philipp C. Heckel <philipp.heckel@gmail.com> 
# author : Vincent Wiencek <vwiencek@gmail.com>

# Dependencies:
# - python-appindicator
# - python-websocket-client

import os
import sys
import time
import gtk
import pynotify
import socket
import threading
import json
import Queue
import subprocess
import websocket
import appindicator
import urllib
import tempfile

def fetch_image(relativeUrl):
	global baseUrl, imagesMap
	
	#caching images in dictionary
	if not relativeUrl in imagesMap:
		tf = tempfile.NamedTemporaryFile(delete=True)
		fname,x = urllib.urlretrieve(baseUrl + relativeUrl, tf.name +".png")
		do_print("Caching image '" + relativeUrl + "' to '" + fname + "'")
		imagesMap[relativeUrl] = fname
	
	return imagesMap[relativeUrl]

def do_notify(request):
	do_print("Creating notification ...")

	if request["image"] == "":
		image = fetch_image("/logo48.png")
	else:
		image = request["image"]
	
	# Alterantive using 'notify-send'
	# os.system("notify-send -t 2000 -i '{0}' '{1}' '{2}'".format(image, request["summary"], request["body"]))

	gtk.gdk.threads_enter()
	pynotify.init("Syncany")
	notification = pynotify.Notification(request["summary"], request["body"], image)
	notification.show()
	gtk.gdk.threads_leave()

	return None		
	
def do_update_icon(request):
	global indicator
	indicator.set_icon(fetch_image(request["imageFileName"]))		
	return None
	
def do_update_text(request):
	global menu_item_status
	
	gtk.gdk.threads_enter()
	
	label = menu_item_status.get_child()
	label.set_text(request["text"])
	
	menu_item_status.show()
	
	gtk.gdk.threads_leave()
	
	return None			
	
def do_update_menu(request):
	global menu, menu_item_status
	global status_text

	gtk.gdk.threads_enter()

	# Remove all children
	for child in menu.get_children():
		menu.remove(child)		
	
	'''Status'''
	menu_item_status.child.set_text(status_text)
	menu_item_status.set_can_default(0);	
	menu_item_status.set_sensitive(0);

	menu.append(menu_item_status)

	'''---'''
	menu.append(gtk.SeparatorMenuItem())	

	'''New connection'''
	menu_item_new = gtk.MenuItem("New sync folder")	
	menu_item_new.connect("activate", menu_item_clicked, "tray_menu_clicked_new")

	menu.append(menu_item_new)
	
	'''Preferences'''
	menu_item_prefs = gtk.MenuItem("Preferences")
	menu_item_prefs.connect("activate", menu_item_clicked, "tray_menu_clicked_preferences")
	
	menu.append(menu_item_prefs)
	
	'''---'''
	menu.append(gtk.SeparatorMenuItem())	

	'''Folders'''
	if request is not None:
		folders = request["folders"]
		
		for folder in folders.itervalues():
			menu_item_folder = gtk.MenuItem(os.path.basename(folder["folder"]) + "(" + folder["status"] + ")")
			menu_item_folder.connect("activate", menu_item_folder_clicked, folder["folder"])
		
			menu.append(menu_item_folder)
		
		if len(folders) > 0:
			'''---'''
			menu.append(gtk.SeparatorMenuItem())	
	
	'''Donate ...'''
	menu_item_donate = gtk.MenuItem("Donate")
	menu_item_donate.connect("activate", menu_item_clicked, "tray_menu_clicked_donate")
	
	menu.append(menu_item_donate)	
	
	'''Website'''
	menu_item_website = gtk.MenuItem("Website")
	menu_item_website.connect("activate", menu_item_clicked, "tray_menu_clicked_website")
	
	menu.append(menu_item_website)	
	
	'''---'''
	menu.append(gtk.SeparatorMenuItem())	

	'''Quit'''
	menu_item_quit = gtk.MenuItem("Exit")
	menu_item_quit.connect("activate", menu_item_clicked, "tray_menu_clicked_quit")
		
	menu.append(menu_item_quit)	
	
	'''Set as menu for indicator'''
	indicator.set_menu(menu)

	'''Show'''
	menu.show_all()
	gtk.gdk.threads_leave()
	
	return None

def init_menu():
	do_update_menu(None)

def init_tray_icon():
	global indicator

	# Default image
	image = fetch_image("/images/tray/tray.png")									

	# Go!
	do_print("Initializing indicator ...")
	
	indicator = appindicator.Indicator("syncany", image, appindicator.CATEGORY_APPLICATION_STATUS)
	indicator.set_status(appindicator.STATUS_ACTIVE)
	indicator.set_attention_icon("indicator-messages-new")	
	
def menu_item_clicked(widget, action):
	do_print("Menu item '" + action + "' clicked.")
	ws.send("{'action': '" + action + "'}")
	
	if action == "tray_menu_clicked_quit":
		time.sleep(2)
		sys.exit(0)

def menu_item_folder_clicked(widget, folder):
	do_print("Folder item '" + folder + "' clicked.")
	ws.send("{'action': 'tray_menu_clicked_folder', 'folder': '" + folder + "'}")

def do_kill():
	# Note: this method cannot contain any do_print() calls since it is called
	#       by do_print if the STDOUT socket breaks!
	
	pid = os.getpid()
	os.system("kill -9 {0}".format(pid))
	
def do_print(msg):
	try:
		sys.stdout.write("{0}\n".format(msg))
		sys.stdout.flush()
	except:
		# An IOError happens when the calling process is killed unexpectedly		
		do_kill()			

def on_ws_message(ws, message):	
	try:
		do_print("Received request: " + message)				

		request = json.loads(message)
		response = None
		
		last_request = time.time()
		
		if request["action"] == "display_notification":
			response = do_notify(request)
		
		elif request["action"] == "update_tray_menu":
			response = do_update_menu(request)
		
		elif request["action"] == "update_tray_icon":
			response = do_update_icon(request)
		
		elif request["action"] == "update_tray_status_text":
			response = do_update_text(request)			
		
	except:	
		do_print("Unexpected error: {0}".format(sys.exc_info()[0]))

	if response is not None:
		ws.send(response)

def on_ws_error(ws, error):
	print "WS error"
	print error

def on_ws_close(ws):
	print "WS closed"
	do_kill()

def on_ws_open(ws):
	print "WS open"

def ws_start_client():
	global ws, wsUrl

	ws = websocket.WebSocketApp(wsUrl,
		on_message = on_ws_message,
		on_error = on_ws_error,
		on_close = on_ws_close,
		header = [ "client_id: appindicator-tray" ])

	ws.on_open = on_ws_open

	ws.run_forever()					

def main():
	'''Init application and menu'''
	init_tray_icon()
	init_menu()	
	
	ws_server_thread = threading.Thread(target=ws_start_client)
	ws_server_thread.setDaemon(True)
	ws_server_thread.start()

	gtk.gdk.threads_init()
	gtk.gdk.threads_enter()
	gtk.main()
		
	#gtk.gdk.threads_leave()

if __name__ == "__main__":
	# Global variables
	imagesMap = dict()
	status_text = "Synced"
	
	updating_count = 0
	indicator = None
	ws = None
	ws_server_thread = None
	terminated = 0
		
	# Default values
	menu = gtk.Menu()
	menu_item_status = gtk.MenuItem(status_text)

	# Go!
	main()
