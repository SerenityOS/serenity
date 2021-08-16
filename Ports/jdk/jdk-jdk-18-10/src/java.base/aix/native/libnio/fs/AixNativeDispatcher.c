/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2013, 2019 SAP SE. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/mntctl.h>

#include "jni.h"
#include "jni_util.h"

#include "sun_nio_fs_AixNativeDispatcher.h"

static jfieldID entry_name;
static jfieldID entry_dir;
static jfieldID entry_fstype;
static jfieldID entry_options;

static jclass entry_cls;

/**
 * Call this to throw an internal UnixException when a system/library
 * call fails
 */
static void throwUnixException(JNIEnv* env, int errnum) {
    jobject x = JNU_NewObjectByName(env, "sun/nio/fs/UnixException",
        "(I)V", errnum);
    if (x != NULL) {
        (*env)->Throw(env, x);
    }
}

/**
 * Initialization
 */
JNIEXPORT void JNICALL
Java_sun_nio_fs_AixNativeDispatcher_init(JNIEnv* env, jclass this)
{
    jclass clazz;

    clazz = (*env)->FindClass(env, "sun/nio/fs/UnixMountEntry");
    CHECK_NULL(clazz);
    entry_name = (*env)->GetFieldID(env, clazz, "name", "[B");
    CHECK_NULL(entry_name);
    entry_dir = (*env)->GetFieldID(env, clazz, "dir", "[B");
    CHECK_NULL(entry_dir);
    entry_fstype = (*env)->GetFieldID(env, clazz, "fstype", "[B");
    CHECK_NULL(entry_fstype);
    entry_options = (*env)->GetFieldID(env, clazz, "opts", "[B");
    CHECK_NULL(entry_options);
    entry_cls = (*env)->NewGlobalRef(env, clazz);
    if (entry_cls == NULL) {
        JNU_ThrowOutOfMemoryError(env, NULL);
        return;
    }
}

/**
 * Special implementation of getextmntent (see SolarisNativeDispatcher.c)
 * that returns all entries at once.
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_nio_fs_AixNativeDispatcher_getmntctl(JNIEnv* env, jclass this)
{
    int must_free_buf = 0;
    char stack_buf[1024];
    char* buffer = stack_buf;
    size_t buffer_size = 1024;
    int num_entries;
    int i;
    jobjectArray ret;
    struct vmount * vm;

    for (i = 0; i < 5; i++) {
        num_entries = mntctl(MCTL_QUERY, buffer_size, buffer);
        if (num_entries != 0) {
            break;
        }
        if (must_free_buf) {
            free(buffer);
        }
        buffer_size *= 8;
        buffer = malloc(buffer_size);
        must_free_buf = 1;
    }
    /* Treat zero entries like errors. */
    if (num_entries <= 0) {
        if (must_free_buf) {
            free(buffer);
        }
        throwUnixException(env, errno);
        return NULL;
    }
    ret = (*env)->NewObjectArray(env, num_entries, entry_cls, NULL);
    if (ret == NULL) {
        if (must_free_buf) {
            free(buffer);
        }
        return NULL;
    }
    vm = (struct vmount*)buffer;
    for (i = 0; i < num_entries; i++) {
        jsize len;
        jbyteArray bytes;
        const char* fstype;
        /* We set all relevant attributes so there is no need to call constructor. */
        jobject entry = (*env)->AllocObject(env, entry_cls);
        if (entry == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetObjectArrayElement(env, ret, i, entry);

        /* vm->vmt_data[...].vmt_size is 32 bit aligned and also includes NULL byte. */
        /* Since we only need the characters, it is necessary to check string size manually. */
        len = strlen((char*)vm + vm->vmt_data[VMT_OBJECT].vmt_off);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)((char *)vm + vm->vmt_data[VMT_OBJECT].vmt_off));
        (*env)->SetObjectField(env, entry, entry_name, bytes);

        len = strlen((char*)vm + vm->vmt_data[VMT_STUB].vmt_off);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)((char *)vm + vm->vmt_data[VMT_STUB].vmt_off));
        (*env)->SetObjectField(env, entry, entry_dir, bytes);

        switch (vm->vmt_gfstype) {
            case MNT_J2:
                fstype = "jfs2";
                break;
            case MNT_NAMEFS:
                fstype = "namefs";
                break;
            case MNT_NFS:
                fstype = "nfs";
                break;
            case MNT_JFS:
                fstype = "jfs";
                break;
            case MNT_CDROM:
                fstype = "cdrom";
                break;
            case MNT_PROCFS:
                fstype = "procfs";
                break;
            case MNT_NFS3:
                fstype = "nfs3";
                break;
            case MNT_AUTOFS:
                fstype = "autofs";
                break;
            case MNT_UDF:
                fstype = "udfs";
                break;
            case MNT_NFS4:
                fstype = "nfs4";
                break;
            case MNT_CIFS:
                fstype = "smbfs";
                break;
            default:
                fstype = "unknown";
        }
        len = strlen(fstype);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)fstype);
        (*env)->SetObjectField(env, entry, entry_fstype, bytes);

        len = strlen((char*)vm + vm->vmt_data[VMT_ARGS].vmt_off);
        bytes = (*env)->NewByteArray(env, len);
        if (bytes == NULL) {
            if (must_free_buf) {
                free(buffer);
            }
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)((char *)vm + vm->vmt_data[VMT_ARGS].vmt_off));
        (*env)->SetObjectField(env, entry, entry_options, bytes);

        /* goto the next vmount structure: */
        vm = (struct vmount *)((char *)vm + vm->vmt_length);
    }

    if (must_free_buf) {
        free(buffer);
    }
    return ret;
}
