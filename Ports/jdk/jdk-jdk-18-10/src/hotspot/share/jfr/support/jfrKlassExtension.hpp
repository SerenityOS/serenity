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

#ifndef SHARE_JFR_SUPPORT_JFRKLASSEXTENSION_HPP
#define SHARE_JFR_SUPPORT_JFRKLASSEXTENSION_HPP

#include "jfr/instrumentation/jfrEventClassTransformer.hpp"
#include "jfr/support/jfrTraceIdExtension.hpp"

#define DEFINE_KLASS_TRACE_ID_OFFSET \
  static ByteSize trace_id_offset() { return in_ByteSize(offset_of(InstanceKlass, _trace_id)); }

#define KLASS_TRACE_ID_OFFSET InstanceKlass::trace_id_offset()

#define JDK_JFR_EVENT_SUBKLASS 16
#define JDK_JFR_EVENT_KLASS    32
#define EVENT_HOST_KLASS       64
#define EVENT_RESERVED         128
#define IS_EVENT_KLASS(ptr) (((ptr)->trace_id() & (JDK_JFR_EVENT_KLASS | JDK_JFR_EVENT_SUBKLASS)) != 0)
#define ON_KLASS_CREATION(k, p, t) if (IS_EVENT_KLASS(k)) JfrEventClassTransformer::on_klass_creation(k, p, t)

#endif // SHARE_JFR_SUPPORT_JFRKLASSEXTENSION_HPP
