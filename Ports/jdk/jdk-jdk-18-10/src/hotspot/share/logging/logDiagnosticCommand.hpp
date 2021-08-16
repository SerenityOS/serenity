/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_LOGGING_LOGDIAGNOSTICCOMMAND_HPP
#define SHARE_LOGGING_LOGDIAGNOSTICCOMMAND_HPP

#include "services/diagnosticCommand.hpp"

// The LogDiagnosticCommand represents the 'VM.log' DCMD
// that allows configuration of the logging at runtime.
// It can be used to view or modify the current log configuration.
// VM.log without additional arguments prints the usage description.
// The 'list' argument will list all available log tags,
// levels, decorators and currently configured log outputs.
// Specifying 'disable' will disable logging completely.
// The remaining arguments are used to set a log output to log everything
// with the specified tags and levels using the given decorators.
class LogDiagnosticCommand : public DCmdWithParser {
 protected:
  DCmdArgument<char *> _output;
  DCmdArgument<char *> _output_options;
  DCmdArgument<char *> _what;
  DCmdArgument<char *> _decorators;
  DCmdArgument<bool> _disable;
  DCmdArgument<bool> _list;
  DCmdArgument<bool> _rotate;

 public:
  LogDiagnosticCommand(outputStream* output, bool heap_allocated);
  void execute(DCmdSource source, TRAPS);
  static void registerCommand();
  static int num_arguments();

  static const char* name() {
    return "VM.log";
  }

  static const char* description() {
    return "Lists current log configuration, enables/disables/configures a log output, or rotates all logs.";
  }

  // Used by SecurityManager. This DCMD requires ManagementPermission = control.
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission", "control", NULL};
    return p;
  }
};

#endif // SHARE_LOGGING_LOGDIAGNOSTICCOMMAND_HPP
