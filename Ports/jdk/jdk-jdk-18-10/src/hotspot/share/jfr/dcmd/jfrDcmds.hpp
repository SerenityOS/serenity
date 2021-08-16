/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_DCMD_JFRDCMDS_HPP
#define SHARE_JFR_DCMD_JFRDCMDS_HPP

#include "services/diagnosticCommand.hpp"
class JfrJavaArguments;

class JfrDCmd : public DCmd {
 private:
  const char* _args;
  const int _num_arguments;
  char _delimiter;
 protected:
  JfrDCmd(outputStream* output, bool heap, int num_arguments);
  virtual const char* javaClass() const = 0;
  void invoke(JfrJavaArguments& method, TRAPS) const;
 public:
  virtual void execute(DCmdSource source, TRAPS);
  virtual void print_help(const char* name) const;
  virtual GrowableArray<const char*>* argument_name_array() const;
  virtual GrowableArray<DCmdArgumentInfo*>* argument_info_array() const;
  virtual void parse(CmdLine* line, char delim, TRAPS);
};

class JfrStartFlightRecordingDCmd : public JfrDCmd {
 public:
  JfrStartFlightRecordingDCmd(outputStream* output, bool heap) : JfrDCmd(output, heap, num_arguments()) {}

  static const char* name() {
    return "JFR.start";
  }
  static const char* description() {
    return "Starts a new JFR recording";
  }
  static const char* impact() {
    return "Medium: Depending on the settings for a recording, the impact can range from low to high.";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission", "monitor", NULL};
    return p;
  }
  virtual const char* javaClass() const {
    return "jdk/jfr/internal/dcmd/DCmdStart";
  }
  static int num_arguments() {
    return 11;
  }
};

class JfrDumpFlightRecordingDCmd : public JfrDCmd {
 public:
  JfrDumpFlightRecordingDCmd(outputStream* output, bool heap) : JfrDCmd(output, heap, num_arguments()) {}

  static const char* name() {
    return "JFR.dump";
  }
  static const char* description() {
    return "Copies contents of a JFR recording to file. Either the name or the recording id must be specified.";
  }
  static const char* impact() {
    return "Low";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission", "monitor", NULL};
    return p;
  }
  virtual const char* javaClass() const {
    return "jdk/jfr/internal/dcmd/DCmdDump";
  }
  static int num_arguments() {
    return 7;
  }
};

class JfrCheckFlightRecordingDCmd : public JfrDCmd {
 public:
  JfrCheckFlightRecordingDCmd(outputStream* output, bool heap) : JfrDCmd(output, heap, num_arguments()) {}

  static const char* name() {
    return "JFR.check";
  }
  static const char* description() {
    return "Checks running JFR recording(s)";
  }
  static const char* impact() {
    return "Low";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission", "monitor", NULL};
    return p;
  }
  virtual const char* javaClass() const {
    return "jdk/jfr/internal/dcmd/DCmdCheck";
  }
  static int num_arguments() {
    return 2;
  }
};

class JfrStopFlightRecordingDCmd : public JfrDCmd {
 public:
  JfrStopFlightRecordingDCmd(outputStream* output, bool heap) : JfrDCmd(output, heap, num_arguments()) {}

  static const char* name() {
    return "JFR.stop";
  }
  static const char* description() {
    return "Stops a JFR recording";
  }
  static const char* impact() {
    return "Low";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission", "monitor", NULL};
    return p;
  }
  virtual const char* javaClass() const {
    return "jdk/jfr/internal/dcmd/DCmdStop";
  }
  static int num_arguments() {
    return 2;
  }
};

class JfrConfigureFlightRecorderDCmd : public DCmdWithParser {
  friend class JfrOptionSet;
 protected:
  DCmdArgument<char*> _repository_path;
  DCmdArgument<char*> _dump_path;
  DCmdArgument<jlong> _stack_depth;
  DCmdArgument<jlong> _global_buffer_count;
  DCmdArgument<MemorySizeArgument> _global_buffer_size;
  DCmdArgument<MemorySizeArgument> _thread_buffer_size;
  DCmdArgument<MemorySizeArgument> _memory_size;
  DCmdArgument<MemorySizeArgument> _max_chunk_size;
  DCmdArgument<bool>  _sample_threads;
  bool _verbose;

 public:
  JfrConfigureFlightRecorderDCmd(outputStream* output, bool heap);
  void set_verbose(bool verbose) {
    _verbose = verbose;
  }
  static const char* name() {
    return "JFR.configure";
  }
  static const char* description() {
    return "Configure JFR";
  }
  static const char* impact() {
    return "Low";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission", "monitor", NULL};
    return p;
  }
  static int num_arguments();
  virtual void execute(DCmdSource source, TRAPS);
  virtual void print_help(const char* name) const;
};


bool register_jfr_dcmds();

#endif // SHARE_JFR_DCMD_JFRDCMDS_HPP
