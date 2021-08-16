/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "TimeZone_md.h"

#include "java_util_TimeZone.h"

/*
 * Gets the platform defined TimeZone ID
 */
JNIEXPORT jstring JNICALL
Java_java_util_TimeZone_getSystemTimeZoneID(JNIEnv *env, jclass ign,
                                            jstring java_home)
{
    const char *java_home_dir;
    char *javaTZ;
    jstring jstrJavaTZ = NULL;

    CHECK_NULL_RETURN(java_home, NULL);

    java_home_dir = JNU_GetStringPlatformChars(env, java_home, 0);
    CHECK_NULL_RETURN(java_home_dir, NULL);

    /*
     * Invoke platform dependent mapping function
     */
    javaTZ = findJavaTZ_md(java_home_dir);
    if (javaTZ != NULL) {
        jstrJavaTZ = JNU_NewStringPlatform(env, javaTZ);
        free((void *)javaTZ);
    }

    JNU_ReleaseStringPlatformChars(env, java_home, java_home_dir);
    return jstrJavaTZ;
}

/*
 * Gets a GMT offset-based time zone ID (e.g., "GMT-08:00")
 */
JNIEXPORT jstring JNICALL
Java_java_util_TimeZone_getSystemGMTOffsetID(JNIEnv *env, jclass ign)
{
    char *id = getGMTOffsetID();
    jstring jstrID = NULL;

    if (id != NULL) {
        jstrID = JNU_NewStringPlatform(env, id);
        free((void *)id);
    }
    return jstrID;
}
