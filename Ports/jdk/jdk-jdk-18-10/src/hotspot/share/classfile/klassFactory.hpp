/*
* Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_KLASSFACTORY_HPP
#define SHARE_CLASSFILE_KLASSFACTORY_HPP

#include "memory/allocation.hpp"
#include "runtime/handles.hpp"

class ClassFileStream;
class ClassLoaderData;
class ClassLoadInfo;
class Symbol;

/*
 * KlassFactory is an interface to implementations of the following mapping/function:
 *
 * Summary: create a VM internal runtime representation ("Klass")
            from a bytestream (classfile).
 *
 * Input:  a named bytestream in the Java class file format (see JVMS, chapter 4).
 * Output: a VM runtime representation of a Java class
 *
 * Pre-conditions:
 *   a non-NULL ClassFileStream* // the classfile bytestream
 *   a non-NULL Symbol*          // the name of the class
 *   a non-NULL ClassLoaderData* // the metaspace allocator
 *   (no pending exceptions)
 *
 * Returns:
 *   if the returned value is non-NULL, that value is an indirection (pointer/handle)
 *   to a Klass. The caller will not have a pending exception.
 *
 *   On broken invariants and/or runtime errors the returned value will be
 *   NULL (or a NULL handle) and the caller *might* now have a pending exception.
 *
 */

class KlassFactory : AllStatic {

 public:
  static InstanceKlass* create_from_stream(ClassFileStream* stream,
                                           Symbol* name,
                                           ClassLoaderData* loader_data,
                                           const ClassLoadInfo& cl_info,
                                           TRAPS);
  static InstanceKlass* check_shared_class_file_load_hook(
                                          InstanceKlass* ik,
                                          Symbol* class_name,
                                          Handle class_loader,
                                          Handle protection_domain,
                                          const ClassFileStream *cfs,
                                          TRAPS);
};

#endif // SHARE_CLASSFILE_KLASSFACTORY_HPP
