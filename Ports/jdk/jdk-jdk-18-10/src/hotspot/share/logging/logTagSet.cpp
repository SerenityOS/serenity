/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "logging/logDecorations.hpp"
#include "logging/logFileStreamOutput.hpp"
#include "logging/logLevel.hpp"
#include "logging/logMessageBuffer.hpp"
#include "logging/logOutput.hpp"
#include "logging/logTag.hpp"
#include "logging/logTagSet.hpp"
#include "logging/logTagSetDescriptions.hpp"
#include "memory/allocation.inline.hpp"
#include "utilities/ostream.hpp"

LogTagSet*  LogTagSet::_list      = NULL;
size_t      LogTagSet::_ntagsets  = 0;

// This constructor is called only during static initialization.
// See the declaration in logTagSet.hpp for more information.
LogTagSet::LogTagSet(PrefixWriter prefix_writer, LogTagType t0, LogTagType t1, LogTagType t2, LogTagType t3, LogTagType t4)
    : _next(_list), _write_prefix(prefix_writer) {
  _tag[0] = t0;
  _tag[1] = t1;
  _tag[2] = t2;
  _tag[3] = t3;
  _tag[4] = t4;
  for (_ntags = 0; _ntags < LogTag::MaxTags && _tag[_ntags] != LogTag::__NO_TAG; _ntags++) {
  }
  _list = this;
  _ntagsets++;

  // Set the default output to warning and error level for all new tagsets.
  _output_list.set_output_level(&StdoutLog, LogLevel::Default);
}

void LogTagSet::update_decorators(const LogDecorators& decorator) {
  LogDecorators new_decorators = decorator;
  for (LogOutputList::Iterator it = _output_list.iterator(); it != _output_list.end(); it++) {
    new_decorators.combine_with((*it)->decorators());
  }
  _decorators = new_decorators;
}

bool LogTagSet::has_output(const LogOutput* output) {
  for (LogOutputList::Iterator it = _output_list.iterator(); it != _output_list.end(); it++) {
    if (*it == output) {
      return true;
    }
  }
  return false;
}

void LogTagSet::log(LogLevelType level, const char* msg) {
  // Increasing the atomic reader counter in iterator(level) must
  // happen before the creation of LogDecorations instance so
  // wait_until_no_readers() in LogConfiguration::configure_output()
  // synchronizes _decorations as well. The order is guaranteed by
  // the implied memory order of Atomic::add().
  LogOutputList::Iterator it = _output_list.iterator(level);
  LogDecorations decorations(level, *this, _decorators);

  for (; it != _output_list.end(); it++) {
    (*it)->write(decorations, msg);
  }
}

void LogTagSet::log(const LogMessageBuffer& msg) {
  LogOutputList::Iterator it = _output_list.iterator(msg.least_detailed_level());
  LogDecorations decorations(LogLevel::Invalid, *this, _decorators);

  for (; it != _output_list.end(); it++) {
    (*it)->write(msg.iterator(it.level(), decorations));
  }
}

void LogTagSet::label(outputStream* st, const char* separator) const {
  for (size_t i = 0; i < _ntags; i++) {
    st->print("%s%s", (i == 0 ? "" : separator), LogTag::name(_tag[i]));
  }
}

int LogTagSet::label(char* buf, size_t len, const char* separator) const {
  stringStream ss(buf, len);
  label(&ss, separator);
  size_t written = ss.size();
  if (written >= len - 1) {
    return -1; // truncation
  }
  return (int)written;
}

void LogTagSet::write(LogLevelType level, const char* fmt, ...) {
  va_list args;
  va_start(args, fmt);
  vwrite(level, fmt, args);
  va_end(args);
}

const size_t vwrite_buffer_size = 512;

