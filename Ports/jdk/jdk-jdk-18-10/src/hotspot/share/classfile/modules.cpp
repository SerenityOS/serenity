/*
* Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "cds/metaspaceShared.hpp"
#include "classfile/classFileParser.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataShared.hpp"
#include "classfile/javaAssertions.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/modules.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/stringUtils.hpp"
#include "utilities/utf8.hpp"

static bool verify_module_name(const char *module_name, int len) {
  assert(module_name != NULL, "invariant");
  return (len > 0 && len <= Symbol::max_length());
}

static bool verify_package_name(const char* package_name, int len) {
  assert(package_name != NULL, "Package name derived from non-null jstring can't be NULL");
  return (len > 0 && len <= Symbol::max_length() &&
    ClassFileParser::verify_unqualified_name(package_name, len,
    ClassFileParser::LegalClass));
}

static char* get_module_name(oop module, int& len, TRAPS) {
  oop name_oop = java_lang_Module::name(module);
  if (name_oop == NULL) {
    THROW_MSG_NULL(vmSymbols::java_lang_NullPointerException(), "Null module name");
  }
  char* module_name = java_lang_String::as_utf8_string(name_oop, len);
  if (!verify_module_name(module_name, len)) {
    THROW_MSG_NULL(vmSymbols::java_lang_IllegalArgumentException(),
                   err_msg("Invalid module name: %s", module_name));
  }
  return module_name;
}

static Symbol* as_symbol(jstring str_object) {
  if (str_object == NULL) {
    return NULL;
  }
  int len;
  char* str = java_lang_String::as_utf8_string(JNIHandles::resolve_non_null(str_object), len);
  return SymbolTable::new_symbol(str, len);
}

ModuleEntryTable* Modules::get_module_entry_table(Handle h_loader) {
  // This code can be called during start-up, before the classLoader's classLoader data got
  // created.  So, call register_loader() to make sure the classLoader data gets created.
  ClassLoaderData *loader_cld = SystemDictionary::register_loader(h_loader);
  return loader_cld->modules();
}

static PackageEntryTable* get_package_entry_table(Handle h_loader) {
  // This code can be called during start-up, before the classLoader's classLoader data got
  // created.  So, call register_loader() to make sure the classLoader data gets created.
  ClassLoaderData *loader_cld = SystemDictionary::register_loader(h_loader);
  return loader_cld->packages();
}

static ModuleEntry* get_module_entry(Handle module, TRAPS) {
  if (!java_lang_Module::is_instance(module())) {
    THROW_MSG_NULL(vmSymbols::java_lang_IllegalArgumentException(),
                   "module is not an instance of type java.lang.Module");
  }
  return java_lang_Module::module_entry(module());
}


static PackageEntry* get_locked_package_entry(ModuleEntry* module_entry, const char* package_name, int len) {
  assert(Module_lock->owned_by_self(), "should have the Module_lock");
  assert(package_name != NULL, "Precondition");
  TempNewSymbol pkg_symbol = SymbolTable::new_symbol(package_name, len);
  PackageEntryTable* package_entry_table = module_entry->loader_data()->packages();
  assert(package_entry_table != NULL, "Unexpected null package entry table");
  PackageEntry* package_entry = package_entry_table->locked_lookup_only(pkg_symbol);
  assert(package_entry == NULL || package_entry->module() == module_entry, "Unexpectedly found a package linked to another module");
  return package_entry;
}

static PackageEntry* get_package_entry_by_name(Symbol* package, Handle h_loader) {
  if (package != NULL) {
    PackageEntryTable* const package_entry_table =
      get_package_entry_table(h_loader);
    assert(package_entry_table != NULL, "Unexpected null package entry table");
    return package_entry_table->lookup_only(package);
  }
  return NULL;
}

bool Modules::is_package_defined(Symbol* package, Handle h_loader) {
  PackageEntry* res = get_package_entry_by_name(package, h_loader);
  return res != NULL;
}

// Converts the String oop to an internal package
// Will use the provided buffer if it's sufficiently large, otherwise allocates
// a resource array
// The length of the resulting string will be assigned to utf8_len
static const char* as_internal_package(oop package_string, char* buf, int buflen, int& utf8_len) {
  char* package_name = java_lang_String::as_utf8_string_full(package_string, buf, buflen, utf8_len);

  // Turn all '/'s into '.'s
  for (int index = 0; index < utf8_len; index++) {
    if (package_name[index] == JVM_SIGNATURE_DOT) {
      package_name[index] = JVM_SIGNATURE_SLASH;
    }
  }
  return package_name;
}

static void define_javabase_module(Handle module_handle, jstring version, jstring location,
                                   objArrayHandle pkgs, int num_packages, TRAPS) {
  ResourceMark rm(THREAD);

  // Obtain java.base's module version
  TempNewSymbol version_symbol = as_symbol(version);

  // Obtain java.base's location
  TempNewSymbol location_symbol = as_symbol(location);

  // Check that the packages are syntactically ok.
  char buf[128];
  GrowableArray<Symbol*>* pkg_list = new GrowableArray<Symbol*>(num_packages);
  for (int x = 0; x < num_packages; x++) {
    oop pkg_str = pkgs->obj_at(x);

    if (pkg_str == NULL || pkg_str->klass() != vmClasses::String_klass()) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                err_msg("Bad package name"));
    }

    int package_len;
    const char* package_name = as_internal_package(pkg_str, buf, sizeof(buf), package_len);
    if (!verify_package_name(package_name, package_len)) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                err_msg("Invalid package name: %s for module: " JAVA_BASE_NAME, package_name));
    }
    Symbol* pkg_symbol = SymbolTable::new_symbol(package_name, package_len);
    pkg_list->append(pkg_symbol);
  }

  // Validate java_base's loader is the boot loader.
  oop loader = java_lang_Module::loader(module_handle());
  if (loader != NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Class loader must be the boot class loader");
  }
  Handle h_loader(THREAD, loader);

  // Ensure the boot loader's PackageEntryTable has been created
  PackageEntryTable* package_table = get_package_entry_table(h_loader);
  assert(pkg_list->length() == 0 || package_table != NULL, "Bad package_table");

  // Ensure java.base's ModuleEntry has been created
  assert(ModuleEntryTable::javabase_moduleEntry() != NULL, "No ModuleEntry for " JAVA_BASE_NAME);

  bool duplicate_javabase = false;
  {
    MutexLocker m1(THREAD, Module_lock);

    if (ModuleEntryTable::javabase_defined()) {
      duplicate_javabase = true;
    } else {

      // Verify that all java.base packages created during bootstrapping are in
      // pkg_list.  If any are not in pkg_list, than a non-java.base class was
      // loaded erroneously pre java.base module definition.
      package_table->verify_javabase_packages(pkg_list);

      // loop through and add any new packages for java.base
      for (int x = 0; x < pkg_list->length(); x++) {
        // Some of java.base's packages were added early in bootstrapping, ignore duplicates.
        package_table->locked_create_entry_if_not_exist(pkg_list->at(x),
                                                        ModuleEntryTable::javabase_moduleEntry());
        assert(package_table->locked_lookup_only(pkg_list->at(x)) != NULL,
               "Unable to create a " JAVA_BASE_NAME " package entry");
        // Unable to have a GrowableArray of TempNewSymbol.  Must decrement the refcount of
        // the Symbol* that was created above for each package. The refcount was incremented
        // by SymbolTable::new_symbol and as well by the PackageEntry creation.
        pkg_list->at(x)->decrement_refcount();
      }

      // Finish defining java.base's ModuleEntry
      ModuleEntryTable::finalize_javabase(module_handle, version_symbol, location_symbol);
    }
  }
  if (duplicate_javabase) {
    THROW_MSG(vmSymbols::java_lang_InternalError(),
              "Module " JAVA_BASE_NAME " is already defined");
  }

  // Only the thread that actually defined the base module will get here,
  // so no locking is needed.

  // Patch any previously loaded class's module field with java.base's java.lang.Module.
  ModuleEntryTable::patch_javabase_entries(module_handle);

  log_info(module, load)(JAVA_BASE_NAME " location: %s",
                         location_symbol != NULL ? location_symbol->as_C_string() : "NULL");
  log_debug(module)("define_javabase_module(): Definition of module: "
                    JAVA_BASE_NAME ", version: %s, location: %s, package #: %d",
                    version_symbol != NULL ? version_symbol->as_C_string() : "NULL",
                    location_symbol != NULL ? location_symbol->as_C_string() : "NULL",
                    pkg_list->length());

  // packages defined to java.base
  if (log_is_enabled(Trace, module)) {
    for (int x = 0; x < pkg_list->length(); x++) {
      log_trace(module)("define_javabase_module(): creation of package %s for module " JAVA_BASE_NAME,
                        (pkg_list->at(x))->as_C_string());
    }
  }
}

// Caller needs ResourceMark.
void throw_dup_pkg_exception(const char* module_name, PackageEntry* package, TRAPS) {
  const char* package_name = package->name()->as_C_string();
  if (package->module()->is_named()) {
    THROW_MSG(vmSymbols::java_lang_IllegalStateException(),
      err_msg("Package %s for module %s is already in another module, %s, defined to the class loader",
              package_name, module_name, package->module()->name()->as_C_string()));
  } else {
    THROW_MSG(vmSymbols::java_lang_IllegalStateException(),
      err_msg("Package %s for module %s is already in the unnamed module defined to the class loader",
              package_name, module_name));
  }
}

void Modules::define_module(Handle module, jboolean is_open, jstring version,
                            jstring location, jobjectArray packages, TRAPS) {
  check_cds_restrictions(CHECK);
  ResourceMark rm(THREAD);

  if (module.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Null module object");
  }

  if (!java_lang_Module::is_instance(module())) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "module is not an instance of type java.lang.Module");
  }

  int module_name_len;
  char* module_name = get_module_name(module(), module_name_len, CHECK);
  if (module_name == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Module name cannot be null");
  }

  // Resolve packages
  objArrayHandle packages_h(THREAD, objArrayOop(JNIHandles::resolve(packages)));
  int num_packages = (packages_h.is_null() ? 0 : packages_h->length());

  // Special handling of java.base definition
  if (strcmp(module_name, JAVA_BASE_NAME) == 0) {
    assert(is_open == JNI_FALSE, "java.base module cannot be open");
    define_javabase_module(module, version, location, packages_h, num_packages, CHECK);
    return;
  }

  oop loader = java_lang_Module::loader(module());
  // Make sure loader is not the jdk.internal.reflect.DelegatingClassLoader.
  if (loader != java_lang_ClassLoader::non_reflection_class_loader(loader)) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Class loader is an invalid delegating class loader");
  }
  Handle h_loader = Handle(THREAD, loader);
  // define_module can be called during start-up, before the class loader's ClassLoaderData
  // has been created.  SystemDictionary::register_loader ensures creation, if needed.
  ClassLoaderData* loader_data = SystemDictionary::register_loader(h_loader);
  assert(loader_data != NULL, "class loader data shouldn't be null");

  // Only modules defined to either the boot or platform class loader, can define a "java/" package.
  bool java_pkg_disallowed = !h_loader.is_null() &&
        !SystemDictionary::is_platform_class_loader(h_loader());

  // Check that the list of packages has no duplicates and that the
  // packages are syntactically ok.
  char buf[128];
  GrowableArray<Symbol*>* pkg_list = new GrowableArray<Symbol*>(num_packages);
  for (int x = 0; x < num_packages; x++) {
    oop pkg_str = packages_h->obj_at(x);
    if (pkg_str == NULL || pkg_str->klass() != vmClasses::String_klass()) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                err_msg("Bad package name"));
    }

    int package_len;
    const char* package_name = as_internal_package(pkg_str, buf, sizeof(buf), package_len);
    if (!verify_package_name(package_name, package_len)) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                err_msg("Invalid package name: %s for module: %s",
                        package_name, module_name));
    }

    // Only modules defined to either the boot or platform class loader, can define a "java/" package.
    if (java_pkg_disallowed &&
        (strncmp(package_name, JAVAPKG, JAVAPKG_LEN) == 0 &&
          (package_name[JAVAPKG_LEN] == JVM_SIGNATURE_SLASH || package_name[JAVAPKG_LEN] == '\0'))) {
      const char* class_loader_name = loader_data->loader_name_and_id();
      size_t pkg_len = strlen(package_name);
      char* pkg_name = NEW_RESOURCE_ARRAY_IN_THREAD(THREAD, char, pkg_len + 1);
      strncpy(pkg_name, package_name, pkg_len + 1);
      StringUtils::replace_no_expand(pkg_name, "/", ".");
      const char* msg_text1 = "Class loader (instance of): ";
      const char* msg_text2 = " tried to define prohibited package name: ";
      size_t len = strlen(msg_text1) + strlen(class_loader_name) + strlen(msg_text2) + pkg_len + 1;
      char* message = NEW_RESOURCE_ARRAY_IN_THREAD(THREAD, char, len);
      jio_snprintf(message, len, "%s%s%s%s", msg_text1, class_loader_name, msg_text2, pkg_name);
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), message);
    }

    Symbol* pkg_symbol = SymbolTable::new_symbol(package_name, package_len);
    pkg_list->append(pkg_symbol);
  }

  ModuleEntryTable* module_table = get_module_entry_table(h_loader);
  assert(module_table != NULL, "module entry table shouldn't be null");

  // Create symbol* entry for module name.
  TempNewSymbol module_symbol = SymbolTable::new_symbol(module_name, module_name_len);

  bool dupl_modules = false;

  // Create symbol for module version.
  TempNewSymbol version_symbol = as_symbol(version);

  // Create symbol* entry for module location.
  TempNewSymbol location_symbol = as_symbol(location);

  PackageEntryTable* package_table = NULL;
  PackageEntry* existing_pkg = NULL;
  {
    MutexLocker ml(THREAD, Module_lock);

    if (num_packages > 0) {
      package_table = get_package_entry_table(h_loader);
      assert(package_table != NULL, "Missing package_table");

      // Check that none of the packages exist in the class loader's package table.
      for (int x = 0; x < pkg_list->length(); x++) {
        existing_pkg = package_table->locked_lookup_only(pkg_list->at(x));
        if (existing_pkg != NULL) {
          // This could be because the module was already defined.  If so,
          // report that error instead of the package error.
          if (module_table->lookup_only(module_symbol) != NULL) {
            dupl_modules = true;
          }
          break;
        }
      }
    }  // if (num_packages > 0)...

    // Add the module and its packages.
    if (!dupl_modules && existing_pkg == NULL) {
      if (module_table->lookup_only(module_symbol) == NULL) {
        // Create the entry for this module in the class loader's module entry table.
        ModuleEntry* module_entry = module_table->locked_create_entry(module,
                                    (is_open == JNI_TRUE), module_symbol,
                                    version_symbol, location_symbol, loader_data);
        assert(module_entry != NULL, "module_entry creation failed");

        // Add the packages.
        assert(pkg_list->length() == 0 || package_table != NULL, "Bad package table");
        for (int y = 0; y < pkg_list->length(); y++) {
          package_table->locked_create_entry(pkg_list->at(y), module_entry);

          // Unable to have a GrowableArray of TempNewSymbol.  Must decrement the refcount of
          // the Symbol* that was created above for each package. The refcount was incremented
          // by SymbolTable::new_symbol and as well by the PackageEntry creation.
          pkg_list->at(y)->decrement_refcount();
        }

        // Store pointer to ModuleEntry record in java.lang.Module object.
        java_lang_Module::set_module_entry(module(), module_entry);
      } else {
         dupl_modules = true;
      }
    }
  }  // Release the lock

  // any errors ?
  if (dupl_modules) {
     THROW_MSG(vmSymbols::java_lang_IllegalStateException(),
               err_msg("Module %s is already defined", module_name));
  } else if (existing_pkg != NULL) {
      throw_dup_pkg_exception(module_name, existing_pkg, CHECK);
  }

  log_info(module, load)("%s location: %s", module_name,
                         location_symbol != NULL ? location_symbol->as_C_string() : "NULL");
  LogTarget(Debug, module) lt;
  if (lt.is_enabled()) {
    LogStream ls(lt);
    ls.print("define_module(): creation of module: %s, version: %s, location: %s, ",
                 module_name, version_symbol != NULL ? version_symbol->as_C_string() : "NULL",
                 location_symbol != NULL ? location_symbol->as_C_string() : "NULL");
    loader_data->print_value_on(&ls);
    ls.print_cr(", package #: %d", pkg_list->length());
    for (int y = 0; y < pkg_list->length(); y++) {
      log_trace(module)("define_module(): creation of package %s for module %s",
                        (pkg_list->at(y))->as_C_string(), module_name);
    }
  }

  // If the module is defined to the boot loader and an exploded build is being
  // used, prepend <java.home>/modules/modules_name to the system boot class path.
  if (h_loader.is_null() && !ClassLoader::has_jrt_entry()) {
    ClassLoader::add_to_exploded_build_list(THREAD, module_symbol);
  }

#ifdef COMPILER2
  // Special handling of jdk.incubator.vector
  if (strcmp(module_name, "jdk.incubator.vector") == 0) {
    if (FLAG_IS_DEFAULT(EnableVectorSupport)) {
      FLAG_SET_DEFAULT(EnableVectorSupport, true);
    }
    if (EnableVectorSupport && FLAG_IS_DEFAULT(EnableVectorReboxing)) {
      FLAG_SET_DEFAULT(EnableVectorReboxing, true);
    }
    if (EnableVectorSupport && EnableVectorReboxing && FLAG_IS_DEFAULT(EnableVectorAggressiveReboxing)) {
      FLAG_SET_DEFAULT(EnableVectorAggressiveReboxing, true);
    }
    if (EnableVectorSupport && FLAG_IS_DEFAULT(UseVectorStubs)) {
      FLAG_SET_DEFAULT(UseVectorStubs, true);
    }
    log_info(compilation)("EnableVectorSupport=%s",            (EnableVectorSupport            ? "true" : "false"));
    log_info(compilation)("EnableVectorReboxing=%s",           (EnableVectorReboxing           ? "true" : "false"));
    log_info(compilation)("EnableVectorAggressiveReboxing=%s", (EnableVectorAggressiveReboxing ? "true" : "false"));
    log_info(compilation)("UseVectorStubs=%s",                 (UseVectorStubs                 ? "true" : "false"));
  }
#endif // COMPILER2
}

#if INCLUDE_CDS_JAVA_HEAP
void Modules::define_archived_modules(Handle h_platform_loader, Handle h_system_loader, TRAPS) {
  assert(UseSharedSpaces && MetaspaceShared::use_full_module_graph(), "must be");

  // We don't want the classes used by the archived full module graph to be redefined by JVMTI.
  // Luckily, such classes are loaded in the JVMTI "early" phase, and CDS is disabled if a JVMTI
  // agent wants to redefine classes in this phase.
  JVMTI_ONLY(assert(JvmtiExport::is_early_phase(), "must be"));
  assert(!(JvmtiExport::should_post_class_file_load_hook() && JvmtiExport::has_early_class_hook_env()),
         "CDS should be disabled if early class hooks are enabled");

  Handle java_base_module(THREAD, ClassLoaderDataShared::restore_archived_oops_for_null_class_loader_data());
  // Patch any previously loaded class's module field with java.base's java.lang.Module.
  ModuleEntryTable::patch_javabase_entries(java_base_module);

  if (h_platform_loader.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Null platform loader object");
  }

  if (h_system_loader.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Null system loader object");
  }

  ClassLoaderData* platform_loader_data = SystemDictionary::register_loader(h_platform_loader);
  ClassLoaderDataShared::restore_java_platform_loader_from_archive(platform_loader_data);

  ClassLoaderData* system_loader_data = SystemDictionary::register_loader(h_system_loader);
  ClassLoaderDataShared::restore_java_system_loader_from_archive(system_loader_data);
}

void Modules::check_cds_restrictions(TRAPS) {
  if (DumpSharedSpaces && Universe::is_module_initialized() && MetaspaceShared::use_full_module_graph()) {
    THROW_MSG(vmSymbols::java_lang_UnsupportedOperationException(),
              "During -Xshare:dump, module system cannot be modified after it's initialized");
  }
}
#endif // INCLUDE_CDS_JAVA_HEAP

void Modules::set_bootloader_unnamed_module(Handle module, TRAPS) {
  ResourceMark rm(THREAD);

  if (module.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Null module object");
  }
  if (!java_lang_Module::is_instance(module())) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "module is not an instance of type java.lang.Module");
  }

  // Ensure that this is an unnamed module
  oop name = java_lang_Module::name(module());
  if (name != NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "boot loader's unnamed module's java.lang.Module has a name");
  }

  // Validate java_base's loader is the boot loader.
  oop loader = java_lang_Module::loader(module());
  if (loader != NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Class loader must be the boot class loader");
  }

  log_debug(module)("set_bootloader_unnamed_module(): recording unnamed module for boot loader");

  // Set java.lang.Module for the boot loader's unnamed module
  ClassLoaderData* boot_loader_data = ClassLoaderData::the_null_class_loader_data();
  ModuleEntry* unnamed_module = boot_loader_data->unnamed_module();
  assert(unnamed_module != NULL, "boot loader's unnamed ModuleEntry not defined");
  unnamed_module->set_module(boot_loader_data->add_handle(module));
  // Store pointer to the ModuleEntry in the unnamed module's java.lang.Module object.
  java_lang_Module::set_module_entry(module(), unnamed_module);
}

void Modules::add_module_exports(Handle from_module, jstring package_name, Handle to_module, TRAPS) {
  check_cds_restrictions(CHECK);

  if (package_name == NULL) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "package is null");
  }
  if (from_module.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "from_module is null");
  }
  ModuleEntry* from_module_entry = get_module_entry(from_module, CHECK);
  if (from_module_entry == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "from_module cannot be found");
  }

  // All packages in unnamed and open modules are exported by default.
  if (!from_module_entry->is_named() || from_module_entry->is_open()) return;

  ModuleEntry* to_module_entry;
  if (to_module.is_null()) {
    to_module_entry = NULL;  // It's an unqualified export.
  } else {
    to_module_entry = get_module_entry(to_module, CHECK);
    if (to_module_entry == NULL) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                "to_module is invalid");
    }
  }

  PackageEntry* package_entry = NULL;
  char buf[128];
  int package_len;

  ResourceMark rm(THREAD);
  const char* pkg = as_internal_package(JNIHandles::resolve_non_null(package_name), buf, sizeof(buf), package_len);
  {
    MutexLocker ml(THREAD, Module_lock);
    package_entry = get_locked_package_entry(from_module_entry, pkg, package_len);
    // Do nothing if modules are the same
    // If the package is not found we'll throw an exception later
    if (from_module_entry != to_module_entry &&
        package_entry != NULL) {
      package_entry->set_exported(to_module_entry);
    }
  }

  // Handle errors and logging outside locked section
  if (package_entry == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              err_msg("Package %s not found in from_module %s",
                      pkg != NULL ? pkg : "",
                      from_module_entry->name()->as_C_string()));
  }

  if (log_is_enabled(Debug, module)) {
    log_debug(module)("add_module_exports(): package %s in module %s is exported to module %s",
                      package_entry->name()->as_C_string(),
                      from_module_entry->name()->as_C_string(),
                      to_module_entry == NULL ? "NULL" :
                      to_module_entry->is_named() ?
                      to_module_entry->name()->as_C_string() : UNNAMED_MODULE);
  }
}


void Modules::add_module_exports_qualified(Handle from_module, jstring package,
                                           Handle to_module, TRAPS) {
  check_cds_restrictions(CHECK);
  if (to_module.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "to_module is null");
  }
  add_module_exports(from_module, package, to_module, CHECK);
}

void Modules::add_reads_module(Handle from_module, Handle to_module, TRAPS) {
  check_cds_restrictions(CHECK);
  if (from_module.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "from_module is null");
  }

  ModuleEntry* from_module_entry = get_module_entry(from_module, CHECK);
  if (from_module_entry == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "from_module is not valid");
  }

  ModuleEntry* to_module_entry;
  if (!to_module.is_null()) {
    to_module_entry = get_module_entry(to_module, CHECK);
    if (to_module_entry == NULL) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                "to_module is invalid");
    }
  } else {
    to_module_entry = NULL;
  }

  ResourceMark rm(THREAD);
  log_debug(module)("add_reads_module(): Adding read from module %s to module %s",
                    from_module_entry->is_named() ?
                    from_module_entry->name()->as_C_string() : UNNAMED_MODULE,
                    to_module_entry == NULL ? "all unnamed" :
                      (to_module_entry->is_named() ?
                       to_module_entry->name()->as_C_string() : UNNAMED_MODULE));

  // if modules are the same or if from_module is unnamed then no need to add the read.
  if (from_module_entry != to_module_entry && from_module_entry->is_named()) {
    from_module_entry->add_read(to_module_entry);
  }
}

// This method is called by JFR and JNI.
jobject Modules::get_module(jclass clazz, TRAPS) {
  assert(ModuleEntryTable::javabase_defined(),
         "Attempt to call get_module before " JAVA_BASE_NAME " is defined");

  if (clazz == NULL) {
    THROW_MSG_(vmSymbols::java_lang_NullPointerException(),
               "class is null", JNI_FALSE);
  }
  oop mirror = JNIHandles::resolve_non_null(clazz);
  if (mirror == NULL) {
    log_debug(module)("get_module(): no mirror, returning NULL");
    return NULL;
  }
  if (!java_lang_Class::is_instance(mirror)) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Invalid class", JNI_FALSE);
  }

  oop module = java_lang_Class::module(mirror);

  assert(module != NULL, "java.lang.Class module field not set");
  assert(java_lang_Module::is_instance(module), "module is not an instance of type java.lang.Module");

  LogTarget(Debug,module) lt;
  if (lt.is_enabled()) {
    ResourceMark rm(THREAD);
    LogStream ls(lt);
    Klass* klass = java_lang_Class::as_Klass(mirror);
    oop module_name = java_lang_Module::name(module);
    if (module_name != NULL) {
      ls.print("get_module(): module ");
      java_lang_String::print(module_name, tty);
    } else {
      ls.print("get_module(): Unnamed Module");
    }
    if (klass != NULL) {
      ls.print_cr(" for class %s", klass->external_name());
    } else {
      ls.print_cr(" for primitive class");
    }
  }

  return JNIHandles::make_local(THREAD, module);
}

oop Modules::get_named_module(Handle h_loader, const char* package_name) {
  assert(ModuleEntryTable::javabase_defined(),
         "Attempt to call get_named_module before " JAVA_BASE_NAME " is defined");
  assert(h_loader.is_null() || java_lang_ClassLoader::is_subclass(h_loader->klass()),
         "Class loader is not a subclass of java.lang.ClassLoader");
  assert(package_name != NULL, "the package_name should not be NULL");

  if (strlen(package_name) == 0) {
    return NULL;
  }
  TempNewSymbol package_sym = SymbolTable::new_symbol(package_name);
  const PackageEntry* const pkg_entry =
    get_package_entry_by_name(package_sym, h_loader);
  const ModuleEntry* const module_entry = (pkg_entry != NULL ? pkg_entry->module() : NULL);

  if (module_entry != NULL && module_entry->module() != NULL && module_entry->is_named()) {
    return module_entry->module();
  }
  return NULL;
}

// Export package in module to all unnamed modules.
void Modules::add_module_exports_to_all_unnamed(Handle module, jstring package_name, TRAPS) {
  check_cds_restrictions(CHECK);
  if (module.is_null()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "module is null");
  }
  if (package_name == NULL) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "package is null");
  }
  ModuleEntry* module_entry = get_module_entry(module, CHECK);
  if (module_entry == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "module is invalid");
  }

  // No-op for unnamed module and open modules
  if (!module_entry->is_named() || module_entry->is_open())
    return;

  ResourceMark rm(THREAD);
  char buf[128];
  int pkg_len;
  const char* pkg = as_internal_package(JNIHandles::resolve_non_null(package_name), buf, sizeof(buf), pkg_len);
  PackageEntry* package_entry = NULL;
  {
    MutexLocker m1(THREAD, Module_lock);
    package_entry = get_locked_package_entry(module_entry, pkg, pkg_len);

    // Mark package as exported to all unnamed modules.
    if (package_entry != NULL) {
      package_entry->set_is_exported_allUnnamed();
    }
  }

  // Handle errors and logging outside locked section
  if (package_entry == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              err_msg("Package %s not found in module %s",
                      pkg != NULL ? pkg : "",
                      module_entry->name()->as_C_string()));
  }

  if (log_is_enabled(Debug, module)) {
    log_debug(module)("add_module_exports_to_all_unnamed(): package %s in module"
                      " %s is exported to all unnamed modules",
                       package_entry->name()->as_C_string(),
                       module_entry->name()->as_C_string());
  }
}
