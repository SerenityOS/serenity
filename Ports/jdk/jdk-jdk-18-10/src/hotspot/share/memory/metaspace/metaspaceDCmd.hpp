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

#ifndef SHARE_MEMORY_METASPACE_METASPACEDCMD_HPP
#define SHARE_MEMORY_METASPACE_METASPACEDCMD_HPP

#include "services/diagnosticCommand.hpp"

class outputStream;

namespace metaspace {

class MetaspaceDCmd : public DCmdWithParser {
  DCmdArgument<bool> _basic;
  DCmdArgument<bool> _show_loaders;
  DCmdArgument<bool> _by_spacetype;
  DCmdArgument<bool> _by_chunktype;
  DCmdArgument<bool> _show_vslist;
  DCmdArgument<char*> _scale;
  DCmdArgument<bool> _show_classes;
public:
  MetaspaceDCmd(outputStream* output, bool heap);
  static const char* name() {
    return "VM.metaspace";
  }
  static const char* description() {
    return "Prints the statistics for the metaspace";
  }
  static const char* impact() {
      return "Medium: Depends on number of classes loaded.";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission",
                        "monitor", NULL};
    return p;
  }
  static int num_arguments();
  virtual void execute(DCmdSource source, TRAPS);
};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_METASPACEDCMD_HPP
