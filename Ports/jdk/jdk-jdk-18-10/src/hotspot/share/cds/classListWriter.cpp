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
#include "cds/classListWriter.hpp"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderData.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.hpp"
#include "runtime/mutexLocker.hpp"

fileStream* ClassListWriter::_classlist_file = NULL;

void ClassListWriter::init() {
  // For -XX:DumpLoadedClassList=<file> option
  if (DumpLoadedClassList != NULL) {
    const char* list_name = make_log_name(DumpLoadedClassList, NULL);
    _classlist_file = new(ResourceObj::C_HEAP, mtInternal)
                         fileStream(list_name);
    _classlist_file->print_cr("# NOTE: Do not modify this file.");
    _classlist_file->print_cr("#");
    _classlist_file->print_cr("# This file is generated via the -XX:DumpLoadedClassList=<class_list_file> option");
    _classlist_file->print_cr("# and is used at CDS archive dump time (see -Xshare:dump).");
    _classlist_file->print_cr("#");
    FREE_C_HEAP_ARRAY(char, list_name);
  }
}

void ClassListWriter::write(const InstanceKlass* k, const ClassFileStream* cfs) {
  assert(is_enabled(), "must be");

  if (!ClassLoader::has_jrt_entry()) {
    warning("DumpLoadedClassList and CDS are not supported in exploded build");
    DumpLoadedClassList = NULL;
    return;
  }

  ClassListWriter w;
  write_to_stream(k, w.stream(), cfs);
}

class ClassListWriter::IDTable : public ResourceHashtable<
  const InstanceKlass*, int,
  15889, // prime number
  ResourceObj::C_HEAP> {};

ClassListWriter::IDTable* ClassListWriter::_id_table = NULL;
int ClassListWriter::_total_ids = 0;

int ClassListWriter::get_id(const InstanceKlass* k) {
  assert_locked();
  if (_id_table == NULL) {
    _id_table = new (ResourceObj::C_HEAP, mtClass)IDTable();
  }
  bool created;
  int* v = _id_table->put_if_absent(k, &created);
  if (created) {
    *v = _total_ids++;
  }
  return *v;
}

bool ClassListWriter::has_id(const InstanceKlass* k) {
  assert_locked();
  if (_id_table != NULL) {
    return _id_table->get(k) != NULL;
  } else {
    return false;
  }
}

void ClassListWriter::handle_class_unloading(const InstanceKlass* klass) {
  assert_locked();
  if (_id_table != NULL) {
    _id_table->remove(klass);
  }
}

void ClassListWriter::write_to_stream(const InstanceKlass* k, outputStream* stream, const ClassFileStream* cfs) {
  assert_locked();
  ClassLoaderData* loader_data = k->class_loader_data();

  if (!SystemDictionaryShared::is_builtin_loader(loader_data)) {
    if (cfs == NULL || strncmp(cfs->source(), "file:", 5) != 0) {
      return;
    }
    if (!SystemDictionaryShared::add_unregistered_class(Thread::current(), (InstanceKlass*)k)) {
      return;
    }
  }


  {
    InstanceKlass* super = k->java_super();
    if (super != NULL && !has_id(super)) {
      return;
    }

    Array<InstanceKlass*>* interfaces = k->local_interfaces();
    int len = interfaces->length();
    for (int i = 0; i < len; i++) {
      InstanceKlass* intf = interfaces->at(i);
      if (!has_id(intf)) {
        return;
      }
    }
  }

  if (k->is_hidden()) {
    return;
  }

  if (k->module()->is_patched()) {
    return;
  }

  ResourceMark rm;
  stream->print("%s id: %d", k->name()->as_C_string(), get_id(k));
  if (!SystemDictionaryShared::is_builtin_loader(loader_data)) {
    InstanceKlass* super = k->java_super();
    assert(super != NULL, "must be");
    stream->print(" super: %d", get_id(super));

    Array<InstanceKlass*>* interfaces = k->local_interfaces();
    int len = interfaces->length();
    if (len > 0) {
      stream->print(" interfaces:");
      for (int i = 0; i < len; i++) {
        InstanceKlass* intf = interfaces->at(i);
        stream->print(" %d", get_id(intf));
      }
    }

#ifdef _WINDOWS
    // "file:/C:/dir/foo.jar" -> "C:/dir/foo.jar"
    stream->print(" source: %s", cfs->source() + 6);
#else
    // "file:/dir/foo.jar" -> "/dir/foo.jar"
    stream->print(" source: %s", cfs->source() + 5);
#endif
  }

  stream->cr();
  stream->flush();
}

void ClassListWriter::delete_classlist() {
  if (_classlist_file != NULL) {
    delete _classlist_file;
  }
}
