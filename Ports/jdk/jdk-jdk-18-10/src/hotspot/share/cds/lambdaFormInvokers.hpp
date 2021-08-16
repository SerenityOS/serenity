/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CDS_LAMBDAFORMINVOKERS_HPP
#define SHARE_CDS_LAMBDAFORMINVOKERS_HPP
#include "memory/allStatic.hpp"
#include "runtime/handles.hpp"
#include "utilities/growableArray.hpp"

class ClassFileStream;
template <class T>
class Array;

class LambdaFormInvokers : public AllStatic {
 private:
  static GrowableArrayCHeap<char*, mtClassShared>* _lambdaform_lines;
  // For storing LF form lines (LF_RESOLVE only) in read only table.
  static Array<Array<char>*>* _static_archive_invokers;
  static void reload_class(char* name, ClassFileStream& st, TRAPS);
 public:
  static void append(char* line);
  static void append_filtered(char* line);
  static void dump_static_archive_invokers();
  static void read_static_archive_invokers();
  static void regenerate_holder_classes(TRAPS);
  static void serialize(SerializeClosure* soc);
};
#endif // SHARE_CDS_LAMBDAFORMINVOKERS_HPP
