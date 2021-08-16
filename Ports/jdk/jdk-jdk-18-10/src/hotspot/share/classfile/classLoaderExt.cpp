/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/filemap.hpp"
#include "classfile/classFileParser.hpp"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.inline.hpp"
#include "classfile/classLoaderExt.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoadInfo.hpp"
#include "classfile/klassFactory.hpp"
#include "classfile/modules.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "runtime/arguments.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/os.hpp"
#include "services/threadService.hpp"
#include "utilities/stringUtils.hpp"

jshort ClassLoaderExt::_app_class_paths_start_index = ClassLoaderExt::max_classpath_index;
jshort ClassLoaderExt::_app_module_paths_start_index = ClassLoaderExt::max_classpath_index;
jshort ClassLoaderExt::_max_used_path_index = 0;
bool ClassLoaderExt::_has_app_classes = false;
bool ClassLoaderExt::_has_platform_classes = false;
bool ClassLoaderExt::_has_non_jar_in_classpath = false;

void ClassLoaderExt::append_boot_classpath(ClassPathEntry* new_entry) {
  if (UseSharedSpaces) {
    warning("Sharing is only supported for boot loader classes because bootstrap classpath has been appended");
    FileMapInfo::current_info()->set_has_platform_or_app_classes(false);
  }
  ClassLoader::add_to_boot_append_entries(new_entry);
}

void ClassLoaderExt::setup_app_search_path(JavaThread* current) {
  Arguments::assert_is_dumping_archive();
  _app_class_paths_start_index = ClassLoader::num_boot_classpath_entries();
  char* app_class_path = os::strdup(Arguments::get_appclasspath());

  if (strcmp(app_class_path, ".") == 0) {
    // This doesn't make any sense, even for AppCDS, so let's skip it. We
    // don't want to throw an error here because -cp "." is usually assigned
    // by the launcher when classpath is not specified.
    trace_class_path("app loader class path (skipped)=", app_class_path);
  } else {
    trace_class_path("app loader class path=", app_class_path);
    ClassLoader::setup_app_search_path(current, app_class_path);
  }
}

void ClassLoaderExt::process_module_table(JavaThread* current, ModuleEntryTable* met) {
  ResourceMark rm(current);
  for (int i = 0; i < met->table_size(); i++) {
    for (ModuleEntry* m = met->bucket(i); m != NULL;) {
      char* path = m->location()->as_C_string();
      if (strncmp(path, "file:", 5) == 0) {
        path = ClassLoader::skip_uri_protocol(path);
        ClassLoader::setup_module_search_path(current, path);
      }
      m = m->next();
    }
  }
}
void ClassLoaderExt::setup_module_paths(JavaThread* current) {
  Arguments::assert_is_dumping_archive();
  _app_module_paths_start_index = ClassLoader::num_boot_classpath_entries() +
                              ClassLoader::num_app_classpath_entries();
  Handle system_class_loader (current, SystemDictionary::java_system_loader());
  ModuleEntryTable* met = Modules::get_module_entry_table(system_class_loader);
  process_module_table(current, met);
}

char* ClassLoaderExt::read_manifest(JavaThread* current, ClassPathEntry* entry,
                                    jint *manifest_size, bool clean_text) {
  const char* name = "META-INF/MANIFEST.MF";
  char* manifest;
  jint size;

  assert(entry->is_jar_file(), "must be");
  manifest = (char*) ((ClassPathZipEntry*)entry )->open_entry(current, name, &size, true);

  if (manifest == NULL) { // No Manifest
    *manifest_size = 0;
    return NULL;
  }


  if (clean_text) {
    // See http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#JAR%20Manifest
    // (1): replace all CR/LF and CR with LF
    StringUtils::replace_no_expand(manifest, "\r\n", "\n");

    // (2) remove all new-line continuation (remove all "\n " substrings)
    StringUtils::replace_no_expand(manifest, "\n ", "");
  }

  *manifest_size = (jint)strlen(manifest);
  return manifest;
}

