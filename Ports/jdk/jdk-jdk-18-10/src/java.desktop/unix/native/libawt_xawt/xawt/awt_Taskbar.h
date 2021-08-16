/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

#ifndef AWT_TASKBAR_H
#define AWT_TASKBAR_H

#include "gtk_interface.h"

typedef void UnityLauncherEntry;
typedef void DbusmenuMenuitem;

static UnityLauncherEntry* (*fp_unity_launcher_entry_get_for_desktop_file) (const gchar* desktop_file);

static void (*fp_unity_launcher_entry_set_count) (UnityLauncherEntry* self, gint64 value);
static void (*fp_unity_launcher_entry_set_count_visible) (UnityLauncherEntry* self, gboolean value);

static void (*fp_unity_launcher_entry_set_urgent) (UnityLauncherEntry* self, gboolean value);

static void (*fp_unity_launcher_entry_set_progress) (UnityLauncherEntry* self, gdouble value);
static void (*fp_unity_launcher_entry_set_progress_visible) (UnityLauncherEntry* self, gboolean value);


static DbusmenuMenuitem* (*fp_dbusmenu_menuitem_new) (void);
static gboolean (*fp_dbusmenu_menuitem_property_set) (DbusmenuMenuitem* mi, const gchar* property, const gchar* value);
static gboolean (*fp_dbusmenu_menuitem_property_set_int) (DbusmenuMenuitem * mi, const gchar * property, const gint value);
static gint (*fp_dbusmenu_menuitem_property_get_int) (const DbusmenuMenuitem * mi, const gchar * property);
static gboolean (*fp_dbusmenu_menuitem_child_append) (DbusmenuMenuitem* mi, DbusmenuMenuitem* child);
static gboolean (*fp_dbusmenu_menuitem_child_delete) (DbusmenuMenuitem * mi, DbusmenuMenuitem * child);
static GList * (*fp_dbusmenu_menuitem_take_children) (DbusmenuMenuitem * mi);
static void (*fp_dbusmenu_menuitem_foreach) (DbusmenuMenuitem * mi, void (*func) (DbusmenuMenuitem * mi, gpointer data), gpointer data);
static void (*fp_unity_launcher_entry_set_quicklist) (UnityLauncherEntry* self, DbusmenuMenuitem* value);
static DbusmenuMenuitem* (*fp_unity_launcher_entry_get_quicklist) (UnityLauncherEntry* self);


#endif /* AWT_TASKBAR_H */

