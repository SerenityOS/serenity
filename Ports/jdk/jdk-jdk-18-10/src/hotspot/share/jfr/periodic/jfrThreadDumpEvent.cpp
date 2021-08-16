/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/dcmd/jfrDcmds.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/periodic/jfrThreadDumpEvent.hpp"
#include "logging/log.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/ostream.hpp"

/**
*  Worker impl for generating and writing dcmd commands
*  as jfr events.
*  dispatch to diagnosticcommands "parse_and_execute"
*
*  param: cmd = the DCMD to execute (including options)
*/
static bool execute_dcmd(bufferedStream& st, const char* const cmd) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  assert(!HAS_PENDING_EXCEPTION, "dcmd does not expect pending exceptions on entry!");
  // delegate to DCmd execution
  DCmd::parse_and_execute(DCmd_Source_Internal, &st, cmd, ' ', THREAD);
  if (HAS_PENDING_EXCEPTION) {
    log_debug(jfr, system)("unable to create jfr event for DCMD %s", cmd);
    log_debug(jfr, system)("exception type: %s", PENDING_EXCEPTION->klass()->external_name());
    // don't unwind this exception
    CLEAR_PENDING_EXCEPTION;
    // if exception occurred,
    // reset stream.
    st.reset();
    return false;
  }
  return true;
}

// caller needs ResourceMark
const char* JfrDcmdEvent::thread_dump() {
  assert(EventThreadDump::is_enabled(), "invariant");
  bufferedStream st;
  execute_dcmd(st, "Thread.print");
  return st.as_string();
}
