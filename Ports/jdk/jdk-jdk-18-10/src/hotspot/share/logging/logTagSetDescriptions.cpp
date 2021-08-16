/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/logTag.hpp"
#include "logging/logTagSet.hpp"
#include "logging/logTagSetDescriptions.hpp"

// List of described tag sets. Tags should be specified using the LOG_TAGS()
// macro. Described tag sets can be listed from command line (or DCMD) using
// -Xlog:help (or "VM.log list")
#define LOG_TAG_SET_DESCRIPTION_LIST \
  LOG_TAG_SET_DESCRIPTION(LOG_TAGS(logging), \
                          "Logging for the log framework itself")

#define LOG_TAG_SET_DESCRIPTION(tags, descr) \
  { &LogTagSetMapping<tags>::tagset(), descr },

struct LogTagSetDescription tagset_descriptions[] = {
  LOG_TAG_SET_DESCRIPTION_LIST
  { NULL, NULL }
};
