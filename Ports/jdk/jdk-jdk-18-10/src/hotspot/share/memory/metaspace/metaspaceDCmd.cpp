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
#include "memory/metaspace.hpp"
#include "memory/metaspace/metaspaceDCmd.hpp"
#include "memory/metaspace/metaspaceReporter.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/vmOperations.hpp"
#include "services/diagnosticCommand.hpp"
#include "services/nmtCommon.hpp"

namespace metaspace {

MetaspaceDCmd::MetaspaceDCmd(outputStream* output, bool heap) :
  DCmdWithParser(output, heap),
  _basic("basic", "Prints a basic summary (does not need a safepoint).", "BOOLEAN", false, "false"),
  _show_loaders("show-loaders", "Shows usage by class loader.", "BOOLEAN", false, "false"),
  _by_spacetype("by-spacetype", "Break down numbers by loader type.", "BOOLEAN", false, "false"),
  _by_chunktype("by-chunktype", "Break down numbers by chunk type.", "BOOLEAN", false, "false"),
  _show_vslist("vslist", "Shows details about the underlying virtual space.", "BOOLEAN", false, "false"),
  _scale("scale", "Memory usage in which to scale. Valid values are: 1, KB, MB or GB (fixed scale) "
         "or \"dynamic\" for a dynamically chosen scale.",
         "STRING", false, "dynamic"),
  _show_classes("show-classes", "If show-loaders is set, shows loaded classes for each loader.", "BOOLEAN", false, "false")
{
  _dcmdparser.add_dcmd_option(&_basic);
  _dcmdparser.add_dcmd_option(&_show_loaders);
  _dcmdparser.add_dcmd_option(&_show_classes);
  _dcmdparser.add_dcmd_option(&_by_chunktype);
  _dcmdparser.add_dcmd_option(&_by_spacetype);
  _dcmdparser.add_dcmd_option(&_show_vslist);
  _dcmdparser.add_dcmd_option(&_scale);
}

int MetaspaceDCmd::num_arguments() {
  ResourceMark rm;
  MetaspaceDCmd* dcmd = new MetaspaceDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

void MetaspaceDCmd::execute(DCmdSource source, TRAPS) {
  // Parse scale value.
  const char* scale_value = _scale.value();
  size_t scale = 0;
  if (scale_value != NULL) {
    if (strcasecmp("dynamic", scale_value) == 0) {
      scale = 0;
    } else {
      scale = NMT_ONLY(NMTUtil::scale_from_name(scale_value)) NOT_NMT(0);
      if (scale == 0) {
        output()->print_cr("Invalid scale: \"%s\". Will use dynamic scaling.", scale_value);
      }
    }
  }
  if (_basic.value() == true) {
    if (_show_loaders.value() || _by_chunktype.value() || _by_spacetype.value() ||
        _show_vslist.value()) {
      // Basic mode. Just print essentials. Does not need to be at a safepoint.
      output()->print_cr("In basic mode, additional arguments are ignored.");
    }
    MetaspaceUtils::print_basic_report(output(), scale);
  } else {
    // Full mode. Requires safepoint.
    int flags = 0;
    if (_show_loaders.value())         flags |= (int)MetaspaceReporter::Option::ShowLoaders;
    if (_show_classes.value())         flags |= (int)MetaspaceReporter::Option::ShowClasses;
    if (_by_chunktype.value())         flags |= (int)MetaspaceReporter::Option::BreakDownByChunkType;
    if (_by_spacetype.value())         flags |= (int)MetaspaceReporter::Option::BreakDownBySpaceType;
    if (_show_vslist.value())          flags |= (int)MetaspaceReporter::Option::ShowVSList;
    VM_PrintMetadata op(output(), scale, flags);
    VMThread::execute(&op);
  }
}

} // namespace metaspace

