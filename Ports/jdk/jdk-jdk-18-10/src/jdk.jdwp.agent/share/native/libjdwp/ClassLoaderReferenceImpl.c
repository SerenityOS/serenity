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
#include "ClassLoaderReferenceImpl.h"
#include "inStream.h"
#include "outStream.h"

static jboolean
visibleClasses(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env = getEnv();
    jobject loader;

    loader = inStream_readClassLoaderRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiError error;
        jint count;
        jclass *classes;
        int i;

        error = allClassLoaderClasses(loader, &classes, &count);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            (void)outStream_writeInt(out, count);
            for (i = 0; i < count; i++) {
                jbyte tag;
                jclass clazz;

                clazz = classes[i];
                tag = referenceTypeTag(clazz);

                (void)outStream_writeByte(out, tag);
                (void)outStream_writeObjectRef(env, out, clazz);
            }
        }

        if ( classes != NULL )
            jvmtiDeallocate(classes);

     } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

Command ClassLoaderReference_Commands[] = {
    {visibleClasses, "VisibleClasses"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ClassLoaderReference)
