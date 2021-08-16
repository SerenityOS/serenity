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

#include "precompiled.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/classLoaderStats.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/globalDefinitions.hpp"


class ClassStatsClosure : public KlassClosure {
public:
  int _num_classes;

  ClassStatsClosure() :
    _num_classes(0) {
  }

  virtual void do_klass(Klass* k) {
    _num_classes++;
  }
};

void ClassLoaderStatsClosure::do_cld(ClassLoaderData* cld) {
  oop cl = cld->class_loader();

  // The hashtable key is the ClassLoader oop since we want to account
  // for "real" classes and hidden classes together
  bool added = false;
  ClassLoaderStats* cls = _stats->put_if_absent(cl, &added);
  if (added) {
    cls->_class_loader = cl;
    _total_loaders++;
  }
  assert(cls->_class_loader == cl, "Sanity");

  if (!cld->has_class_mirror_holder()) {
    cls->_cld = cld;
  }

  if (cl != NULL) {
    cls->_parent = java_lang_ClassLoader::parent(cl);
    addEmptyParents(cls->_parent);
  }

  ClassStatsClosure csc;
  cld->classes_do(&csc);
  bool is_hidden = false;
  if(cld->has_class_mirror_holder()) {
    // If cld has a class holder then it must be hidden.
    // Either way, count it as a hidden class.
    cls->_hidden_classes_count += csc._num_classes;
  } else {
    cls->_classes_count = csc._num_classes;
  }
  _total_classes += csc._num_classes;

  ClassLoaderMetaspace* ms = cld->metaspace_or_null();
  if (ms != NULL) {
    size_t used_bytes, capacity_bytes;
    ms->calculate_jfr_stats(&used_bytes, &capacity_bytes);
    if(cld->has_class_mirror_holder()) {
      cls->_hidden_chunk_sz += capacity_bytes;
      cls->_hidden_block_sz += used_bytes;
    } else {
      cls->_chunk_sz = capacity_bytes;
      cls->_block_sz = used_bytes;
    }
    _total_chunk_sz += capacity_bytes;
    _total_block_sz += used_bytes;
  }
}


// Handles the difference in pointer width on 32 and 64 bit platforms
#ifdef _LP64
  #define SPACE "%8s"
#else
  #define SPACE "%s"
#endif


bool ClassLoaderStatsClosure::do_entry(oop const& key, ClassLoaderStats const& cls) {
  Klass* class_loader_klass = (cls._class_loader == NULL ? NULL : cls._class_loader->klass());
  Klass* parent_klass = (cls._parent == NULL ? NULL : cls._parent->klass());

  _out->print(INTPTR_FORMAT "  " INTPTR_FORMAT "  " INTPTR_FORMAT "  " UINTX_FORMAT_W(6) "  " SIZE_FORMAT_W(8) "  " SIZE_FORMAT_W(8) "  ",
      p2i(class_loader_klass), p2i(parent_klass), p2i(cls._cld),
      cls._classes_count,
      cls._chunk_sz, cls._block_sz);
  if (class_loader_klass != NULL) {
    _out->print("%s", class_loader_klass->external_name());
  } else {
    _out->print("<boot class loader>");
  }
  _out->cr();
  if (cls._hidden_classes_count > 0) {
    _out->print_cr(SPACE SPACE SPACE "                                    " UINTX_FORMAT_W(6) "  " SIZE_FORMAT_W(8) "  " SIZE_FORMAT_W(8) "   + hidden classes",
        "", "", "",
        cls._hidden_classes_count,
        cls._hidden_chunk_sz, cls._hidden_block_sz);
  }
  return true;
}


void ClassLoaderStatsClosure::print() {
  _out->print_cr("ClassLoader" SPACE " Parent" SPACE "      CLD*" SPACE "       Classes   ChunkSz   BlockSz  Type", "", "", "");
  _stats->iterate(this);
  _out->print("Total = " UINTX_FORMAT_W(-6), _total_loaders);
  _out->print(SPACE SPACE SPACE "                      ", "", "", "");
  _out->print_cr(UINTX_FORMAT_W(6) "  " SIZE_FORMAT_W(8) "  " SIZE_FORMAT_W(8) "  ",
      _total_classes,
      _total_chunk_sz,
      _total_block_sz);
  _out->print_cr("ChunkSz: Total size of all allocated metaspace chunks");
  _out->print_cr("BlockSz: Total size of all allocated metaspace blocks (each chunk has several blocks)");
}


void ClassLoaderStatsClosure::addEmptyParents(oop cl) {
  while (cl != NULL && java_lang_ClassLoader::loader_data_acquire(cl) == NULL) {
    // This classloader has not loaded any classes
    bool added = false;
    ClassLoaderStats* cls = _stats->put_if_absent(cl, &added);
    if (added) {
      cls->_class_loader = cl;
      cls->_parent = java_lang_ClassLoader::parent(cl);
      _total_loaders++;
    }
    assert(cls->_class_loader == cl, "Sanity");

    cl = java_lang_ClassLoader::parent(cl);
  }
}


void ClassLoaderStatsVMOperation::doit() {
  ClassLoaderStatsClosure clsc (_out);
  ClassLoaderDataGraph::loaded_cld_do(&clsc);
  clsc.print();
}


void ClassLoaderStatsDCmd::execute(DCmdSource source, TRAPS) {
  ClassLoaderStatsVMOperation op(output());
  VMThread::execute(&op);
}
