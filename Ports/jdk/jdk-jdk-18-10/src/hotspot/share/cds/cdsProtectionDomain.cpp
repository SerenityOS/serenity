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

#include "precompiled.hpp"
#include "cds/cdsProtectionDomain.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderExt.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/symbol.hpp"
#include "runtime/javaCalls.hpp"

OopHandle CDSProtectionDomain::_shared_protection_domains;
OopHandle CDSProtectionDomain::_shared_jar_urls;
OopHandle CDSProtectionDomain::_shared_jar_manifests;

// Initializes the java.lang.Package and java.security.ProtectionDomain objects associated with
// the given InstanceKlass.
// Returns the ProtectionDomain for the InstanceKlass.
Handle CDSProtectionDomain::init_security_info(Handle class_loader, InstanceKlass* ik, PackageEntry* pkg_entry, TRAPS) {
  Handle pd;

  if (ik != NULL) {
    int index = ik->shared_classpath_index();
    assert(index >= 0, "Sanity");
    SharedClassPathEntry* ent = FileMapInfo::shared_path(index);
    Symbol* class_name = ik->name();

    if (ent->is_modules_image()) {
      // For shared app/platform classes originated from the run-time image:
      //   The ProtectionDomains are cached in the corresponding ModuleEntries
      //   for fast access by the VM.
      // all packages from module image are already created during VM bootstrap in
      // Modules::define_module().
      assert(pkg_entry != NULL, "archived class in module image cannot be from unnamed package");
      ModuleEntry* mod_entry = pkg_entry->module();
      pd = get_shared_protection_domain(class_loader, mod_entry, CHECK_(pd));
    } else {
      // For shared app/platform classes originated from JAR files on the class path:
      //   Each of the 3 SystemDictionaryShared::_shared_xxx arrays has the same length
      //   as the shared classpath table in the shared archive (see
      //   FileMap::_shared_path_table in filemap.hpp for details).
      //
      //   If a shared InstanceKlass k is loaded from the class path, let
      //
      //     index = k->shared_classpath_index():
      //
      //   FileMap::_shared_path_table[index] identifies the JAR file that contains k.
      //
      //   k's protection domain is:
      //
      //     ProtectionDomain pd = _shared_protection_domains[index];
      //
      //   and k's Package is initialized using
      //
      //     manifest = _shared_jar_manifests[index];
      //     url = _shared_jar_urls[index];
      //     define_shared_package(class_name, class_loader, manifest, url, CHECK_(pd));
      //
      //   Note that if an element of these 3 _shared_xxx arrays is NULL, it will be initialized by
      //   the corresponding SystemDictionaryShared::get_shared_xxx() function.
      Handle manifest = get_shared_jar_manifest(index, CHECK_(pd));
      Handle url = get_shared_jar_url(index, CHECK_(pd));
      int index_offset = index - ClassLoaderExt::app_class_paths_start_index();
      if (index_offset < PackageEntry::max_index_for_defined_in_class_path()) {
        if (pkg_entry == NULL || !pkg_entry->is_defined_by_cds_in_class_path(index_offset)) {
          // define_shared_package only needs to be called once for each package in a jar specified
          // in the shared class path.
          define_shared_package(class_name, class_loader, manifest, url, CHECK_(pd));
          if (pkg_entry != NULL) {
            pkg_entry->set_defined_by_cds_in_class_path(index_offset);
          }
        }
      } else {
        define_shared_package(class_name, class_loader, manifest, url, CHECK_(pd));
      }
      pd = get_shared_protection_domain(class_loader, index, url, CHECK_(pd));
    }
  }
  return pd;
}

Handle CDSProtectionDomain::get_package_name(Symbol* class_name, TRAPS) {
  ResourceMark rm(THREAD);
  Handle pkgname_string;
  TempNewSymbol pkg = ClassLoader::package_from_class_name(class_name);
  if (pkg != NULL) { // Package prefix found
    const char* pkgname = pkg->as_klass_external_name();
    pkgname_string = java_lang_String::create_from_str(pkgname,
                                                       CHECK_(pkgname_string));
  }
  return pkgname_string;
}

PackageEntry* CDSProtectionDomain::get_package_entry_from_class(InstanceKlass* ik, Handle class_loader) {
  PackageEntry* pkg_entry = ik->package();
  if (MetaspaceShared::use_full_module_graph() && ik->is_shared() && pkg_entry != NULL) {
    assert(MetaspaceShared::is_in_shared_metaspace(pkg_entry), "must be");
    assert(!ik->is_shared_unregistered_class(), "unexpected archived package entry for an unregistered class");
    assert(ik->module()->is_named(), "unexpected archived package entry for a class in an unnamed module");
    return pkg_entry;
  }
  TempNewSymbol pkg_name = ClassLoader::package_from_class_name(ik->name());
  if (pkg_name != NULL) {
    pkg_entry = SystemDictionaryShared::class_loader_data(class_loader)->packages()->lookup_only(pkg_name);
  } else {
    pkg_entry = NULL;
  }
  return pkg_entry;
}

