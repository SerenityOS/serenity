/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_DIAGNOSTICFRAMEWORK_HPP
#define SHARE_SERVICES_DIAGNOSTICFRAMEWORK_HPP

#include "classfile/vmSymbols.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/os.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/ostream.hpp"
#include <type_traits>


enum DCmdSource {
  DCmd_Source_Internal  = 0x01U,  // invocation from the JVM
  DCmd_Source_AttachAPI = 0x02U,  // invocation via the attachAPI
  DCmd_Source_MBean     = 0x04U   // invocation via a MBean
};

// Warning: strings referenced by the JavaPermission struct are passed to
// the native part of the JDK. Avoid use of dynamically allocated strings
// that could be de-allocated before the JDK native code had time to
// convert them into Java Strings.
struct JavaPermission {
  const char* _class;
  const char* _name;
  const char* _action;
};

// CmdLine is the class used to handle a command line containing a single
// diagnostic command and its arguments. It provides methods to access the
// command name and the beginning of the arguments. The class is also
// able to identify commented command lines and the "stop" keyword
class CmdLine : public StackObj {
private:
  const char* _cmd;
  size_t      _cmd_len;
  const char* _args;
  size_t      _args_len;
public:
  CmdLine(const char* line, size_t len, bool no_command_name);
  const char* args_addr() const   { return _args; }
  size_t args_len() const         { return _args_len; }
  const char* cmd_addr() const    { return _cmd; }
  size_t cmd_len() const          { return _cmd_len; }
  bool is_empty() const           { return _cmd_len == 0; }
  bool is_executable() const      { return is_empty() || _cmd[0] != '#'; }
  bool is_stop() const            { return !is_empty() && strncmp("stop", _cmd, _cmd_len) == 0; }
};

// Iterator class taking a character string in input and returning a CmdLine
// instance for each command line. The argument delimiter has to be specified.
class DCmdIter : public StackObj {
  friend class DCmd;
private:
  const char* const _str;
  const char        _delim;
  const size_t      _len;
  size_t      _cursor;
public:

  DCmdIter(const char* str, char delim)
   : _str(str), _delim(delim), _len(::strlen(str)),
     _cursor(0) {}
  bool has_next() const { return _cursor < _len; }
  CmdLine next() {
    assert(_cursor <= _len, "Cannot iterate more");
    size_t n = _cursor;
    while (n < _len && _str[n] != _delim) n++;
    CmdLine line(&(_str[_cursor]), n - _cursor, false);
    _cursor = n + 1;
    // The default copy constructor of CmdLine is used to return a CmdLine
    // instance to the caller.
    return line;
  }
};

// Iterator class to iterate over diagnostic command arguments
class DCmdArgIter : public ResourceObj {
  const char* const _buffer;
  const size_t      _len;
  size_t      _cursor;
  const char* _key_addr;
  size_t      _key_len;
  const char* _value_addr;
  size_t      _value_len;
  const char  _delim;
public:
  DCmdArgIter(const char* buf, size_t len, char delim)
    : _buffer(buf), _len(len), _cursor(0), _key_addr(NULL),
      _key_len(0), _value_addr(NULL), _value_len(0), _delim(delim) {}

  bool next(TRAPS);
  const char* key_addr() const    { return _key_addr; }
  size_t key_length() const       { return _key_len; }
  const char* value_addr() const  { return _value_addr; }
  size_t value_length() const     { return _value_len; }
};

// A DCmdInfo instance provides a description of a diagnostic command. It is
// used to export the description to the JMX interface of the framework.
class DCmdInfo : public ResourceObj {
protected:
  const char* const _name;           /* Name of the diagnostic command */
  const char* const _description;    /* Short description */
  const char* const _impact;         /* Impact on the JVM */
  const JavaPermission _permission;  /* Java Permission required to execute this command if any */
  const int         _num_arguments;  /* Number of supported options or arguments */
  const bool        _is_enabled;     /* True if the diagnostic command can be invoked, false otherwise */
public:
  DCmdInfo(const char* name,
          const char* description,
          const char* impact,
          JavaPermission permission,
          int num_arguments,
          bool enabled)
  : _name(name), _description(description), _impact(impact), _permission(permission),
    _num_arguments(num_arguments), _is_enabled(enabled) {}
  const char* name() const          { return _name; }
  const char* description() const   { return _description; }
  const char* impact() const        { return _impact; }
  const JavaPermission& permission() const { return _permission; }
  int num_arguments() const         { return _num_arguments; }
  bool is_enabled() const           { return _is_enabled; }

