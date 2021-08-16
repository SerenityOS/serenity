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

#ifndef SHARE_JFR_SUPPORT_JFRTHREADEXTENSION_HPP
#define SHARE_JFR_SUPPORT_JFRTHREADEXTENSION_HPP

#include "jfr/periodic/sampling/jfrThreadSampler.hpp"
#include "jfr/support/jfrThreadLocal.hpp"

#define DEFINE_THREAD_LOCAL_FIELD_JFR mutable JfrThreadLocal _jfr_thread_local

#define DEFINE_THREAD_LOCAL_OFFSET_JFR \
  static ByteSize jfr_thread_local_offset() { return in_ByteSize(offset_of(Thread, _jfr_thread_local)); }

#define THREAD_LOCAL_OFFSET_JFR Thread::jfr_thread_local_offset()

#define DEFINE_THREAD_LOCAL_TRACE_ID_OFFSET_JFR \
  static ByteSize trace_id_offset() { return in_ByteSize(offset_of(JfrThreadLocal, _trace_id)); }

#define DEFINE_THREAD_LOCAL_ACCESSOR_JFR \
  JfrThreadLocal* jfr_thread_local() const { return &_jfr_thread_local; }

#define THREAD_ID_OFFSET_JFR JfrThreadLocal::trace_id_offset()

#define THREAD_LOCAL_WRITER_OFFSET_JFR \
  JfrThreadLocal::java_event_writer_offset() + THREAD_LOCAL_OFFSET_JFR

#define SUSPEND_THREAD_CONDITIONAL(thread) if ((thread)->is_trace_suspend()) JfrThreadSampling::on_javathread_suspend(thread)

#endif // SHARE_JFR_SUPPORT_JFRTHREADEXTENSION_HPP
