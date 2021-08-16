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

#ifndef SHARE_CLASSFILE_CLASSLOADERSTATS_HPP
#define SHARE_CLASSFILE_CLASSLOADERSTATS_HPP


#include "classfile/classLoaderData.hpp"
#include "oops/klass.hpp"
#include "oops/oop.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/vmOperation.hpp"
#include "services/diagnosticCommand.hpp"
#include "utilities/resourceHash.hpp"


class ClassLoaderStatsDCmd : public DCmd {
public:
  ClassLoaderStatsDCmd(outputStream* output, bool heap) :
    DCmd(output, heap) {
  }

  static const char* name() {
    return "VM.classloader_stats";
  }

  static const char* description() {
    return "Print statistics about all ClassLoaders.";
  }

  static const char* impact() {
    return "Low";
  }

  virtual void execute(DCmdSource source, TRAPS);

  static int num_arguments() {
    return 0;
  }

  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission",
                        "monitor", NULL};
    return p;
  }
};


class ClassLoaderStats : public ResourceObj {
public:
  ClassLoaderData*  _cld;
  oop               _class_loader;
  oop               _parent;

  size_t            _chunk_sz;
  size_t            _block_sz;
  uintx             _classes_count;

  size_t            _hidden_chunk_sz;
  size_t            _hidden_block_sz;
  uintx             _hidden_classes_count;

  ClassLoaderStats() :
    _cld(0),
    _class_loader(0),
    _parent(0),
    _chunk_sz(0),
    _block_sz(0),
    _classes_count(0),
    _hidden_chunk_sz(0),
    _hidden_block_sz(0),
    _hidden_classes_count(0) {
  }
};


class ClassLoaderStatsClosure : public CLDClosure {
protected:
  static unsigned oop_hash(oop const& s1) {
    // Robert Jenkins 1996 & Thomas Wang 1997
    // http://web.archive.org/web/20071223173210/http://www.concentric.net/~Ttwang/tech/inthash.htm
    uintptr_t tmp = cast_from_oop<uintptr_t>(s1);
    unsigned hash = (unsigned)tmp;
    hash = ~hash + (hash << 15);
    hash = hash ^ (hash >> 12);
    hash = hash + (hash << 2);
    hash = hash ^ (hash >> 4);
    hash = hash * 2057;
    hash = hash ^ (hash >> 16);
    return hash;
  }

  typedef ResourceHashtable<oop, ClassLoaderStats,
                            256, ResourceObj::RESOURCE_AREA, mtInternal,
                            ClassLoaderStatsClosure::oop_hash> StatsTable;

  outputStream* _out;
  StatsTable* _stats;
  uintx   _total_loaders;
  uintx   _total_classes;
  size_t  _total_chunk_sz;
  size_t  _total_block_sz;

public:
  ClassLoaderStatsClosure(outputStream* out) :
    _out(out),
    _stats(new StatsTable()),
    _total_loaders(0),
    _total_classes(0),
    _total_chunk_sz(0),
    _total_block_sz(0) {
  }

  virtual void do_cld(ClassLoaderData* cld);
  virtual bool do_entry(oop const& key, ClassLoaderStats const& cls);
  void print();

private:
  void addEmptyParents(oop cl);
};


class ClassLoaderStatsVMOperation : public VM_Operation {
  outputStream* _out;

public:
  ClassLoaderStatsVMOperation(outputStream* out) :
    _out(out) {
  }

  VMOp_Type type() const {
    return VMOp_ClassLoaderStatsOperation;
  }

  void doit();
};

#endif // SHARE_CLASSFILE_CLASSLOADERSTATS_HPP