  static bool by_name(void* name, DCmdInfo* info);
};

// A DCmdArgumentInfo instance provides a description of a diagnostic command
// argument. It is used to export the description to the JMX interface of the
// framework.
class DCmdArgumentInfo : public ResourceObj {
protected:
  const char* const _name;            /* Option/Argument name*/
  const char* const _description;     /* Short description */
  const char* const _type;            /* Type: STRING, BOOLEAN, etc. */
  const char* const _default_string;  /* Default value in a parsable string */
  const bool        _mandatory;       /* True if the option/argument is mandatory */
  const bool        _option;          /* True if it is an option, false if it is an argument */
                                /* (see diagnosticFramework.hpp for option/argument definitions) */
  const bool        _multiple;        /* True is the option can be specified several time */
  const int         _position;        /* Expected position for this argument (this field is */
                                /* meaningless for options) */
public:
  DCmdArgumentInfo(const char* name, const char* description, const char* type,
                   const char* default_string, bool mandatory, bool option,
                   bool multiple, int position = -1)
    : _name(name), _description(description), _type(type),
      _default_string(default_string), _mandatory(mandatory), _option(option),
      _multiple(multiple), _position(position) {}

  const char* name() const        { return _name; }
  const char* description() const { return _description; }
  const char* type() const        { return _type; }
  const char* default_string() const { return _default_string; }
  bool is_mandatory() const       { return _mandatory; }
  bool is_option() const          { return _option; }
  bool is_multiple() const        { return _multiple; }
  int position() const            { return _position; }
};

// The DCmdParser class can be used to create an argument parser for a
// diagnostic command. It is not mandatory to use it to parse arguments.
// The DCmdParser parses a CmdLine instance according to the parameters that
// have been declared by its associated diagnostic command. A parameter can
// either be an option or an argument. Options are identified by the option name
// while arguments are identified by their position in the command line. The
// position of an argument is defined relative to all arguments passed on the
// command line, options are not considered when defining an argument position.
// The generic syntax of a diagnostic command is:
//
//    <command name> [<option>=<value>] [<argument_value>]
//
// Example:
//
//    command_name option1=value1 option2=value argumentA argumentB argumentC
//
// In this command line, the diagnostic command receives five parameters, two
// options named option1 and option2, and three arguments. argumentA's position
// is 0, argumentB's position is 1 and argumentC's position is 2.
class DCmdParser {
private:
  GenDCmdArgument* _options;
  GenDCmdArgument* _arguments_list;
public:
  DCmdParser()
    : _options(NULL), _arguments_list(NULL) {}
  void add_dcmd_option(GenDCmdArgument* arg);
  void add_dcmd_argument(GenDCmdArgument* arg);
  GenDCmdArgument* lookup_dcmd_option(const char* name, size_t len);
  GenDCmdArgument* arguments_list() const { return _arguments_list; };
  void check(TRAPS);
  void parse(CmdLine* line, char delim, TRAPS);
  void print_help(outputStream* out, const char* cmd_name) const;
  void reset(TRAPS);
  void cleanup();
  int num_arguments() const;
  GrowableArray<const char*>* argument_name_array() const;
  GrowableArray<DCmdArgumentInfo*>* argument_info_array() const;
};

// The DCmd class is the parent class of all diagnostic commands
// Diagnostic command instances should not be instantiated directly but
// created using the associated factory. The factory can be retrieved with
// the DCmdFactory::getFactory() method.
// A diagnostic command instance can either be allocated in the resource Area
// or in the C-heap. Allocation in the resource area is recommended when the
// current thread is the only one which will access the diagnostic command
// instance. Allocation in the C-heap is required when the diagnostic command
// is accessed by several threads (for instance to perform asynchronous
// execution).
// To ensure a proper cleanup, it's highly recommended to use a DCmdMark for
// each diagnostic command instance. In case of a C-heap allocated diagnostic
// command instance, the DCmdMark must be created in the context of the last
// thread that will access the instance.
class DCmd : public ResourceObj {
protected:
  outputStream* const _output;
  const bool          _is_heap_allocated;
public:
  DCmd(outputStream* output, bool heap_allocated)
   : _output(output), _is_heap_allocated(heap_allocated) {}

  // Child classes: please always provide these methods:
  //  static const char* name()             { return "<command name>";}
  //  static const char* description()      { return "<command help>";}

