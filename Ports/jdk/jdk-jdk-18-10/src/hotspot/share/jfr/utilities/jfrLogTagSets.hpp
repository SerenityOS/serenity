/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRLOGTAGSETS_HPP
#define SHARE_JFR_UTILITIES_JFRLOGTAGSETS_HPP

#include "logging/logTag.hpp"

/*
 * This file declares the Unified Logging (UL) tag sets for JFR.
 *
 * The JFR_LOG_TAG_SET_LIST macro will generate an enum
 * (zero-based ordinals) for these log tag sets.
 * Log tag set order (enum ordinals) is important
 * because we need to mirror this order in Java.
 *
 * Adding a new log tag set:
 *
 * 1. Ensure the log tag primitive which you attempt to use in your new log tag set is defined to UL.
 *    UL log tags are defined in logging/logTag.hpp.
 * 2. Append the new log tag set to the JFR_LOG_TAG_SET_LIST macro in this file.
 * 3. Make the corresponding change in Java by updating jdk.jfr.internal.LogTag.java
 *    to add another enum instance with a corresponding tag id ordinal.
 *
 */

#define JFR_LOG_TAG_SET_LIST \
  JFR_LOG_TAG(jfr) \
  JFR_LOG_TAG(jfr, system) \
  JFR_LOG_TAG(jfr, system, event) \
  JFR_LOG_TAG(jfr, system, setting) \
  JFR_LOG_TAG(jfr, system, bytecode) \
  JFR_LOG_TAG(jfr, system, parser) \
  JFR_LOG_TAG(jfr, system, metadata) \
  JFR_LOG_TAG(jfr, system, streaming) \
  JFR_LOG_TAG(jfr, system, throttle) \
  JFR_LOG_TAG(jfr, metadata) \
  JFR_LOG_TAG(jfr, event) \
  JFR_LOG_TAG(jfr, setting) \
  JFR_LOG_TAG(jfr, dcmd) \
  JFR_LOG_TAG(jfr, start)
  /* NEW TAGS, DONT FORGET TO UPDATE JAVA SIDE */

#endif // SHARE_JFR_UTILITIES_JFRLOGTAGSETS_HPP