void LogTagSet::vwrite(LogLevelType level, const char* fmt, va_list args) {
  assert(level >= LogLevel::First && level <= LogLevel::Last, "Log level:%d is incorrect", level);
  char buf[vwrite_buffer_size];
  va_list saved_args;           // For re-format on buf overflow.
  va_copy(saved_args, args);
  size_t prefix_len = _write_prefix(buf, sizeof(buf));
  // Check that string fits in buffer; resize buffer if necessary
  int ret;
  if (prefix_len < vwrite_buffer_size) {
    ret = os::vsnprintf(buf + prefix_len, sizeof(buf) - prefix_len, fmt, args);
  } else {
    // Buffer too small. Just call printf to find out the length for realloc below.
    ret = os::vsnprintf(nullptr, 0, fmt, args);
  }

  assert(ret >= 0, "Log message buffer issue");
  if (ret < 0) {
    // Error, just log contents in buf.
    log(level, buf);
    log(level, "Log message buffer issue");
    va_end(saved_args);
    return;
  }


  size_t newbuf_len = (size_t)ret + prefix_len + 1; // total bytes needed including prefix.
  if (newbuf_len <= sizeof(buf)) {
    log(level, buf);
  } else {
    // Buffer too small, allocate a large enough buffer using malloc/free to avoid circularity.
    char* newbuf = (char*)::malloc(newbuf_len * sizeof(char));
    if (newbuf != nullptr) {
      prefix_len = _write_prefix(newbuf, newbuf_len);
      ret = os::vsnprintf(newbuf + prefix_len, newbuf_len - prefix_len, fmt, saved_args);
      assert(ret >= 0, "Log message newbuf issue");
      // log the contents in newbuf even with error happened.
      log(level, newbuf);
      if (ret < 0) {
        log(level, "Log message newbuf issue");
      }
      ::free(newbuf);
    } else {
      // Native OOM, use buf to output the least message. At this moment buf is full of either
      // truncated prefix or truncated prefix + string. Put trunc_msg at the end of buf.
      const char* trunc_msg = "..(truncated), native OOM";
      const size_t ltr = strlen(trunc_msg) + 1;
      ret = os::snprintf(buf + sizeof(buf) - ltr, ltr, "%s", trunc_msg);
      assert(ret >= 0, "Log message buffer issue");
      // log the contents in newbuf even with error happened.
      log(level, buf);
      if (ret < 0) {
        log(level, "Log message buffer issue under OOM");
      }
    }
  }
  va_end(saved_args);
}

static const size_t TagSetBufferSize = 128;

void LogTagSet::describe_tagsets(outputStream* out) {
  out->print_cr("Described tag sets:");
  for (const LogTagSetDescription* d = tagset_descriptions; d->tagset != NULL; d++) {
    out->sp();
    d->tagset->label(out, "+");
    out->print_cr(": %s", d->descr);
  }
}

static int qsort_strcmp(const void* a, const void* b) {
  return strcmp((*(const char**)a), (*(const char**)b));
}

void LogTagSet::list_all_tagsets(outputStream* out) {
  char** tagset_labels = NEW_C_HEAP_ARRAY(char*, _ntagsets, mtLogging);

  // Generate the list of tagset labels
  size_t idx = 0;
  for (LogTagSet* ts = first(); ts != NULL; ts = ts->next()) {
    char buf[TagSetBufferSize];
    ts->label(buf, sizeof(buf), "+");
    tagset_labels[idx++] = os::strdup_check_oom(buf, mtLogging);
  }
  assert(idx == _ntagsets, "_ntagsets and list of tagsets not in sync");

  // Sort them lexicographically
  qsort(tagset_labels, _ntagsets, sizeof(*tagset_labels), qsort_strcmp);

  // Print and then free the labels
  out->print("Available tag sets: ");
  for (idx = 0; idx < _ntagsets; idx++) {
    out->print("%s%s", (idx == 0 ? "" : ", "), tagset_labels[idx]);
    os::free(tagset_labels[idx]);
  }
  out->cr();
  FREE_C_HEAP_ARRAY(char*, tagset_labels);
}