// Define Package for shared app classes from JAR file and also checks for
// package sealing (all done in Java code)
// See http://docs.oracle.com/javase/tutorial/deployment/jar/sealman.html
void CDSProtectionDomain::define_shared_package(Symbol*  class_name,
                                                   Handle class_loader,
                                                   Handle manifest,
                                                   Handle url,
                                                   TRAPS) {
  assert(SystemDictionary::is_system_class_loader(class_loader()), "unexpected class loader");
  // get_package_name() returns a NULL handle if the class is in unnamed package
  Handle pkgname_string = get_package_name(class_name, CHECK);
  if (pkgname_string.not_null()) {
    Klass* app_classLoader_klass = vmClasses::jdk_internal_loader_ClassLoaders_AppClassLoader_klass();
    JavaValue result(T_OBJECT);
    JavaCallArguments args(3);
    args.set_receiver(class_loader);
    args.push_oop(pkgname_string);
    args.push_oop(manifest);
    args.push_oop(url);
    JavaCalls::call_virtual(&result, app_classLoader_klass,
                            vmSymbols::defineOrCheckPackage_name(),
                            vmSymbols::defineOrCheckPackage_signature(),
                            &args,
                            CHECK);
  }
}

Handle CDSProtectionDomain::create_jar_manifest(const char* manifest_chars, size_t size, TRAPS) {
  typeArrayOop buf = oopFactory::new_byteArray((int)size, CHECK_NH);
  typeArrayHandle bufhandle(THREAD, buf);
  ArrayAccess<>::arraycopy_from_native(reinterpret_cast<const jbyte*>(manifest_chars),
                                         buf, typeArrayOopDesc::element_offset<jbyte>(0), size);
  Handle bais = JavaCalls::construct_new_instance(vmClasses::ByteArrayInputStream_klass(),
                      vmSymbols::byte_array_void_signature(),
                      bufhandle, CHECK_NH);
  // manifest = new Manifest(ByteArrayInputStream)
  Handle manifest = JavaCalls::construct_new_instance(vmClasses::Jar_Manifest_klass(),
                      vmSymbols::input_stream_void_signature(),
                      bais, CHECK_NH);
  return manifest;
}

Handle CDSProtectionDomain::get_shared_jar_manifest(int shared_path_index, TRAPS) {
  Handle manifest;
  if (shared_jar_manifest(shared_path_index) == NULL) {
    SharedClassPathEntry* ent = FileMapInfo::shared_path(shared_path_index);
    size_t size = (size_t)ent->manifest_size();
    if (size == 0) {
      return Handle();
    }

    // ByteArrayInputStream bais = new ByteArrayInputStream(buf);
    const char* src = ent->manifest();
    assert(src != NULL, "No Manifest data");
    manifest = create_jar_manifest(src, size, CHECK_NH);
    atomic_set_shared_jar_manifest(shared_path_index, manifest());
  }
  manifest = Handle(THREAD, shared_jar_manifest(shared_path_index));
  assert(manifest.not_null(), "sanity");
  return manifest;
}

Handle CDSProtectionDomain::get_shared_jar_url(int shared_path_index, TRAPS) {
  Handle url_h;
  if (shared_jar_url(shared_path_index) == NULL) {
    JavaValue result(T_OBJECT);
    const char* path = FileMapInfo::shared_path_name(shared_path_index);
    Handle path_string = java_lang_String::create_from_str(path, CHECK_(url_h));
    Klass* classLoaders_klass =
        vmClasses::jdk_internal_loader_ClassLoaders_klass();
    JavaCalls::call_static(&result, classLoaders_klass,
                           vmSymbols::toFileURL_name(),
                           vmSymbols::toFileURL_signature(),
                           path_string, CHECK_(url_h));

    atomic_set_shared_jar_url(shared_path_index, result.get_oop());
  }

  url_h = Handle(THREAD, shared_jar_url(shared_path_index));
  assert(url_h.not_null(), "sanity");
  return url_h;
}

// Get the ProtectionDomain associated with the CodeSource from the classloader.
Handle CDSProtectionDomain::get_protection_domain_from_classloader(Handle class_loader,
                                                                      Handle url, TRAPS) {
  // CodeSource cs = new CodeSource(url, null);
  Handle cs = JavaCalls::construct_new_instance(vmClasses::CodeSource_klass(),
                  vmSymbols::url_code_signer_array_void_signature(),
                  url, Handle(), CHECK_NH);

  // protection_domain = SecureClassLoader.getProtectionDomain(cs);
  Klass* secureClassLoader_klass = vmClasses::SecureClassLoader_klass();
  JavaValue obj_result(T_OBJECT);
  JavaCalls::call_virtual(&obj_result, class_loader, secureClassLoader_klass,
                          vmSymbols::getProtectionDomain_name(),
                          vmSymbols::getProtectionDomain_signature(),
                          cs, CHECK_NH);
  return Handle(THREAD, obj_result.get_oop());
}

