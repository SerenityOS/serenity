/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

#include <CoreFoundation/CoreFoundation.h>
#include <CoreServices/CoreServices.h>

/**
 * Creates a CF string from the given Java string.
 * If javaString is NULL, NULL is returned.
 * If a memory error occurs, and OutOfMemoryError is thrown and
 * NULL is returned.
 */
static CFStringRef toCFString(JNIEnv *env, jstring javaString)
{
    if (javaString == NULL) {
        return NULL;
    } else {
        CFStringRef result = NULL;
        jsize length = (*env)->GetStringLength(env, javaString);
        const jchar *chars = (*env)->GetStringChars(env, javaString, NULL);
        if (chars == NULL) {
            JNU_ThrowOutOfMemoryError(env, "toCFString failed");
            return NULL;
        }
        result = CFStringCreateWithCharacters(NULL, (const UniChar *)chars,
                                              length);
        (*env)->ReleaseStringChars(env, javaString, chars);
        if (result == NULL) {
            JNU_ThrowOutOfMemoryError(env, "toCFString failed");
            return NULL;
        }
        return result;
    }
}

/**
 * Creates a Java string from the given CF string.
 * If cfString is NULL, NULL is returned.
 * If a memory error occurs, and OutOfMemoryError is thrown and
 * NULL is returned.
 */
static jstring toJavaString(JNIEnv *env, CFStringRef cfString)
{
    if (cfString == NULL) {
        return NULL;
    } else {
        jstring javaString = NULL;

        CFIndex length = CFStringGetLength(cfString);
        const UniChar *constchars = CFStringGetCharactersPtr(cfString);
        if (constchars) {
            javaString = (*env)->NewString(env, constchars, length);
        } else {
            UniChar *chars = malloc(length * sizeof(UniChar));
            if (chars == NULL) {
                JNU_ThrowOutOfMemoryError(env, "toJavaString failed");
                return NULL;
            }
            CFStringGetCharacters(cfString, CFRangeMake(0, length), chars);
            javaString = (*env)->NewString(env, chars, length);
            free(chars);
        }
        return javaString;
    }
}

/**
 * Returns the content type corresponding to the supplied file extension.
 * The mapping is determined using Uniform Type Identifiers (UTIs).  If
 * the file extension parameter is NULL, a CFString cannot be created
 * from the file extension parameter, there is no UTI corresponding to
 * the file extension, the UTI cannot supply a MIME type for the file
 * extension, or a Java string cannot be created, then NULL is returned;
 * otherwise the MIME type string is returned.
 */
JNIEXPORT jstring JNICALL
Java_sun_nio_fs_UTIFileTypeDetector_probe0(JNIEnv* env, jobject ftd,
                                           jstring ext)
{
    jstring result = NULL;

    CFStringRef extension = toCFString(env, ext);
    if (extension != NULL) {
        CFStringRef uti =
            UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                  extension, NULL);
        CFRelease(extension);

        if (uti != NULL) {
            CFStringRef mimeType =
                UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType);
            CFRelease(uti);

            if (mimeType != NULL) {
                result = toJavaString(env, mimeType);
                CFRelease(mimeType);
            }
        }
    }

    return result;
}
