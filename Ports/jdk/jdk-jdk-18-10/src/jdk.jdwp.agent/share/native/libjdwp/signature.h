/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_SIGNATURE_H
#define JDWP_SIGNATURE_H

#define JVM_TYPE_ASSERT(typeKey)\
JDI_ASSERT_MSG(JDWP_Tag_OBJECT == typeKey || \
               JDWP_Tag_ARRAY == typeKey || \
               JDWP_Tag_BOOLEAN == typeKey || \
               JDWP_Tag_BYTE == typeKey || \
               JDWP_Tag_CHAR == typeKey || \
               JDWP_Tag_DOUBLE == typeKey || \
               JDWP_Tag_FLOAT == typeKey || \
               JDWP_Tag_INT == typeKey || \
               JDWP_Tag_LONG == typeKey || \
               JDWP_Tag_SHORT == typeKey || \
               JDWP_Tag_VOID == typeKey, \
               "Tag is not a JVM basic type")

static inline jbyte jdwpTag(const char *signature) {
     JVM_TYPE_ASSERT(signature[0]);
     return signature[0];
}

static inline jboolean isReferenceTag(jbyte typeKey) {
    JVM_TYPE_ASSERT(typeKey);
    return (typeKey == JDWP_TAG(OBJECT)) || (typeKey == JDWP_TAG(ARRAY));
}

static inline jboolean isArrayTag(jbyte typeKey) {
    JVM_TYPE_ASSERT(typeKey);
    return (typeKey == JDWP_TAG(ARRAY));
}

char* componentTypeSignature(const char *signature);

void convertSignatureToClassname(char *convert);

void methodSignature_init(char *signature, void **cursor);
jboolean methodSignature_nextArgumentExists(void **cursor, jbyte *argumentTag);
jbyte methodSignature_returnTag(char *signature);

#endif
