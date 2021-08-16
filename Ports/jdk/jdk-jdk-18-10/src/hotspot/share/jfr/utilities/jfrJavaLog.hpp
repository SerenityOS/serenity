/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRJAVALOG_HPP
#define SHARE_JFR_UTILITIES_JFRJAVALOG_HPP

#include "memory/allocation.hpp"
#include "utilities/exceptions.hpp"

/*
 * A thin two-way "bridge" allowing our Java components to interface with Unified Logging (UL)
 *
 * Java can "subscribe" to be notified about UL configuration changes.
 * On such a configuration change, if applicable, the passed in LogTag enum instance
 * will be updated to reflect a new LogLevel.
 *
 * Log messages originating in Java are forwarded to UL for output.
 *
 */

class JfrJavaLog : public AllStatic {
 public:
  static void subscribe_log_level(jobject log_tag, jint id, TRAPS);
  static void log(jint tag_set, jint level, jstring message, TRAPS);
  static void log_event(JNIEnv* env, jint level, jobjectArray lines, bool system, TRAPS);
};

#endif // SHARE_JFR_UTILITIES_JFRJAVALOG_HPP
