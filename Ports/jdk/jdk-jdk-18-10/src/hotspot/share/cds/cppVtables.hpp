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

#ifndef SHARE_CDS_CPPVTABLES_HPP
#define SHARE_CDS_CPPVTABLES_HPP

#include "memory/allocation.hpp"
#include "memory/allStatic.hpp"
#include "utilities/globalDefinitions.hpp"

class ArchiveBuilder;
class Method;
class SerializeClosure;
class CppVtableInfo;

// Support for C++ vtables in CDS archive.
class CppVtables : AllStatic {
  static CppVtableInfo** _index;
public:
  static char* dumptime_init(ArchiveBuilder* builder);
  static void zero_archived_vtables();
  static intptr_t* get_archived_vtable(MetaspaceObj::Type msotype, address obj);
  static void serialize(SerializeClosure* sc);
  static bool is_valid_shared_method(const Method* m) NOT_CDS_RETURN_(false);
};

#endif // SHARE_CDS_CPPVTABLES_HPP
