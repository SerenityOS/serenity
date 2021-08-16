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

#ifndef SHARE_CLASSFILE_MODULES_HPP
#define SHARE_CLASSFILE_MODULES_HPP

#include "memory/allocation.hpp"
#include "runtime/handles.hpp"

class ModuleEntryTable;
class Symbol;

class Modules : AllStatic {
  static void check_cds_restrictions(TRAPS) NOT_CDS_JAVA_HEAP_RETURN;

public:
  // define_module defines a module containing the specified packages. It binds the
  // module to its class loader by creating the ModuleEntry record in the
  // ClassLoader's ModuleEntry table, and creates PackageEntry records in the class
  // loader's PackageEntry table.  The jstring for all package names will convert "."
  // to "/"
  //
  //  IllegalArgumentExceptions are thrown for the following :
  // * Module's Class loader is not a subclass of java.lang.ClassLoader
  // * Module's Class loader already has a module with that name
  // * Module's Class loader has already defined types for any of the module's packages
  // * Module_name is syntactically bad
  // * Packages contains an illegal package name or a non-String object
  // * A package already exists in another module for this class loader
  // * Module is an unnamed module
  //  NullPointerExceptions are thrown if module is null.
  static void define_module(Handle module, jboolean is_open, jstring version,
                            jstring location, jobjectArray packages, TRAPS);

  static void define_archived_modules(Handle h_platform_loader, Handle h_system_loader,
                                      TRAPS) NOT_CDS_JAVA_HEAP_RETURN;

  // Provides the java.lang.Module for the unnamed module defined
  // to the boot loader.
  //
  //  IllegalArgumentExceptions are thrown for the following :
  //  * Module has a name
  //  * Module is not a subclass of java.lang.Module
  //  * Module's class loader is not the boot loader
  //  NullPointerExceptions are thrown if module is null.
  static void set_bootloader_unnamed_module(Handle module, TRAPS);

  // This either does a qualified export of package in module from_module to module
  // to_module or, if to_module is null, does an unqualified export of package.
  // Any "." in the package name will be converted to "/"
  //
  // Error conditions causing IlegalArgumentException to be throw :
  // * Module from_module does not exist
  // * Module to_module is not null and does not exist
  // * Package is not syntactically correct
  // * Package is not defined for from_module's class loader
  // * Package is not in module from_module.
  static void add_module_exports(Handle from_module, jstring package, Handle to_module, TRAPS);

  // This does a qualified export of package in module from_module to module
  // to_module.  Any "." in the package name will be converted to "/"
  //
  // Error conditions causing IlegalArgumentException to be throw :
  // * Module from_module does not exist
  // * Module to_module does not exist
  // * Package is not syntactically correct
  // * Package is not defined for from_module's class loader
  // * Package is not in module from_module.
  static void add_module_exports_qualified(Handle from_module, jstring package, Handle to_module, TRAPS);

  // add_reads_module adds module to_module to the list of modules that from_module
  // can read.  If from_module is the same as to_module then this is a no-op.
  // If to_module is null then from_module is marked as a loose module (meaning that
  // from_module can read all current and future unnamed  modules).
  // An IllegalArgumentException is thrown if from_module is null or either (non-null)
  // module does not exist.
  static void add_reads_module(Handle from_module, Handle to_module, TRAPS);

  // Return the java.lang.Module object for this class object.
  static jobject get_module(jclass clazz, TRAPS);

  // Return the java.lang.Module object for this class loader and package.
  // Returns NULL if the package name is empty, if the resulting package
  // entry is NULL, if the module is not found or is unnamed.
  // The package should contain /'s, not .'s, as in java/lang, not java.lang.
  static oop get_named_module(Handle h_loader, const char* package);

  // Marks the specified package as exported to all unnamed modules.
  // If either module or package is null then NullPointerException is thrown.
  // If module or package is bad, or module is unnamed, or package is not in
  // module then IllegalArgumentException is thrown.
  static void add_module_exports_to_all_unnamed(Handle module, jstring package, TRAPS);

  // Return TRUE iff package is defined by loader
  static bool is_package_defined(Symbol* package_name, Handle h_loader);
  static ModuleEntryTable* get_module_entry_table(Handle h_loader);
};

#endif // SHARE_CLASSFILE_MODULES_HPP