// Returns the ProtectionDomain associated with the JAR file identified by the url.
Handle CDSProtectionDomain::get_shared_protection_domain(Handle class_loader,
                                                            int shared_path_index,
                                                            Handle url,
                                                            TRAPS) {
  Handle protection_domain;
  if (shared_protection_domain(shared_path_index) == NULL) {
    Handle pd = get_protection_domain_from_classloader(class_loader, url, THREAD);
    atomic_set_shared_protection_domain(shared_path_index, pd());
  }

  // Acquire from the cache because if another thread beats the current one to
  // set the shared protection_domain and the atomic_set fails, the current thread
  // needs to get the updated protection_domain from the cache.
  protection_domain = Handle(THREAD, shared_protection_domain(shared_path_index));
  assert(protection_domain.not_null(), "sanity");
  return protection_domain;
}

// Returns the ProtectionDomain associated with the moduleEntry.
Handle CDSProtectionDomain::get_shared_protection_domain(Handle class_loader,
                                                         ModuleEntry* mod, TRAPS) {
  ClassLoaderData *loader_data = mod->loader_data();
  if (mod->shared_protection_domain() == NULL) {
    Symbol* location = mod->location();
    if (location != NULL) {
      Handle location_string = java_lang_String::create_from_symbol(
                                     location, CHECK_NH);
      Handle url;
      JavaValue result(T_OBJECT);
      if (location->starts_with("jrt:/")) {
        url = JavaCalls::construct_new_instance(vmClasses::URL_klass(),
                                                vmSymbols::string_void_signature(),
                                                location_string, CHECK_NH);
      } else {
        Klass* classLoaders_klass =
          vmClasses::jdk_internal_loader_ClassLoaders_klass();
        JavaCalls::call_static(&result, classLoaders_klass, vmSymbols::toFileURL_name(),
                               vmSymbols::toFileURL_signature(),
                               location_string, CHECK_NH);
        url = Handle(THREAD, result.get_oop());
      }

      Handle pd = get_protection_domain_from_classloader(class_loader, url,
                                                         CHECK_NH);
      mod->set_shared_protection_domain(loader_data, pd);
    }
  }

  Handle protection_domain(THREAD, mod->shared_protection_domain());
  assert(protection_domain.not_null(), "sanity");
  return protection_domain;
}

void CDSProtectionDomain::atomic_set_array_index(OopHandle array, int index, oop o) {
  // Benign race condition:  array.obj_at(index) may already be filled in.
  // The important thing here is that all threads pick up the same result.
  // It doesn't matter which racing thread wins, as long as only one
  // result is used by all threads, and all future queries.
  ((objArrayOop)array.resolve())->atomic_compare_exchange_oop(index, o, NULL);
}

oop CDSProtectionDomain::shared_protection_domain(int index) {
  return ((objArrayOop)_shared_protection_domains.resolve())->obj_at(index);
}

void CDSProtectionDomain::allocate_shared_protection_domain_array(int size, TRAPS) {
  if (_shared_protection_domains.resolve() == NULL) {
    oop spd = oopFactory::new_objArray(
        vmClasses::ProtectionDomain_klass(), size, CHECK);
    _shared_protection_domains = OopHandle(Universe::vm_global(), spd);
  }
}

oop CDSProtectionDomain::shared_jar_url(int index) {
  return ((objArrayOop)_shared_jar_urls.resolve())->obj_at(index);
}

void CDSProtectionDomain::allocate_shared_jar_url_array(int size, TRAPS) {
  if (_shared_jar_urls.resolve() == NULL) {
    oop sju = oopFactory::new_objArray(
        vmClasses::URL_klass(), size, CHECK);
    _shared_jar_urls = OopHandle(Universe::vm_global(), sju);
  }
}

oop CDSProtectionDomain::shared_jar_manifest(int index) {
  return ((objArrayOop)_shared_jar_manifests.resolve())->obj_at(index);
}

void CDSProtectionDomain::allocate_shared_jar_manifest_array(int size, TRAPS) {
  if (_shared_jar_manifests.resolve() == NULL) {
    oop sjm = oopFactory::new_objArray(
        vmClasses::Jar_Manifest_klass(), size, CHECK);
    _shared_jar_manifests = OopHandle(Universe::vm_global(), sjm);
  }
}