  static const char* disabled_message() { return "Diagnostic command currently disabled"; }

  // The impact() method returns a description of the intrusiveness of the diagnostic
  // command on the Java Virtual Machine behavior. The rational for this method is that some
  // diagnostic commands can seriously disrupt the behavior of the Java Virtual Machine
  // (for instance a Thread Dump for an application with several tens of thousands of threads,
  // or a Head Dump with a 40GB+ heap size) and other diagnostic commands have no serious
  // impact on the JVM (for instance, getting the command line arguments or the JVM version).
  // The recommended format for the description is <impact level>: [longer description],
  // where the impact level is selected among this list: {Low, Medium, High}. The optional
  // longer description can provide more specific details like the fact that Thread Dump
  // impact depends on the heap size.
  static const char* impact()       { return "Low: No impact"; }

  // The permission() method returns the description of Java Permission. This
  // permission is required when the diagnostic command is invoked via the
  // DiagnosticCommandMBean. The rationale for this permission check is that
  // the DiagnosticCommandMBean can be used to perform remote invocations of
  // diagnostic commands through the PlatformMBeanServer. The (optional) Java
  // Permission associated with each diagnostic command should ease the work
  // of system administrators to write policy files granting permissions to
  // execute diagnostic commands to remote users. Any diagnostic command with
  // a potential impact on security should overwrite this method.
  static const JavaPermission permission() {
    JavaPermission p = {NULL, NULL, NULL};
    return p;
  }
  // num_arguments() is used by the DCmdFactoryImpl::get_num_arguments() template functions.
  // - For subclasses of DCmdWithParser, it's calculated by DCmdParser::num_arguments().
  // - Other subclasses of DCmd have zero arguments by default. You can change this
  //   by defining your own version of MyDCmd::num_arguments().
  static int num_arguments()        { return 0; }
  outputStream* output() const      { return _output; }
  bool is_heap_allocated() const    { return _is_heap_allocated; }
  virtual void print_help(const char* name) const {
    output()->print_cr("Syntax: %s", name);
  }
  virtual void parse(CmdLine* line, char delim, TRAPS) {
    DCmdArgIter iter(line->args_addr(), line->args_len(), delim);
    bool has_arg = iter.next(CHECK);
    if (has_arg) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                "The argument list of this diagnostic command should be empty.");
    }
  }
  virtual void execute(DCmdSource source, TRAPS) { }
  virtual void reset(TRAPS) { }
  virtual void cleanup() { }

  // support for the JMX interface
  virtual GrowableArray<const char*>* argument_name_array() const {
    GrowableArray<const char*>* array = new GrowableArray<const char*>(0);
    return array;
  }
  virtual GrowableArray<DCmdArgumentInfo*>* argument_info_array() const {
    GrowableArray<DCmdArgumentInfo*>* array = new GrowableArray<DCmdArgumentInfo*>(0);
    return array;
  }

  // main method to invoke the framework
  static void parse_and_execute(DCmdSource source, outputStream* out, const char* cmdline,
                                char delim, TRAPS);
};

class DCmdWithParser : public DCmd {
protected:
  DCmdParser _dcmdparser;
public:
  DCmdWithParser (outputStream *output, bool heap=false) : DCmd(output, heap) { }
  static const char* disabled_message() { return "Diagnostic command currently disabled"; }
  static const char* impact()         { return "Low: No impact"; }
  virtual void parse(CmdLine *line, char delim, TRAPS);
  virtual void execute(DCmdSource source, TRAPS) { }
  virtual void reset(TRAPS);
  virtual void cleanup();
  virtual void print_help(const char* name) const;
  virtual GrowableArray<const char*>* argument_name_array() const;
  virtual GrowableArray<DCmdArgumentInfo*>* argument_info_array() const;
  DCmdParser* dcmdparser() {
    return &_dcmdparser;
  }
};

class DCmdMark : public StackObj {
  DCmd* const _ref;
public:
  DCmdMark(DCmd* cmd) : _ref(cmd) {}
  ~DCmdMark() {
    if (_ref != NULL) {
      _ref->cleanup();
      if (_ref->is_heap_allocated()) {
        delete _ref;
      }
    }
  }
};

