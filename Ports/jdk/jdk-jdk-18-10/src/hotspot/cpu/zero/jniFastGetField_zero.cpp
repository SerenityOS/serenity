/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007 Red Hat, Inc.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

#include "precompiled.hpp"
#include "asm/assembler.inline.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "runtime/safepoint.hpp"

address JNI_FastGetField::generate_fast_get_boolean_field() {
  return (address) -1;
}

address JNI_FastGetField::generate_fast_get_byte_field() {
  return (address) -1;
}

address JNI_FastGetField::generate_fast_get_char_field() {
  return (address) -1;
}

address JNI_FastGetField::generate_fast_get_short_field() {
  return (address) -1;
}

address JNI_FastGetField::generate_fast_get_int_field() {
  return (address) -1;
}

address JNI_FastGetField::generate_fast_get_long_field() {
  return (address) -1;
}

address JNI_FastGetField::generate_fast_get_float_field() {
  return (address) -1;
}

address JNI_FastGetField::generate_fast_get_double_field() {
  return (address) -1;
}
