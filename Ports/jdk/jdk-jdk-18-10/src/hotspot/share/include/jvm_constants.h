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

#ifndef _JVM_CONSTANTS_H_
#define _JVM_CONSTANTS_H_

#include "classfile_constants.h"

#define JVM_RECOGNIZED_CLASS_MODIFIERS (JVM_ACC_PUBLIC | \
                                        JVM_ACC_FINAL | \
                                        JVM_ACC_SUPER | \
                                        JVM_ACC_INTERFACE | \
                                        JVM_ACC_ABSTRACT | \
                                        JVM_ACC_ANNOTATION | \
                                        JVM_ACC_ENUM | \
                                        JVM_ACC_SYNTHETIC)

#define JVM_RECOGNIZED_FIELD_MODIFIERS (JVM_ACC_PUBLIC | \
                                        JVM_ACC_PRIVATE | \
                                        JVM_ACC_PROTECTED | \
                                        JVM_ACC_STATIC | \
                                        JVM_ACC_FINAL | \
                                        JVM_ACC_VOLATILE | \
                                        JVM_ACC_TRANSIENT | \
                                        JVM_ACC_ENUM | \
                                        JVM_ACC_SYNTHETIC)

#define JVM_RECOGNIZED_METHOD_MODIFIERS (JVM_ACC_PUBLIC | \
                                         JVM_ACC_PRIVATE | \
                                         JVM_ACC_PROTECTED | \
                                         JVM_ACC_STATIC | \
                                         JVM_ACC_FINAL | \
                                         JVM_ACC_SYNCHRONIZED | \
                                         JVM_ACC_BRIDGE | \
                                         JVM_ACC_VARARGS | \
                                         JVM_ACC_NATIVE | \
                                         JVM_ACC_ABSTRACT | \
                                         JVM_ACC_STRICT | \
                                         JVM_ACC_SYNTHETIC)

#endif /* _JVM_CONSTANTS_H_ */