// Diagnostic commands are not directly instantiated but created with a factory.
// Each diagnostic command class has its own factory. The DCmdFactory class also
// manages the status of the diagnostic command (hidden, enabled). A DCmdFactory
// has to be registered to make the diagnostic command available (see
// management.cpp)
class DCmdFactory: public CHeapObj<mtInternal> {
private:
  static bool         _send_jmx_notification;
  static bool         _has_pending_jmx_notification;
  static DCmdFactory* _DCmdFactoryList;

  // Pointer to the next factory in the singly-linked list of registered
  // diagnostic commands
  DCmdFactory*        _next;
  // When disabled, a diagnostic command cannot be executed. Any attempt to
  // execute it will result in the printing of the disabled message without
  // instantiating the command.
  const bool          _enabled;
  // When hidden, a diagnostic command doesn't appear in the list of commands
  // provided by the 'help' command.
  const bool          _hidden;
  const uint32_t      _export_flags;
  const int           _num_arguments;

public:
  DCmdFactory(int num_arguments, uint32_t flags, bool enabled, bool hidden)
    : _next(NULL), _enabled(enabled), _hidden(hidden),
      _export_flags(flags), _num_arguments(num_arguments) {}
  bool is_enabled() const       { return _enabled; }
  bool is_hidden() const        { return _hidden; }
  uint32_t export_flags() const { return _export_flags; }
  int num_arguments() const     { return _num_arguments; }
  DCmdFactory* next() const     { return _next; }
  virtual DCmd* create_resource_instance(outputStream* output) const = 0;
  virtual const char* name() const = 0;
  virtual const char* description() const = 0;
  virtual const char* impact() const = 0;
  virtual const JavaPermission permission() const = 0;
  virtual const char* disabled_message() const = 0;
  // Register a DCmdFactory to make a diagnostic command available.
  // Once registered, a diagnostic command must not be unregistered.
  // To prevent a diagnostic command from being executed, just set the
  // enabled flag to false.
  static int register_DCmdFactory(DCmdFactory* factory);
  static DCmdFactory* factory(DCmdSource source, const char* cmd, size_t len);
  // Returns a resourceArea allocated diagnostic command for the given command line
  static DCmd* create_local_DCmd(DCmdSource source, CmdLine &line, outputStream* out, TRAPS);
  static GrowableArray<const char*>* DCmd_list(DCmdSource source);
  static GrowableArray<DCmdInfo*>* DCmdInfo_list(DCmdSource source);

  static void set_jmx_notification_enabled(bool enabled) {
    _send_jmx_notification = enabled;
  }
  static void push_jmx_notification_request();
  static bool has_pending_jmx_notification() { return _has_pending_jmx_notification; }
  static void send_notification(TRAPS);
private:
  static void send_notification_internal(TRAPS);

  friend class HelpDCmd;
};

// Template to easily create DCmdFactory instances. See management.cpp
// where this template is used to create and register factories.
template <class DCmdClass> class DCmdFactoryImpl : public DCmdFactory {
public:
  DCmdFactoryImpl(uint32_t flags, bool enabled, bool hidden) :
    DCmdFactory(get_num_arguments<DCmdClass>(), flags, enabled, hidden) { }
  // Returns a resourceArea allocated instance
  DCmd* create_resource_instance(outputStream* output) const {
    return new DCmdClass(output, false);
  }
  const char* name() const {
    return DCmdClass::name();
  }
  const char* description() const {
    return DCmdClass::description();
  }
  const char* impact() const {
    return DCmdClass::impact();
  }
  const JavaPermission permission() const {
    return DCmdClass::permission();
  }
  const char* disabled_message() const {
     return DCmdClass::disabled_message();
  }

private:
  template <typename T, ENABLE_IF(!std::is_base_of<DCmdWithParser, T>::value)>
  static int get_num_arguments() {
    return T::num_arguments();
  }

  template <typename T, ENABLE_IF(std::is_base_of<DCmdWithParser, T>::value)>
  static int get_num_arguments() {
    ResourceMark rm;
    DCmdClass* dcmd = new DCmdClass(NULL, false);
    if (dcmd != NULL) {
      DCmdMark mark(dcmd);
      return dcmd->dcmdparser()->num_arguments();
    } else {
      return 0;
    }
  }
};

// This class provides a convenient way to register Dcmds, without a need to change
// management.cpp every time. Body of these two methods resides in
// diagnosticCommand.cpp

class DCmdRegistrant : public AllStatic {

private:
    static void register_dcmds();
    static void register_dcmds_ext();

    friend class Management;
};

#endif // SHARE_SERVICES_DIAGNOSTICFRAMEWORK_HPP
