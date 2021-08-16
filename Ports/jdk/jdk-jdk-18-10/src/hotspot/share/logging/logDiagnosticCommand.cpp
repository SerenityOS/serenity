/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/logConfiguration.hpp"
#include "logging/logDiagnosticCommand.hpp"
#include "memory/resourceArea.hpp"
#include "utilities/globalDefinitions.hpp"

LogDiagnosticCommand::LogDiagnosticCommand(outputStream* output, bool heap_allocated)
  : DCmdWithParser(output, heap_allocated),
    _output("output", "The name or index (#<index>) of output to configure.", "STRING", false),
    _output_options("output_options", "Options for the output.", "STRING", false),
    _what("what", "Configures what tags to log.", "STRING", false),
    _decorators("decorators", "Configures which decorators to use. Use 'none' or an empty value to remove all.", "STRING", false),
    _disable("disable", "Turns off all logging and clears the log configuration.", "BOOLEAN", false),
    _list("list", "Lists current log configuration.", "BOOLEAN", false),
    _rotate("rotate", "Rotates all logs.", "BOOLEAN", false) {
  _dcmdparser.add_dcmd_option(&_output);
  _dcmdparser.add_dcmd_option(&_output_options);
  _dcmdparser.add_dcmd_option(&_what);
  _dcmdparser.add_dcmd_option(&_decorators);
  _dcmdparser.add_dcmd_option(&_disable);
  _dcmdparser.add_dcmd_option(&_list);
  _dcmdparser.add_dcmd_option(&_rotate);
}

int LogDiagnosticCommand::num_arguments() {
  ResourceMark rm;
  LogDiagnosticCommand* dcmd = new LogDiagnosticCommand(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

void LogDiagnosticCommand::registerCommand() {
  uint32_t full_visibility = DCmd_Source_Internal | DCmd_Source_AttachAPI | DCmd_Source_MBean;
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<LogDiagnosticCommand>(full_visibility, true, false));
}

void LogDiagnosticCommand::execute(DCmdSource source, TRAPS) {
  bool any_command = false;
  if (_disable.has_value()) {
    LogConfiguration::disable_logging();
    any_command = true;
  }

  if (_output.has_value() || _what.has_value() || _decorators.has_value()) {
    if (!LogConfiguration::parse_log_arguments(_output.value(),
                                               _what.value(),
                                               _decorators.value(),
                                               _output_options.value(),
                                               output())) {
      return;
    }
    any_command = true;
  }

  if (_list.has_value()) {
    LogConfiguration::describe(output());
    any_command = true;
  }

  if (_rotate.has_value()) {
    LogConfiguration::rotate_all_outputs();
    any_command = true;
  }

  if (!any_command) {
    // If no argument was provided, print usage
    print_help(LogDiagnosticCommand::name());
  }

}
