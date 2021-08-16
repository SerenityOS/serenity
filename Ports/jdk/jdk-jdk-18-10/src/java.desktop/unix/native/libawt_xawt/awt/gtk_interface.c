/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <dlfcn.h>
#include <stdlib.h>
#include "jvm_md.h"
#include "gtk_interface.h"

GtkApi* gtk2_load(JNIEnv *env, const char* lib_name);
GtkApi* gtk3_load(JNIEnv *env, const char* lib_name);

gboolean gtk2_check(const char* lib_name, gboolean load);
gboolean gtk3_check(const char* lib_name, gboolean load);

GtkApi *gtk;

typedef struct {
    GtkVersion version;
    const char* name;
    const char* vname;
    GtkApi* (*load)(JNIEnv *env, const char* lib_name);
    gboolean (*check)(const char* lib_name, gboolean load);
} GtkLib;

static GtkLib gtk_libs[] = {
    {
        GTK_3,
        JNI_LIB_NAME("gtk-3"),
        VERSIONED_JNI_LIB_NAME("gtk-3", "0"),
        &gtk3_load,
        &gtk3_check
    },
    {
        GTK_2,
        JNI_LIB_NAME("gtk-x11-2.0"),
        VERSIONED_JNI_LIB_NAME("gtk-x11-2.0", "0"),
        &gtk2_load,
        &gtk2_check
    }
};

static GtkLib** get_libs_order(GtkVersion version) {
    static GtkLib** load_order;
    static int n_libs = 0;
    if (!n_libs) {
        n_libs = sizeof(gtk_libs) / sizeof(GtkLib);
        load_order = calloc(n_libs + 1, sizeof(GtkLib *));
        if (load_order == NULL) {
          return NULL;
        }
    }
    int i, first = 0;
    for (i = 0; i < n_libs; i++) {
        load_order[i] = &gtk_libs[i];
        if (load_order[i]->version == version) {
            first = i;
        }
    }
    if (first) {
        for (i = first; i > 0; i--) {
            load_order[i] = load_order[i - 1];
        }
        load_order[0] = &gtk_libs[first];
    }
    return load_order;
}

static GtkLib* get_loaded() {
    GtkLib** libs = get_libs_order(GTK_ANY);
    if (libs == NULL) return NULL;
    while(!gtk && *libs) {
        GtkLib* lib = *libs++;
        if (lib->check(lib->vname, /* load = */FALSE)) {
            return lib;
        }
        if (lib->check(lib->name, /* load = */FALSE)) {
            return lib;
        }
    }
    return NULL;
}

gboolean gtk_load(JNIEnv *env, GtkVersion version, gboolean verbose) {
    if (gtk == NULL) {
        GtkLib* lib = get_loaded();
        if (lib) {
            if (verbose) {
                fprintf(stderr, "Looking for GTK%d library...\n",
                                                                 lib->version);
            }
            gtk = lib->load(env, lib->vname);
            if (!gtk) {
                gtk = lib->load(env, lib->name);
            }
        } else {
            GtkLib** libs = get_libs_order(version);
            while (!gtk && libs && *libs) {
                lib = *libs++;
                if (version == GTK_ANY || lib->version == version) {
                    if (verbose) {
                        fprintf(stderr, "Looking for GTK%d library...\n",
                                                                  lib->version);
                    }
                    gtk = lib->load(env, lib->vname);
                    if (!gtk) {
                        gtk = lib->load(env, lib->name);
                    }
                    if (verbose && !gtk) {
                        fprintf(stderr, "Not found.\n");
                    }
                }
            }
        }
        if (verbose) {
            if (gtk) {
                fprintf(stderr, "GTK%d library loaded.\n", lib->version);
            } else {
                fprintf(stderr, "Failed to load GTK library.\n");
            }
        }
    }
    return gtk != NULL;
}

static gboolean check_version(GtkVersion version) {
    GtkLib** libs = get_libs_order(version);
    if (libs == NULL) return FALSE;
    while (*libs) {
        GtkLib* lib = *libs++;
        if (lib->check(lib->vname, /* load = */TRUE)) {
            return TRUE;
        }
        if (lib->check(lib->name, /* load = */TRUE)) {
            return TRUE;
        }
    }
    return FALSE;
}

gboolean gtk_check_version(GtkVersion version) {
    if (gtk || get_loaded()) {
        return TRUE;
    }
    return check_version(version);
}