char* ClassLoaderExt::get_class_path_attr(const char* jar_path, char* manifest, jint manifest_size) {
  const char* tag = "Class-Path: ";
  const int tag_len = (int)strlen(tag);
  char* found = NULL;
  char* line_start = manifest;
  char* end = manifest + manifest_size;

  assert(*end == 0, "must be nul-terminated");

  while (line_start < end) {
    char* line_end = strchr(line_start, '\n');
    if (line_end == NULL) {
      // JAR spec require the manifest file to be terminated by a new line.
      break;
    }
    if (strncmp(tag, line_start, tag_len) == 0) {
      if (found != NULL) {
        // Same behavior as jdk/src/share/classes/java/util/jar/Attributes.java
        // If duplicated entries are found, the last one is used.
        log_warning(cds)("Warning: Duplicate name in Manifest: %s.\n"
                      "Ensure that the manifest does not have duplicate entries, and\n"
                      "that blank lines separate individual sections in both your\n"
                      "manifest and in the META-INF/MANIFEST.MF entry in the jar file:\n%s\n", tag, jar_path);
      }
      found = line_start + tag_len;
      assert(found <= line_end, "sanity");
      *line_end = '\0';
    }
    line_start = line_end + 1;
  }
  return found;
}

void ClassLoaderExt::process_jar_manifest(JavaThread* current, ClassPathEntry* entry,
                                          bool check_for_duplicates) {
  ResourceMark rm(current);
  jint manifest_size;
  char* manifest = read_manifest(current, entry, &manifest_size);

  if (manifest == NULL) {
    return;
  }

  if (strstr(manifest, "Extension-List:") != NULL) {
    vm_exit_during_cds_dumping(err_msg("-Xshare:dump does not support Extension-List in JAR manifest: %s", entry->name()));
  }

  char* cp_attr = get_class_path_attr(entry->name(), manifest, manifest_size);

  if (cp_attr != NULL && strlen(cp_attr) > 0) {
    trace_class_path("found Class-Path: ", cp_attr);

    char sep = os::file_separator()[0];
    const char* dir_name = entry->name();
    const char* dir_tail = strrchr(dir_name, sep);
    int dir_len;
    if (dir_tail == NULL) {
      dir_len = 0;
    } else {
      dir_len = dir_tail - dir_name + 1;
    }

    // Split the cp_attr by spaces, and add each file
    char* file_start = cp_attr;
    char* end = file_start + strlen(file_start);

    while (file_start < end) {
      char* file_end = strchr(file_start, ' ');
      if (file_end != NULL) {
        *file_end = 0;
        file_end += 1;
      } else {
        file_end = end;
      }

      size_t name_len = strlen(file_start);
      if (name_len > 0) {
        ResourceMark rm(current);
        size_t libname_len = dir_len + name_len;
        char* libname = NEW_RESOURCE_ARRAY(char, libname_len + 1);
        int n = os::snprintf(libname, libname_len + 1, "%.*s%s", dir_len, dir_name, file_start);
        assert((size_t)n == libname_len, "Unexpected number of characters in string");
        if (ClassLoader::update_class_path_entry_list(current, libname, true, false, true /* from_class_path_attr */)) {
          trace_class_path("library = ", libname);
        } else {
          trace_class_path("library (non-existent) = ", libname);
          FileMapInfo::record_non_existent_class_path_entry(libname);
        }
      }

      file_start = file_end;
    }
  }
}

void ClassLoaderExt::setup_search_paths(JavaThread* current) {
  ClassLoaderExt::setup_app_search_path(current);
}

