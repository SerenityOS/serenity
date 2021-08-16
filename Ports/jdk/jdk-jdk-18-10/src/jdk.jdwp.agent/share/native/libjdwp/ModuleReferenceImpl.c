/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "util.h"
#include "inStream.h"
#include "outStream.h"
#include "ModuleReferenceImpl.h"


static jclass jlM(JNIEnv *env) {
    return findClass(env, "Ljava/lang/Module;");
}

static jboolean
getName(PacketInputStream *in, PacketOutputStream *out)
{
    static jmethodID method = NULL;
    JNIEnv *env = getEnv();
    char *name = NULL;
    jstring namestr;
    jobject module;

    if (method == NULL) {
        method = getMethod(env, jlM(env), "getName", "()Ljava/lang/String;");
    }
    module = inStream_readModuleRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    namestr = (jstring)JNI_FUNC_PTR(env, CallObjectMethod) (env, module, method);
    if (namestr != NULL) {
        name = (char*)JNI_FUNC_PTR(env, GetStringUTFChars)(env, namestr, NULL);
    } else {
        // The JDWP converts null into an empty string
    }
    (void)outStream_writeString(out, name);
    if (name != NULL) {
        JNI_FUNC_PTR(env, ReleaseStringUTFChars)(env, namestr, name);
    }
    return JNI_TRUE;
}

static jboolean
getClassLoader(PacketInputStream *in, PacketOutputStream *out)
{
    static jmethodID method = NULL;
    JNIEnv *env = getEnv();
    jobject loader;
    jobject module;

    if (method == NULL) {
        method = getMethod(env, jlM(env), "getClassLoader", "()Ljava/lang/ClassLoader;");
    }
    module = inStream_readModuleRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    loader = JNI_FUNC_PTR(env, CallObjectMethod) (env, module, method);

    (void)outStream_writeObjectRef(env, out, loader);
    return JNI_TRUE;
}

Command ModuleReference_Commands[] = {
    {getName, "GetName"},
    {getClassLoader, "GetClassLoader"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ModuleReference)
