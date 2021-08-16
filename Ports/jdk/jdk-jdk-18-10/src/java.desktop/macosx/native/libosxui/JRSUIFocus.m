/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

#import "JNIUtilities.h"

#import "apple_laf_JRSUIFocus.h"
#import "apple_laf_JRSUIControl.h"

#include <Carbon/Carbon.h>


/*
 * Class:     apple_laf_JRSUIFocus
 * Method:    beginNativeFocus
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_apple_laf_JRSUIFocus_beginNativeFocus
(JNIEnv *env, jclass clazz, jlong cgContext, jint ringStyle)
{
    if (cgContext == 0L) return apple_laf_JRSUIFocus_NULL_CG_REF;
    CGContextRef cgRef = (CGContextRef)jlong_to_ptr(cgContext);

    OSStatus status = HIThemeBeginFocus(cgRef, ringStyle, NULL);
    return status == noErr ? apple_laf_JRSUIFocus_SUCCESS : status;
}

/*
 * Class:     apple_laf_JRSUIFocus
 * Method:    endNativeFocus
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_apple_laf_JRSUIFocus_endNativeFocus
(JNIEnv *env, jclass clazz, jlong cgContext)
{
    if (cgContext == 0L) return apple_laf_JRSUIFocus_NULL_CG_REF;
    CGContextRef cgRef = (CGContextRef)jlong_to_ptr(cgContext);

    OSStatus status = HIThemeEndFocus(cgRef);
    return status == noErr ? apple_laf_JRSUIFocus_SUCCESS : status;
}
