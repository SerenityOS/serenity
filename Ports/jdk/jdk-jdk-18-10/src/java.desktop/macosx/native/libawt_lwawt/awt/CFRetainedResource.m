/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
#import "ThreadUtilities.h"

#import <Cocoa/Cocoa.h>

#import "sun_lwawt_macosx_CFRetainedResource.h"


/*
 * Class:     sun_lwawt_macosx_CFRetainedResource
 * Method:    nativeCFRelease
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CFRetainedResource_nativeCFRelease
(JNIEnv *env, jclass clazz, jlong ptr, jboolean releaseOnAppKitThread)
{
    if (releaseOnAppKitThread) {
        // Releasing resources on the main AppKit message loop only
        // Releasing resources on the nested loops may cause dangling
        // pointers after the nested loop is exited
        if ([NSApp respondsToSelector:@selector(postRunnableEvent:)]) {
            [NSApp postRunnableEvent:^() {
                CFRelease(jlong_to_ptr(ptr));
            }];
        } else {
            // could happen if we are embedded inside SWT/FX application,
            [ThreadUtilities performOnMainThreadWaiting:NO block:^() {
                CFRelease(jlong_to_ptr(ptr));
            }];
        }
    } else {

JNI_COCOA_ENTER(env);

        CFRelease(jlong_to_ptr(ptr));

JNI_COCOA_EXIT(env);

    }
}
