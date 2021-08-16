/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_PRINTCLDMETASPACEINFOCLOSURE_HPP
#define SHARE_MEMORY_METASPACE_PRINTCLDMETASPACEINFOCLOSURE_HPP

#include "memory/iterator.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

class PrintCLDMetaspaceInfoClosure : public CLDClosure {
private:
  outputStream* const _out;
  const size_t        _scale;
  const bool          _do_print;
  const bool          _do_print_classes;
  const bool          _break_down_by_chunktype;

public:

  uintx                           _num_loaders;
  uintx                           _num_loaders_without_metaspace;
  uintx                           _num_loaders_unloading;
  ClmsStats                       _stats_total;

  uintx                           _num_loaders_by_spacetype [Metaspace::MetaspaceTypeCount];
  ClmsStats                       _stats_by_spacetype [Metaspace::MetaspaceTypeCount];

  uintx                           _num_classes_by_spacetype [Metaspace::MetaspaceTypeCount];
  uintx                           _num_classes_shared_by_spacetype [Metaspace::MetaspaceTypeCount];
  uintx                           _num_classes;
  uintx                           _num_classes_shared;

  PrintCLDMetaspaceInfoClosure(outputStream* out, size_t scale, bool do_print,
                               bool do_print_classes, bool break_down_by_chunktype);
  void do_cld(ClassLoaderData* cld);

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_PRINTCLDMETASPACEINFOCLOSURE_HPP
