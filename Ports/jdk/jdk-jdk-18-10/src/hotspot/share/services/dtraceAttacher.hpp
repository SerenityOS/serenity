/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_DTRACEATTACHER_HPP
#define SHARE_SERVICES_DTRACEATTACHER_HPP

#define DTRACE_ALLOC_PROBES    0x1
#define DTRACE_METHOD_PROBES   0x2
#define DTRACE_MONITOR_PROBES  0x4
#define DTRACE_ALL_PROBES      (DTRACE_ALLOC_PROBES  | \
                                DTRACE_METHOD_PROBES | \
                                DTRACE_MONITOR_PROBES)

class DTrace : public AllStatic {
 private:
  // disable one or more probes - OR above constants
  static void disable_dprobes(int probe_types);

 public:
  // enable one or more probes - OR above constants
  static void enable_dprobes(int probe_types);
  // all clients detached, do any clean-up
  static void detach_all_clients();
  // set ExtendedDTraceProbes flag
  static void set_extended_dprobes(bool value);
  // set DTraceMonitorProbes flag
  static void set_monitor_dprobes(bool value);
};

#endif // SHARE_SERVICES_DTRACEATTACHER_HPP
