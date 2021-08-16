/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSLOADERHIERARCHYDCMD_HPP
#define SHARE_CLASSFILE_CLASSLOADERHIERARCHYDCMD_HPP

#include "services/diagnosticCommand.hpp"

class ClassLoaderHierarchyDCmd: public DCmdWithParser {
  DCmdArgument<bool> _show_classes;
  DCmdArgument<bool> _verbose;
  DCmdArgument<bool> _fold;
public:

  ClassLoaderHierarchyDCmd(outputStream* output, bool heap);

  static const char* name() {
    return "VM.classloaders";
  }

  static const char* description() {
    return "Prints classloader hierarchy.";
  }
  static const char* impact() {
      return "Medium: Depends on number of class loaders and classes loaded.";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission",
                        "monitor", NULL};
    return p;
  }
  static int num_arguments();
  virtual void execute(DCmdSource source, TRAPS);

};

#endif // SHARE_CLASSFILE_CLASSLOADERHIERARCHYDCMD_HPP
