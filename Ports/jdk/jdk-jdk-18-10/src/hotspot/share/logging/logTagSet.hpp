/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_LOGGING_LOGTAGSET_HPP
#define SHARE_LOGGING_LOGTAGSET_HPP

#include "logging/logDecorators.hpp"
#include "logging/logLevel.hpp"
#include "logging/logOutputList.hpp"
#include "logging/logPrefix.hpp"
#include "logging/logTag.hpp"
#include "utilities/globalDefinitions.hpp"

class LogMessageBuffer;

class outputStream;

// The tagset represents a combination of tags that occur in a log call somewhere.
// Tagsets are created automatically by the LogTagSetMappings and should never be
// instantiated directly somewhere else.
class LogTagSet {
 private:
  static LogTagSet* _list;
  static size_t _ntagsets;

  LogTagSet* const _next;
  size_t _ntags;
  LogTagType _tag[LogTag::MaxTags];

  LogOutputList _output_list;
  LogDecorators _decorators;

  typedef size_t (*PrefixWriter)(char* buf, size_t size);
  PrefixWriter _write_prefix;

  // Keep constructor private to prevent incorrect instantiations of this class.
  // Only LogTagSetMappings can create/contain instances of this class.
  // The constructor links all tagsets together in a global list of tagsets.
  // This list is used during configuration to be able to update all tagsets
  // and their configurations to reflect the new global log configuration.
  LogTagSet(PrefixWriter prefix_writer, LogTagType t0, LogTagType t1, LogTagType t2, LogTagType t3, LogTagType t4);

  template <LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
  friend class LogTagSetMapping;

 public:
  static void describe_tagsets(outputStream* out);
  static void list_all_tagsets(outputStream* out);

  void wait_until_no_readers() const {
    _output_list.wait_until_no_readers();
  }

  static LogTagSet* first() {
    return _list;
  }

  static size_t ntagsets() {
    return _ntagsets;
  }

  LogTagSet* next() {
    return _next;
  }

  size_t ntags() const {
    return _ntags;
  }

  LogTagType tag(size_t idx) const {
    return _tag[idx];
  }

  bool contains(LogTagType tag) const {
    for (size_t i = 0; i < LogTag::MaxTags && _tag[i] != LogTag::__NO_TAG; i++) {
      if (tag == _tag[i]) {
        return true;
      }
    }
    return false;
  }

  LogLevelType level_for(const LogOutput* output) const {
    return _output_list.level_for(output);
  }

  void disable_outputs() {
    _output_list.clear();
  }

  void set_output_level(LogOutput* output, LogLevelType level) {
    _output_list.set_output_level(output, level);
  }

  // Refresh the decorators for this tagset to contain the decorators for all
  // of its current outputs combined with the given decorators.
  void update_decorators(const LogDecorators& decorator = LogDecorators::None);

  void label(outputStream* st, const char* separator = ",") const;
  int label(char *buf, size_t len, const char* separator = ",") const;
  bool has_output(const LogOutput* output);

  // The implementation of this function is put here to ensure
  // that it is inline:able by the log_is_enabled(level, ...) macro.
  bool is_level(LogLevelType level) const {
    return _output_list.is_level(level);
  }
  void log(LogLevelType level, const char* msg);
  void log(const LogMessageBuffer& msg);

  ATTRIBUTE_PRINTF(3, 4)
  void write(LogLevelType level, const char* fmt, ...);

  template <LogLevelType Level>
  ATTRIBUTE_PRINTF(2, 3)
  void write(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vwrite(Level, fmt, args);
    va_end(args);
  }

  ATTRIBUTE_PRINTF(3, 0)
  void vwrite(LogLevelType level, const char* fmt, va_list args);
};

template <LogTagType T0, LogTagType T1 = LogTag::__NO_TAG, LogTagType T2 = LogTag::__NO_TAG,
          LogTagType T3 = LogTag::__NO_TAG, LogTagType T4 = LogTag::__NO_TAG,
          LogTagType GuardTag = LogTag::__NO_TAG>
class LogTagSetMapping : public AllStatic {
private:
  // Verify number of logging tags does not exceed maximum supported.
  STATIC_ASSERT(GuardTag == LogTag::__NO_TAG);
  static LogTagSet _tagset;

public:
  static LogTagSet& tagset() {
    return _tagset;
  }
};

// Instantiate the static field _tagset for all tagsets that are used for logging somewhere.
// (This must be done here rather than the .cpp file because it's a template.)
// Each combination of tags used as template arguments to the Log class somewhere (via macro or not)
// will instantiate the LogTagSetMapping template, which in turn creates the static field for that
// tagset. This _tagset contains the configuration for those tags.
template <LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
LogTagSet LogTagSetMapping<T0, T1, T2, T3, T4, GuardTag>::_tagset(&LogPrefix<T0, T1, T2, T3, T4>::prefix, T0, T1, T2, T3, T4);

extern const size_t vwrite_buffer_size;
#endif // SHARE_LOGGING_LOGTAGSET_HPP
