/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSLOADEREXT_HPP
#define SHARE_CLASSFILE_CLASSLOADEREXT_HPP

#include "classfile/classLoader.hpp"
#include "classfile/moduleEntry.hpp"
#include "utilities/macros.hpp"

class ClassListParser;

class ClassLoaderExt: public ClassLoader { // AllStatic
public:
  static bool should_verify(int classpath_index) {
    CDS_ONLY(return (classpath_index >= _app_class_paths_start_index);)
    NOT_CDS(return false;)
  }

#if INCLUDE_CDS
private:
  enum SomeConstants {
    max_classpath_index = 0x7fff
  };

  static char* get_class_path_attr(const char* jar_path, char* manifest, jint manifest_size);
  static void setup_app_search_path(JavaThread* current); // Only when -Xshare:dump
  static void process_module_table(JavaThread* current, ModuleEntryTable* met);
  // index of first app JAR in shared classpath entry table
  static jshort _app_class_paths_start_index;
  // index of first modular JAR in shared modulepath entry table
  static jshort _app_module_paths_start_index;
  // the largest path index being used during CDS dump time
  static jshort _max_used_path_index;

  static bool _has_app_classes;
  static bool _has_platform_classes;
  static bool _has_non_jar_in_classpath;

  static char* read_manifest(JavaThread* current, ClassPathEntry* entry, jint *manifest_size, bool clean_text);
  static ClassPathEntry* find_classpath_entry_from_cache(JavaThread* current, const char* path);

public:
  static void process_jar_manifest(JavaThread* current, ClassPathEntry* entry, bool check_for_duplicates);

  // Called by JVMTI code to add boot classpath
  static void append_boot_classpath(ClassPathEntry* new_entry);

  static void setup_search_paths(JavaThread* current);
  static void setup_module_paths(JavaThread* current);

  static char* read_manifest(JavaThread* current, ClassPathEntry* entry, jint *manifest_size) {
    // Remove all the new-line continuations (which wrap long lines at 72 characters, see
    // http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#JAR%20Manifest), so
    // that the manifest is easier to parse.
    return read_manifest(current, entry, manifest_size, true);
  }
  static char* read_raw_manifest(JavaThread* current, ClassPathEntry* entry, jint *manifest_size) {
    // Do not remove new-line continuations, so we can easily pass it as an argument to
    // java.util.jar.Manifest.getManifest() at run-time.
    return read_manifest(current, entry, manifest_size, false);
  }

  static jshort app_class_paths_start_index() { return _app_class_paths_start_index; }

  static jshort app_module_paths_start_index() { return _app_module_paths_start_index; }

  static jshort max_used_path_index() { return _max_used_path_index; }

  static void set_max_used_path_index(jshort used_index) {
    _max_used_path_index = used_index;
  }

  static void init_paths_start_index(jshort app_start) {
    _app_class_paths_start_index = app_start;
  }

  static void init_app_module_paths_start_index(jshort module_start) {
    _app_module_paths_start_index = module_start;
  }

  static bool is_boot_classpath(int classpath_index) {
    return classpath_index < _app_class_paths_start_index;
  }

  static bool has_platform_or_app_classes() {
    return _has_app_classes || _has_platform_classes;
  }

  static bool has_non_jar_in_classpath() {
    return _has_non_jar_in_classpath;
  }

  static void record_result(const s2 classpath_index, InstanceKlass* result);
  static InstanceKlass* load_class(Symbol* h_name, const char* path, TRAPS);
  static void set_has_app_classes() {
    _has_app_classes = true;
  }
  static void set_has_platform_classes() {
    _has_platform_classes = true;
  }
  static void set_has_non_jar_in_classpath() {
    _has_non_jar_in_classpath = true;
  }
#endif // INCLUDE_CDS
};

#endif // SHARE_CLASSFILE_CLASSLOADEREXT_HPP