void ClassLoaderExt::record_result(const s2 classpath_index, InstanceKlass* result) {
  Arguments::assert_is_dumping_archive();

  // We need to remember where the class comes from during dumping.
  oop loader = result->class_loader();
  s2 classloader_type = ClassLoader::BOOT_LOADER;
  if (SystemDictionary::is_system_class_loader(loader)) {
    classloader_type = ClassLoader::APP_LOADER;
    ClassLoaderExt::set_has_app_classes();
  } else if (SystemDictionary::is_platform_class_loader(loader)) {
    classloader_type = ClassLoader::PLATFORM_LOADER;
    ClassLoaderExt::set_has_platform_classes();
  }
  if (classpath_index > ClassLoaderExt::max_used_path_index()) {
    ClassLoaderExt::set_max_used_path_index(classpath_index);
  }
  result->set_shared_classpath_index(classpath_index);
  result->set_shared_class_loader_type(classloader_type);
}

// Load the class of the given name from the location given by path. The path is specified by
// the "source:" in the class list file (see classListParser.cpp), and can be a directory or
// a JAR file.
InstanceKlass* ClassLoaderExt::load_class(Symbol* name, const char* path, TRAPS) {
  assert(name != NULL, "invariant");
  assert(DumpSharedSpaces, "this function is only used with -Xshare:dump");
  ResourceMark rm(THREAD);
  const char* class_name = name->as_C_string();
  const char* file_name = file_name_for_class_name(class_name,
                                                   name->utf8_length());
  assert(file_name != NULL, "invariant");

  // Lookup stream for parsing .class file
  ClassFileStream* stream = NULL;
  ClassPathEntry* e = find_classpath_entry_from_cache(THREAD, path);
  if (e == NULL) {
    THROW_NULL(vmSymbols::java_lang_ClassNotFoundException());
  }

  {
    PerfClassTraceTime vmtimer(perf_sys_class_lookup_time(),
                               THREAD->get_thread_stat()->perf_timers_addr(),
                               PerfClassTraceTime::CLASS_LOAD);
    stream = e->open_stream(THREAD, file_name);
  }

  if (stream == NULL) {
    // open_stream could return NULL even when no exception has be thrown (JDK-8263632).
    THROW_NULL(vmSymbols::java_lang_ClassNotFoundException());
    return NULL;
  }
  stream->set_verify(true);

  ClassLoaderData* loader_data = ClassLoaderData::the_null_class_loader_data();
  Handle protection_domain;
  ClassLoadInfo cl_info(protection_domain);
  InstanceKlass* k = KlassFactory::create_from_stream(stream,
                                                      name,
                                                      loader_data,
                                                      cl_info,
                                                      CHECK_NULL);
  return k;
}

struct CachedClassPathEntry {
  const char* _path;
  ClassPathEntry* _entry;
};

static GrowableArray<CachedClassPathEntry>* cached_path_entries = NULL;

ClassPathEntry* ClassLoaderExt::find_classpath_entry_from_cache(JavaThread* current, const char* path) {
  // This is called from dump time so it's single threaded and there's no need for a lock.
  assert(DumpSharedSpaces, "this function is only used with -Xshare:dump");
  if (cached_path_entries == NULL) {
    cached_path_entries = new (ResourceObj::C_HEAP, mtClass) GrowableArray<CachedClassPathEntry>(20, mtClass);
  }
  CachedClassPathEntry ccpe;
  for (int i=0; i<cached_path_entries->length(); i++) {
    ccpe = cached_path_entries->at(i);
    if (strcmp(ccpe._path, path) == 0) {
      if (i != 0) {
        // Put recent entries at the beginning to speed up searches.
        cached_path_entries->remove_at(i);
        cached_path_entries->insert_before(0, ccpe);
      }
      return ccpe._entry;
    }
  }

  struct stat st;
  if (os::stat(path, &st) != 0) {
    // File or directory not found
    return NULL;
  }
  ClassPathEntry* new_entry = NULL;

  new_entry = create_class_path_entry(current, path, &st, false, false);
  if (new_entry == NULL) {
    return NULL;
  }
  ccpe._path = strdup(path);
  ccpe._entry = new_entry;
  cached_path_entries->insert_before(0, ccpe);
  return new_entry;
}
