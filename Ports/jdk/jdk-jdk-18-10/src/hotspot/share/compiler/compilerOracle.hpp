/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_COMPILERORACLE_HPP
#define SHARE_COMPILER_COMPILERORACLE_HPP

#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"

class methodHandle;


// CompilerOracle is an interface for turning on and off compilation
// for some methods

//       OPTION_TYPES: type, name
#define OPTION_TYPES(type) \
  type(Intx, "intx") \
  type(Uintx, "uintx") \
  type(Bool, "bool") \
  type(Ccstr, "ccstr") \
  type(Ccstrlist, "ccstrlist") \
  type(Double, "double")

//       COMPILECOMMAND_OPTIONS: option, name, variant, type
#define COMPILECOMMAND_OPTIONS(option) \
  option(Help,  "help",  Unknown) \
  option(Quiet, "quiet", Unknown) \
  option(Log, "log", Bool) \
  option(Print, "print", Bool) \
  option(Inline,  "inline", Bool) \
  option(DontInline,  "dontinline", Bool) \
  option(Blackhole,  "blackhole", Bool) \
  option(CompileOnly, "compileonly", Bool)\
  option(Exclude, "exclude", Bool) \
  option(Break, "break", Bool) \
  option(BreakAtExecute, "BreakAtExecute", Bool) \
  option(BreakAtCompile, "BreakAtCompile", Bool) \
  option(PrintAssembly, "PrintAssembly", Bool) \
  option(PrintInlining, "PrintInlining", Bool) \
  option(PrintIntrinsics, "PrintIntrinsics", Bool) \
  option(PrintNMethods, "PrintNMethods", Bool)   \
  option(PrintOptoAssembly, "PrintOptoAssembly", Bool) \
  option(PrintDebugInfo,    "PrintDebugInfo",    Bool) \
  option(PrintRelocations,  "PrintRelocations",  Bool) \
  option(PrintDependencies, "PrintDependencies", Bool) \
  option(BackgroundCompilation, "BackgroundCompilation", Bool) \
  option(RepeatCompilation, "RepeatCompilation", Intx) \
  option(ReplayInline,   "ReplayInline", Bool) \
  option(DumpReplay,     "DumpReplay", Bool) \
  option(DumpInline,     "DumpInline", Bool) \
  option(CompileThresholdScaling, "CompileThresholdScaling", Double) \
  option(ControlIntrinsic,  "ControlIntrinsic",  Ccstrlist) \
  option(DisableIntrinsic,  "DisableIntrinsic",  Ccstrlist) \
  option(NoRTMLockEliding,  "NoRTMLockEliding",  Bool) \
  option(UseRTMLockEliding, "UseRTMLockEliding", Bool) \
  option(BlockLayoutByFrequency, "BlockLayoutByFrequency", Bool) \
  option(TraceOptoPipelining, "TraceOptoPipelining", Bool) \
  option(TraceOptoOutput, "TraceOptoOutput", Bool) \
  option(TraceSpilling, "TraceSpilling", Bool) \
  option(PrintIdeal, "PrintIdeal", Bool) \
  option(IGVPrintLevel, "IGVPrintLevel", Intx) \
  option(Vectorize, "Vectorize", Bool) \
  option(VectorizeDebug, "VectorizeDebug", Uintx) \
  option(CloneMapDebug, "CloneMapDebug", Bool) \
  option(IncrementalInlineForceCleanup, "IncrementalInlineForceCleanup", Bool) \
  option(MaxNodeLimit, "MaxNodeLimit", Intx)  \
NOT_PRODUCT(option(TestOptionInt,    "TestOptionInt",    Intx)) \
NOT_PRODUCT(option(TestOptionUint,   "TestOptionUint",   Uintx)) \
NOT_PRODUCT(option(TestOptionBool,   "TestOptionBool",   Bool)) \
NOT_PRODUCT(option(TestOptionBool2,  "TestOptionBool2",  Bool)) \
NOT_PRODUCT(option(TestOptionStr,    "TestOptionStr",    Ccstr)) \
NOT_PRODUCT(option(TestOptionList,   "TestOptionList",   Ccstrlist)) \
NOT_PRODUCT(option(TestOptionDouble, "TestOptionDouble", Double)) \
  option(Option, "option", Unknown) \
  option(Unknown, "unknown", Unknown)

enum class CompileCommand {
  #define enum_of_options(option, name, ctype) option,
    COMPILECOMMAND_OPTIONS(enum_of_options)
  #undef enum_of_options
  Count
};

enum class OptionType {
#define enum_of_types(type, name) type,
    OPTION_TYPES(enum_of_types)
#undef enum_of_types
    Unknown
};

class CompilerOracle : AllStatic {
 private:
  static bool _quiet;
  static void print_parse_error(char* error_msg, char* original_line);
  static void print_command(enum CompileCommand option, const char* name, enum OptionType type);

 public:
  // True if the command file has been specified or is implicit
  static bool has_command_file();

  // Reads from file and adds to lists
  static void parse_from_file();

  // Tells whether we to exclude compilation of method
  static bool should_exclude(const methodHandle& method);
  static bool be_quiet() { return _quiet; }

  // Tells whether we want to inline this method
  static bool should_inline(const methodHandle& method);

  // Tells whether we want to disallow inlining of this method
  static bool should_not_inline(const methodHandle& method);

  // Tells whether we should print the assembly for this method
  static bool should_print(const methodHandle& method);

  // Tells whether we should log the compilation data for this method
  static bool should_log(const methodHandle& method);

  // Tells whether to break when compiling method
  static bool should_break_at(const methodHandle& method);

  // Tells whether there are any methods to print for print_method_statistics()
  static bool should_print_methods();

  // Tags the method as blackhole candidate, if possible.
  static void tag_blackhole_if_possible(const methodHandle& method);

  // A wrapper for checking bool options
  static bool has_option(const methodHandle& method, enum CompileCommand option);

  // Check if method has option and value set. If yes, overwrite value and return true,
  // otherwise leave value unchanged and return false.
  template<typename T>
  static bool has_option_value(const methodHandle& method, enum CompileCommand option, T& value);

  // This check is currently only needed by whitebox API
  template<typename T>
  static bool option_matches_type(enum CompileCommand option, T& value);

  // Reads from string instead of file
  static void parse_from_string(const char* option_string, void (*parser)(char*));
  static void parse_from_line(char* line);
  static void parse_compile_only(char* line);

  // Fast check if there is any option set that compile control needs to know about
  static bool has_any_command_set();

  // convert a string to a proper compilecommand option - used from whitebox.
  // returns CompileCommand::Unknown on names not matching an option.
  static enum CompileCommand string_to_option(const char* name);

  // convert a string to a proper compilecommand option
  // returns CompileCommand::Unknown if name is not an option.
  static enum CompileCommand parse_option_name(const char* name);

  // convert a string to a proper option type
  // returns OptionType::Unknown on strings not matching an option type.
  static enum OptionType parse_option_type(const char* type_str);
};

#endif // SHARE_COMPILER_COMPILERORACLE_HPP
