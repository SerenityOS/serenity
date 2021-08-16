/*
 * Copyright (c) 1995, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "java_awt_Font.h"
#include "sun_awt_FontDescriptor.h"
#include "sun_awt_PlatformFont.h"


/*
 * Class:     java_awt_Font
 * Method:    initIDs
 * Signature: ()V
 */

/* This function gets called from the static initializer for Font.java
   to initialize the fieldIDs for fields that may be accessed from C */

JNIEXPORT void JNICALL
Java_java_awt_Font_initIDs(JNIEnv *env, jclass cls) {
}

/*
 * Class:     sun_awt_FontDescriptor
 * Method:    initIDs
 * Signature: ()V
 */

/* This function gets called from the static initializer for
   FontDescriptor.java to initialize the fieldIDs for fields
   that may be accessed from C */

JNIEXPORT void JNICALL
Java_sun_awt_FontDescriptor_initIDs(JNIEnv *env, jclass cls) {
}

/*
 * Class:     sun_awt_PlatformFont
 * Method:    initIDs
 * Signature: ()V
 */

/* This function gets called from the static initializer for
   PlatformFont.java to initialize the fieldIDs for fields
   that may be accessed from C */

JNIEXPORT void JNICALL
Java_sun_awt_PlatformFont_initIDs(JNIEnv *env, jclass cls) {
}
