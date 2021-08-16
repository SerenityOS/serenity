/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/printCLDMetaspaceInfoClosure.hpp"
#include "memory/metaspace/printMetaspaceInfoKlassClosure.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

namespace metaspace {

PrintCLDMetaspaceInfoClosure::PrintCLDMetaspaceInfoClosure(outputStream* out, size_t scale, bool do_print,
                                                           bool do_print_classes, bool break_down_by_chunktype) :
  _out(out),
  _scale(scale),
  _do_print(do_print),
  _do_print_classes(do_print_classes),
  _break_down_by_chunktype(break_down_by_chunktype),
  _num_loaders(0),
  _num_loaders_without_metaspace(0),
  _num_loaders_unloading(0),
  _num_classes(0), _num_classes_shared(0)
{
  memset(_num_loaders_by_spacetype, 0, sizeof(_num_loaders_by_spacetype));
  memset(_num_classes_by_spacetype, 0, sizeof(_num_classes_by_spacetype));
  memset(_num_classes_shared_by_spacetype, 0, sizeof(_num_classes_shared_by_spacetype));
}

// A closure just to count classes
class CountKlassClosure : public KlassClosure {
public:

  uintx _num_classes;
  uintx _num_classes_shared;

  CountKlassClosure() : _num_classes(0), _num_classes_shared(0) {}
  void do_klass(Klass* k) {
    _num_classes++;
    if (k->is_shared()) {
      _num_classes_shared++;
    }
  }

}; // end: PrintKlassInfoClosure

void PrintCLDMetaspaceInfoClosure::do_cld(ClassLoaderData* cld) {
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at a safepoint");

  if (cld->is_unloading()) {
    _num_loaders_unloading++;
    return;
  }

  ClassLoaderMetaspace* msp = cld->metaspace_or_null();
  if (msp == NULL) {
    _num_loaders_without_metaspace++;
    return;
  }

  // Collect statistics for this class loader metaspace
  ClmsStats this_cld_stat;
  msp->add_to_statistics(&this_cld_stat);

  // And add it to the running totals
  _stats_total.add(this_cld_stat);
  _num_loaders++;
  _stats_by_spacetype[msp->space_type()].add(this_cld_stat);
  _num_loaders_by_spacetype[msp->space_type()] ++;

  // Count classes loaded by this CLD.
  CountKlassClosure ckc;
  cld->classes_do(&ckc);
  // accumulate.
  _num_classes += ckc._num_classes;
  _num_classes_by_spacetype[msp->space_type()] += ckc._num_classes;
  _num_classes_shared += ckc._num_classes_shared;
  _num_classes_shared_by_spacetype[msp->space_type()] += ckc._num_classes_shared;

  // Optionally, print
  if (_do_print) {
    _out->print(UINTX_FORMAT_W(4) ": ", _num_loaders);

    // Print "CLD for [<loader name>,] instance of <loader class name>"
    // or    "CLD for <hidden>, loaded by [<loader name>,] instance of <loader class name>"
    ResourceMark rm;
    const char* name = NULL;
    const char* class_name = NULL;

    // Note: this should also work if unloading:
    Klass* k = cld->class_loader_klass();
    if (k != NULL) {
      class_name = k->external_name();
      Symbol* s = cld->name();
      if (s != NULL) {
        name = s->as_C_string();
      }
    } else {
      name = "<bootstrap>";
    }

    // Print
    _out->print("CLD " PTR_FORMAT, p2i(cld));
    if (cld->is_unloading()) {
      _out->print(" (unloading)");
    }
    _out->print(":");
    if (cld->has_class_mirror_holder()) {
      _out->print(" <hidden class>, loaded by");
    }
    if (name != NULL) {
      _out->print(" \"%s\"", name);
    }
    if (class_name != NULL) {
      _out->print(" instance of %s", class_name);
    }

    if (_do_print_classes) {
      // Print a detailed description of all loaded classes.
      streamIndentor sti(_out, 6);
      _out->cr_indent();
      _out->print("Loaded classes");
      if (ckc._num_classes_shared > 0) {
        _out->print("('s' = shared)");
      }
      _out->print(":");
      PrintMetaspaceInfoKlassClosure pkic(_out, true);
      cld->classes_do(&pkic);
      _out->cr_indent();
      _out->print("-total-: ");
      print_number_of_classes(_out, ckc._num_classes, ckc._num_classes_shared);
    } else {
      // Just print a summary about how many classes have been loaded.
      _out->print(", ");
      print_number_of_classes(_out, ckc._num_classes, ckc._num_classes_shared);
    }

    // Print statistics
    this_cld_stat.print_on(_out, _scale, _break_down_by_chunktype);
    _out->cr();
  }
}

} // namespace metaspace

