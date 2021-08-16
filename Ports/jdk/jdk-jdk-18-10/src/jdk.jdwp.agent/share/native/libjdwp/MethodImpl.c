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
#include "MethodImpl.h"
#include "inStream.h"
#include "outStream.h"

static jboolean
lineTable(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jint count = 0;
    jvmtiLineNumberEntry *table = NULL;
    jmethodID method;
    jlocation firstCodeIndex;
    jlocation lastCodeIndex;
    jboolean isNative;

    /* JVMDI needed the class, but JVMTI does not so we ignore it */
    (void)inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    method = inStream_readMethodID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /*
     * JVMTI behavior for the calls below is unspecified for native
     * methods, so we must check explicitly.
     */
    isNative = isMethodNative(method);
    if (isNative) {
        outStream_setError(out, JDWP_ERROR(NATIVE_METHOD));
        return JNI_TRUE;
    }

    error = methodLocation(method, &firstCodeIndex, &lastCodeIndex);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }
    (void)outStream_writeLocation(out, firstCodeIndex);
    (void)outStream_writeLocation(out, lastCodeIndex);

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLineNumberTable)
                (gdata->jvmti, method, &count, &table);
    if (error == JVMTI_ERROR_ABSENT_INFORMATION) {
        /*
         * Indicate no line info with an empty table. The code indices
         * are still useful, so we don't want to return an error
         */
        (void)outStream_writeInt(out, 0);
    } else if (error == JVMTI_ERROR_NONE) {
        jint i;
        (void)outStream_writeInt(out, count);
        for (i = 0; (i < count) && !outStream_error(out); i++) {
            (void)outStream_writeLocation(out, table[i].start_location);
            (void)outStream_writeInt(out, table[i].line_number);
        }
        jvmtiDeallocate(table);
    } else {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}


static jboolean
doVariableTable(PacketInputStream *in, PacketOutputStream *out,
                int outputGenerics)
{
    jvmtiError error;
    jint count;
    jvmtiLocalVariableEntry *table;
    jmethodID method;
    jint argsSize;
    jboolean isNative;

    /* JVMDI needed the class, but JVMTI does not so we ignore it */
    (void)inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    method = inStream_readMethodID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /*
     * JVMTI behavior for the calls below is unspecified for native
     * methods, so we must check explicitly.
     */
    isNative = isMethodNative(method);
    if (isNative) {
        outStream_setError(out, JDWP_ERROR(NATIVE_METHOD));
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetArgumentsSize)
                (gdata->jvmti, method, &argsSize);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalVariableTable)
                (gdata->jvmti, method, &count, &table);
    if (error == JVMTI_ERROR_NONE) {
        jint i;
        (void)outStream_writeInt(out, argsSize);
        (void)outStream_writeInt(out, count);
        for (i = 0; (i < count) && !outStream_error(out); i++) {
            jvmtiLocalVariableEntry *entry = &table[i];
            (void)outStream_writeLocation(out, entry->start_location);
            (void)outStream_writeString(out, entry->name);
            (void)outStream_writeString(out, entry->signature);
            if (outputGenerics == 1) {
                writeGenericSignature(out, entry->generic_signature);
            }
            (void)outStream_writeInt(out, entry->length);
            (void)outStream_writeInt(out, entry->slot);

            jvmtiDeallocate(entry->name);
            jvmtiDeallocate(entry->signature);
            if (entry->generic_signature != NULL) {
              jvmtiDeallocate(entry->generic_signature);
            }
        }

        jvmtiDeallocate(table);
    } else {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}


static jboolean
variableTable(PacketInputStream *in, PacketOutputStream *out) {
    return doVariableTable(in, out, 0);
}

static jboolean
variableTableWithGenerics(PacketInputStream *in, PacketOutputStream *out) {
    return doVariableTable(in, out, 1);
}


static jboolean
bytecodes(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    unsigned char * bcp;
    jint bytecodeCount;
    jmethodID method;

    /* JVMDI needed the class, but JVMTI does not so we ignore it */
    (void)inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    method = inStream_readMethodID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /* Initialize assuming no bytecodes and no error */
    error         = JVMTI_ERROR_NONE;
    bytecodeCount = 0;
    bcp           = NULL;

    /* Only non-native methods have bytecodes, don't even ask if native. */
    if ( !isMethodNative(method) ) {
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetBytecodes)
                    (gdata->jvmti, method, &bytecodeCount, &bcp);
    }
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    } else {
        (void)outStream_writeByteArray(out, bytecodeCount, (jbyte *)bcp);
        jvmtiDeallocate(bcp);
    }

    return JNI_TRUE;
}

static jboolean
isObsolete(PacketInputStream *in, PacketOutputStream *out)
{
    jboolean isObsolete;
    jmethodID method;

    /* JVMDI needed the class, but JVMTI does not so we ignore it */
    (void)inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    method = inStream_readMethodID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    isObsolete = isMethodObsolete(method);
    (void)outStream_writeBoolean(out, isObsolete);

    return JNI_TRUE;
}

Command Method_Commands[] = {
    {lineTable, "LineTable"},
    {variableTable, "VariableTable"},
    {bytecodes, "Bytecodes"},
    {isObsolete, "IsObsolete"},
    {variableTableWithGenerics, "VariableTableWithGenerics"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(Method)
