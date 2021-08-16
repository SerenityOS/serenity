/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"

#include <sys/param.h>
#include <sys/mount.h>
#ifdef ST_RDONLY
#define statfs statvfs
#define getfsstat getvfsstat
#define f_flags f_flag
#define ISREADONLY ST_RDONLY
#else
#define ISREADONLY MNT_RDONLY
#endif

#include <stdlib.h>
#include <string.h>

static jfieldID entry_name;
static jfieldID entry_dir;
static jfieldID entry_fstype;
static jfieldID entry_options;

struct fsstat_iter {
    struct statfs *buf;
    int pos;
    int nentries;
};

#include "sun_nio_fs_BsdNativeDispatcher.h"

static void throwUnixException(JNIEnv* env, int errnum) {
    jobject x = JNU_NewObjectByName(env, "sun/nio/fs/UnixException",
        "(I)V", errnum);
    if (x != NULL) {
        (*env)->Throw(env, x);
    }
}

/**
 * Initialize jfieldIDs
 */
JNIEXPORT void JNICALL
Java_sun_nio_fs_BsdNativeDispatcher_initIDs(JNIEnv* env, jclass this)
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
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_BsdNativeDispatcher_getfsstat(JNIEnv* env, jclass this)
{
    int nentries;
    size_t bufsize;
    struct fsstat_iter *iter = malloc(sizeof(*iter));

    if (iter == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native heap");
        return 0;
    }

    iter->pos = 0;
    iter->nentries = 0;
    iter->buf = NULL;

    nentries = getfsstat(NULL, 0, MNT_NOWAIT);

    if (nentries <= 0) {
        free(iter);
        throwUnixException(env, errno);
        return 0;
    }

    // It's possible that a new filesystem gets mounted between
    // the first getfsstat and the second so loop until consistant

    while (nentries != iter->nentries) {
        if (iter->buf != NULL)
            free(iter->buf);

        bufsize = nentries * sizeof(struct statfs);
        iter->nentries = nentries;

        iter->buf = malloc(bufsize);
        if (iter->buf == NULL) {
            free(iter);
            JNU_ThrowOutOfMemoryError(env, "native heap");
            return 0;
        }

        nentries = getfsstat(iter->buf, bufsize, MNT_WAIT);
        if (nentries <= 0) {
            free(iter->buf);
            free(iter);
            throwUnixException(env, errno);
            return 0;
        }
    }

    return (jlong)iter;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_BsdNativeDispatcher_fsstatEntry(JNIEnv* env, jclass this,
    jlong value, jobject entry)
{
    struct fsstat_iter *iter = jlong_to_ptr(value);
    jsize len;
    jbyteArray bytes;
    char* name;
    char* dir;
    char* fstype;
    char* options;
    dev_t dev;

    if (iter == NULL || iter->pos >= iter->nentries)
       return -1;

    name = iter->buf[iter->pos].f_mntfromname;
    dir = iter->buf[iter->pos].f_mntonname;
    fstype = iter->buf[iter->pos].f_fstypename;
    if (iter->buf[iter->pos].f_flags & ISREADONLY)
        options="ro";
    else
        options="";

    iter->pos++;

    len = strlen(name);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)name);
    (*env)->SetObjectField(env, entry, entry_name, bytes);

    len = strlen(dir);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)dir);
    (*env)->SetObjectField(env, entry, entry_dir, bytes);

    len = strlen(fstype);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)fstype);
    (*env)->SetObjectField(env, entry, entry_fstype, bytes);

    len = strlen(options);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes == NULL)
        return -1;
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte*)options);
    (*env)->SetObjectField(env, entry, entry_options, bytes);

    return 0;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_BsdNativeDispatcher_endfsstat(JNIEnv* env, jclass this, jlong value)
{
    struct fsstat_iter *iter = jlong_to_ptr(value);

    if (iter != NULL) {
        free(iter->buf);
        free(iter);
    }
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_fs_BsdNativeDispatcher_getmntonname0(JNIEnv *env, jclass this,
    jlong pathAddress)
{
    struct statfs buf;
    const char* path = (const char*)jlong_to_ptr(pathAddress);

    if (statfs(path, &buf) != 0) {
        throwUnixException(env, errno);
    }

    jsize len = strlen(buf.f_mntonname);
    jbyteArray mntonname = (*env)->NewByteArray(env, len);
    if (mntonname != NULL) {
        (*env)->SetByteArrayRegion(env, mntonname, 0, len,
            (jbyte*)buf.f_mntonname);
    }

    return mntonname;
}
