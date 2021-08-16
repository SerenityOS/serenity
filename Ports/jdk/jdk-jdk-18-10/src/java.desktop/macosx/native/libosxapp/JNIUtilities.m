/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "JNIUtilities.h"

NSString* JavaStringToNSString(JNIEnv *env, jstring jstr) {
    if (jstr == NULL) {
        return NULL;
    }
    jsize len = (*env)->GetStringLength(env, jstr);
    const jchar *chars = (*env)->GetStringChars(env, jstr, NULL);
    if (chars == NULL) {
        return NULL;
    }
    NSString *result = [NSString stringWithCharacters:(UniChar *)chars length:len];
    (*env)->ReleaseStringChars(env, jstr, chars);
    return result;
}

jstring NSStringToJavaString(JNIEnv* env, NSString *str) {

    if (str == NULL) {
       return NULL;
    }
    jstring jStr = (*env)->NewStringUTF(env, [str UTF8String]);
    CHECK_EXCEPTION();
    return jStr;
}

/*
 * These next conversion functions are for file system paths.
 * The NSString needs to be in de-composed UTF-16 format for the Apple file system
 * The Java String needs to be in pre-composed UTF-16 format for display by Java.
 * https://developer.apple.com/library/archive/qa/qa1235/_index.html
 * has some information on this.
 */

/*
 * Returns an NSString in decomposed UTF16 format that is compatible with HFS's
 * expectation of the UTF16 format for file system paths.
 *
 * Example string: "/Users/Amélie/"
 *
 * Java's UTF16 string is "/ U s e r s / A m \351 l i e /"
 * macOS UTF16 string suitable for HFS is "/ U s e r s / A m e \314 \201 l i e /"
 *
 * There is no direct API that takes in NSString UTF16 encoded by Java
 * and produces NSString UTF16 for HFS, so we first need to decompose it
 * into chars (suitable for low level C file APIs), and only then
 * create NSString representation of this decomposition back into UTF16 string.
 *
 * https://developer.apple.com/documentation/foundation/nsstring/1414559-filesystemrepresentation?language=objc
 * describes how to get a file system representation as a char* from an NSString
 * and then using FileManager (!) convert it to an NSString.
 * But we want an NSString.
 * So the steps are
 * 1) Convert to NSString
 * 2) call [NSString fileSystemRepresentation] which gives us a char*
 * 3) Convert the returned char* to an NSString using FileManager (is there a better way?)
 */
NSString* NormalizedPathNSStringFromJavaString(JNIEnv *env, jstring pathStr) {
    if (pathStr == NULL) {
        return nil;
    }
    NSString *nsStr = JavaStringToNSString(env, pathStr);
    if (nsStr == NULL) {
        return nil;
    }
    const char* chs = [nsStr fileSystemRepresentation];
    int len = strlen(chs);
    NSString* result = [[NSFileManager defaultManager]
                  stringWithFileSystemRepresentation:chs length:len];
    return result;
}

/*
 * Given what is (potentially) a de-composed NSString, convert it to pre-composed
 * Then convert it into a Java String.
 */
jstring NormalizedPathJavaStringFromNSString(JNIEnv* env, NSString *str) {
    if (str == nil) {
        return NULL;
    }
    NSString *normStr = [str precomposedStringWithCanonicalMapping];
    return NSStringToJavaString(env, normStr);
}
