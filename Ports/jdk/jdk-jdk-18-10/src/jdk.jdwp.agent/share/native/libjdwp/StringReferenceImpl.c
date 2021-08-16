/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "StringReferenceImpl.h"
#include "inStream.h"
#include "outStream.h"

static jboolean
value(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jstring string;

    env = getEnv();

    string = inStream_readStringRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        char *utf;

        utf = (char *)JNI_FUNC_PTR(env,GetStringUTFChars)(env, string, NULL);
        if (!(*env)->ExceptionCheck(env)) {
            (void)outStream_writeString(out, utf);
            JNI_FUNC_PTR(env,ReleaseStringUTFChars)(env, string, utf);
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

Command StringReference_Commands[] = {
    {value, "Value"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(StringReference)
