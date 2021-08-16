/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_JAVA_HPP
#define SHARE_RUNTIME_JAVA_HPP

#include "runtime/os.hpp"
#include "utilities/globalDefinitions.hpp"

class Handle;
class JavaThread;
class Symbol;

// Execute code before all handles are released and thread is killed; prologue to vm_exit
extern void before_exit(JavaThread * thread);

// Forced VM exit (i.e, internal error or JVM_Exit)
extern void vm_exit(int code);

// Wrapper for ::exit()
extern void vm_direct_exit(int code);
extern void vm_direct_exit(int code, const char* message);

// Shutdown the VM but do not exit the process
extern void vm_shutdown();
// Shutdown the VM and abort the process
extern void vm_abort(bool dump_core=true);

// Trigger any necessary notification of the VM being shutdown
extern void notify_vm_shutdown();

// VM exit if error occurs during initialization of VM
extern void vm_exit_during_initialization();
extern void vm_exit_during_initialization(Handle exception);
extern void vm_exit_during_initialization(Symbol* exception_name, const char* message);
extern void vm_exit_during_initialization(const char* error, const char* message = NULL);
extern void vm_shutdown_during_initialization(const char* error, const char* message = NULL);

extern void vm_exit_during_cds_dumping(const char* error, const char* message = NULL);

/**
 * With the integration of the changes to handle the version string
 * as defined by JEP-223, most of the code related to handle the version
 * string prior to JDK 1.6 was removed (partial initialization)
 */
class JDK_Version {
  friend class VMStructs;
  friend class Universe;
  friend void JDK_Version_init();
 private:

  static JDK_Version _current;
  static const char* _java_version;
  static const char* _runtime_name;
  static const char* _runtime_version;
  static const char* _runtime_vendor_version;
  static const char* _runtime_vendor_vm_bug_url;

  uint8_t _major;
  uint8_t _minor;
  uint8_t _security;
  uint8_t _patch;
  uint8_t _build;

  bool is_valid() const {
    return (_major != 0);
  }

  // initializes or partially initializes the _current static field
  static void initialize();

 public:

  JDK_Version() :
      _major(0), _minor(0), _security(0), _patch(0), _build(0)
      {}

  JDK_Version(uint8_t major, uint8_t minor = 0, uint8_t security = 0,
              uint8_t patch = 0, uint8_t build = 0) :
      _major(major), _minor(minor), _security(security), _patch(patch), _build(build)
      {}

  // Returns the current running JDK version
  static JDK_Version current() { return _current; }

  // Factory methods for convenience
  static JDK_Version jdk(uint8_t m) {
    return JDK_Version(m);
  }

  static JDK_Version undefined() {
    return JDK_Version(0);
  }

  bool is_undefined() const {
    return _major == 0;
  }

  uint8_t major_version() const          { return _major; }
  uint8_t minor_version() const          { return _minor; }
  uint8_t security_version() const       { return _security; }
  uint8_t patch_version() const          { return _patch; }
  uint8_t build_number() const           { return _build; }

  // Performs a full ordering comparison using all fields (patch, build, etc.)
  int compare(const JDK_Version& other) const;

  /**
   * Performs comparison using only the major version, returning negative
   * if the major version of 'this' is less than the parameter, 0 if it is
   * equal, and a positive value if it is greater.
   */
  int compare_major(int version) const {
      return major_version() - version;
  }

  void to_string(char* buffer, size_t buflen) const;

  static const char* java_version() {
    return _java_version;
  }
  static void set_java_version(const char* version) {
    _java_version = os::strdup(version);
  }

  static const char* runtime_name() {
    return _runtime_name;
  }
  static void set_runtime_name(const char* name) {
    _runtime_name = os::strdup(name);
  }

  static const char* runtime_version() {
    return _runtime_version;
  }
  static void set_runtime_version(const char* version) {
    _runtime_version = os::strdup(version);
  }

  static const char* runtime_vendor_version() {
    return _runtime_vendor_version;
  }
  static void set_runtime_vendor_version(const char* vendor_version) {
    _runtime_vendor_version = os::strdup(vendor_version);
  }

  static const char* runtime_vendor_vm_bug_url() {
    return _runtime_vendor_vm_bug_url;
  }
  static void set_runtime_vendor_vm_bug_url(const char* vendor_vm_bug_url) {
    _runtime_vendor_vm_bug_url = os::strdup(vendor_vm_bug_url);
  }

};

#endif // SHARE_RUNTIME_JAVA_HPP
