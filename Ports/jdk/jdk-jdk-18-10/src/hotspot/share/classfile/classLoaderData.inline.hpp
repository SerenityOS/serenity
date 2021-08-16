/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSLOADERDATA_INLINE_HPP
#define SHARE_CLASSFILE_CLASSLOADERDATA_INLINE_HPP

#include "classfile/classLoaderData.hpp"

#include "classfile/javaClasses.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "oops/weakHandle.inline.hpp"

inline oop ClassLoaderData::class_loader() const {
  assert(!_unloading, "This oop is not available to unloading class loader data");
  assert(_holder.is_null() || holder_no_keepalive() != NULL , "This class loader data holder must be alive");
  return _class_loader.resolve();
}

inline bool ClassLoaderData::is_boot_class_loader_data() const {
  return this == _the_null_class_loader_data || class_loader() == NULL;
}

inline ClassLoaderData* ClassLoaderData::class_loader_data_or_null(oop loader) {
  if (loader == NULL) {
    return ClassLoaderData::the_null_class_loader_data();
  }
  return java_lang_ClassLoader::loader_data_acquire(loader);
}

inline ClassLoaderData* ClassLoaderData::class_loader_data(oop loader) {
  ClassLoaderData* loader_data = class_loader_data_or_null(loader);
  assert(loader_data != NULL, "Must be");
  return loader_data;
}

#endif // SHARE_CLASSFILE_CLASSLOADERDATA_INLINE_HPP
