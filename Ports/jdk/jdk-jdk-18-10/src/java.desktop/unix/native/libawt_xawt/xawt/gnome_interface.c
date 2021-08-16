/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "gnome_interface.h"

GNOME_URL_SHOW_TYPE *gnome_url_show = NULL;

gboolean gnome_load() {
     void *vfs_handle;
     void *gnome_handle;
     const char *errmsg;
     GNOME_VFS_INIT_TYPE *gnome_vfs_init;

     // trying to open the gnomevfs. VERSIONED_JNI_LIB_NAME
     // macros formats the library name in a system specific manner
     // see jdk/src/solaris/javavm/export/jvm_md.h for more details
     vfs_handle = dlopen(VERSIONED_JNI_LIB_NAME("gnomevfs-2", "0"), RTLD_LAZY);
     if (vfs_handle == NULL) {
         // if we cannot load the library using a version assumed by JNI
         // we are trying to load the library without a version suffix
         vfs_handle = dlopen(JNI_LIB_NAME("gnomevfs-2"), RTLD_LAZY);
         if (vfs_handle == NULL) {
 #ifdef DEBUG
             fprintf(stderr, "can not load libgnomevfs-2.so\n");
 #endif
             return FALSE;
         }
     }
     dlerror(); /* Clear errors */
     gnome_vfs_init = (GNOME_VFS_INIT_TYPE*)dlsym(vfs_handle, "gnome_vfs_init");
     if (gnome_vfs_init == NULL){
 #ifdef DEBUG
         fprintf(stderr, "dlsym( gnome_vfs_init) returned NULL\n");
 #endif
         return FALSE;
     }
     if ((errmsg = dlerror()) != NULL) {
 #ifdef DEBUG
         fprintf(stderr, "can not find symbol gnome_vfs_init %s \n", errmsg);
 #endif
         return FALSE;
     }
     // call gonme_vfs_init()
     (*gnome_vfs_init)();

     gnome_handle = dlopen(VERSIONED_JNI_LIB_NAME("gnome-2", "0"), RTLD_LAZY);
     if (gnome_handle == NULL) {
         gnome_handle = dlopen(JNI_LIB_NAME("gnome-2"), RTLD_LAZY);
         if (gnome_handle == NULL) {
 #ifdef DEBUG
             fprintf(stderr, "can not load libgnome-2.so\n");
 #endif
             return FALSE;
         }
     }
     dlerror(); /* Clear errors */
     gnome_url_show = (GNOME_URL_SHOW_TYPE*)dlsym(gnome_handle, "gnome_url_show");
     if ((errmsg = dlerror()) != NULL) {
 #ifdef DEBUG
         fprintf(stderr, "can not find symble gnome_url_show\n");
 #endif
         return FALSE;
     }
     return TRUE;
}
