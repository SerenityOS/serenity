/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARED_CDS_CDSPROTECTIONDOMAIN_HPP
#define SHARED_CDS_CDSPROTECTIONDOMAIN_HPP
#include "oops/oopHandle.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/thread.hpp"
#include "classfile/moduleEntry.hpp"

class InstanceKlass;
class Symbol;
class PackageEntry;
class ModuleEntry;

// CDS security
class CDSProtectionDomain : AllStatic {
  // See init_security_info for more info.
  static OopHandle _shared_protection_domains;
  static OopHandle _shared_jar_urls;
  static OopHandle _shared_jar_manifests;

public:
  // Package handling:
  //
  // 1. For named modules in the runtime image
  //    BOOT classes: Reuses the existing JVM_GetSystemPackage(s) interfaces
  //                  to get packages in named modules for shared classes.
  //                  Package for non-shared classes in named module is also
  //                  handled using JVM_GetSystemPackage(s).
  //
  //    APP  classes: VM calls ClassLoaders.AppClassLoader::definePackage(String, Module)
  //                  to define package for shared app classes from named
  //                  modules.
  //
  //    PLATFORM  classes: VM calls ClassLoaders.PlatformClassLoader::definePackage(String, Module)
  //                  to define package for shared platform classes from named
  //                  modules.
  //
  // 2. For unnamed modules
  //    BOOT classes: Reuses the existing JVM_GetSystemPackage(s) interfaces to
  //                  get packages for shared boot classes in unnamed modules.
  //
  //    APP  classes: VM calls ClassLoaders.AppClassLoader::defineOrCheckPackage()
  //                  with with the manifest and url from archived data.
  //
  //    PLATFORM  classes: No package is defined.
  //
  // The following two define_shared_package() functions are used to define
  // package for shared APP and PLATFORM classes.
  static Handle        get_package_name(Symbol*  class_name, TRAPS);
  static PackageEntry* get_package_entry_from_class(InstanceKlass* ik, Handle class_loader);
  static void define_shared_package(Symbol*  class_name,
                                    Handle class_loader,
                                    Handle manifest,
                                    Handle url,
                                    TRAPS);
  static Handle create_jar_manifest(const char* man, size_t size, TRAPS);
  static Handle get_shared_jar_manifest(int shared_path_index, TRAPS);
  static Handle get_shared_jar_url(int shared_path_index, TRAPS);
  static Handle get_protection_domain_from_classloader(Handle class_loader,
                                                       Handle url, TRAPS);
  static Handle get_shared_protection_domain(Handle class_loader,
                                             int shared_path_index,
                                             Handle url,
                                             TRAPS);
  static Handle get_shared_protection_domain(Handle class_loader,
                                             ModuleEntry* mod, TRAPS);
  static void atomic_set_array_index(OopHandle array, int index, oop o);
  static oop shared_protection_domain(int index);
  static void allocate_shared_protection_domain_array(int size, TRAPS);
  static oop shared_jar_url(int index);
  static void allocate_shared_jar_url_array(int size, TRAPS);
  static oop shared_jar_manifest(int index);
  static void allocate_shared_jar_manifest_array(int size, TRAPS);
  static Handle init_security_info(Handle class_loader, InstanceKlass* ik, PackageEntry* pkg_entry, TRAPS);

  static void allocate_shared_data_arrays(int size, TRAPS) {
    allocate_shared_protection_domain_array(size, CHECK);
    allocate_shared_jar_url_array(size, CHECK);
    allocate_shared_jar_manifest_array(size, CHECK);
  }
  static void atomic_set_shared_protection_domain(int index, oop pd) {
    atomic_set_array_index(_shared_protection_domains, index, pd);
  }
  static void atomic_set_shared_jar_url(int index, oop url) {
    atomic_set_array_index(_shared_jar_urls, index, url);
  }
  static void atomic_set_shared_jar_manifest(int index, oop man) {
    atomic_set_array_index(_shared_jar_manifests, index, man);
  }
};

#endif // SHARED_CDS_CDSPROTECTIONDOMAIN_HPP
