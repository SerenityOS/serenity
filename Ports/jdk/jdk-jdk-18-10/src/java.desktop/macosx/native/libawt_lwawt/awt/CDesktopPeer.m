/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
#import <CoreFoundation/CoreFoundation.h>
#import <ApplicationServices/ApplicationServices.h>

/*
 * Class:     sun_lwawt_macosx_CDesktopPeer
 * Method:    _lsOpenURI
 * Signature: (Ljava/lang/String;)I;
 */
JNIEXPORT jint JNICALL Java_sun_lwawt_macosx_CDesktopPeer__1lsOpenURI
(JNIEnv *env, jclass clz, jstring uri)
{
    OSStatus status = noErr;
JNI_COCOA_ENTER(env);

    // I would love to use NSWorkspace here, but it's not thread safe. Why? I don't know.
    // So we use LaunchServices directly.

    NSURL *url = [NSURL URLWithString:JavaStringToNSString(env, uri)];

    LSLaunchFlags flags = kLSLaunchDefaults;

    LSApplicationParameters params = {0, flags, NULL, NULL, NULL, NULL, NULL};
    status = LSOpenURLsWithRole((CFArrayRef)[NSArray arrayWithObject:url], kLSRolesAll, NULL, &params, NULL, 0);

JNI_COCOA_EXIT(env);
    return status;
}

/*
 * Class:     sun_lwawt_macosx_CDesktopPeer
 * Method:    _lsOpenFile
 * Signature: (Ljava/lang/String;Z)I;
 */
JNIEXPORT jint JNICALL Java_sun_lwawt_macosx_CDesktopPeer__1lsOpenFile
(JNIEnv *env, jclass clz, jstring jpath, jboolean print)
{
    OSStatus status = noErr;
JNI_COCOA_ENTER(env);

    // I would love to use NSWorkspace here, but it's not thread safe. Why? I don't know.
    // So we use LaunchServices directly.

    NSString *path  = NormalizedPathNSStringFromJavaString(env, jpath);

    NSURL *url = [NSURL fileURLWithPath:(NSString *)path];

    // This byzantine workaround is necesary, or else directories won't open in Finder
    url = (NSURL *)CFURLCreateWithFileSystemPath(NULL, (CFStringRef)[url path], kCFURLPOSIXPathStyle, false);

    LSLaunchFlags flags = kLSLaunchDefaults;
    if (print) flags |= kLSLaunchAndPrint;

    LSApplicationParameters params = {0, flags, NULL, NULL, NULL, NULL, NULL};
    status = LSOpenURLsWithRole((CFArrayRef)[NSArray arrayWithObject:url], kLSRolesAll, NULL, &params, NULL, 0);
    [url release];

JNI_COCOA_EXIT(env);
    return status;
}

