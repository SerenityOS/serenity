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
#ifndef SHARE_LOGGING_LOGOUTPUTLIST_HPP
#define SHARE_LOGGING_LOGOUTPUTLIST_HPP

#include "logging/logLevel.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class LogOutput;

// Data structure to keep track of log outputs for a given tagset.
// Essentially a sorted linked list going from error level outputs
// to outputs of finer levels. Keeps an index from each level to
// the first node in the list for the corresponding level.
// This allows a log message on, for example, info level to jump
// straight into the list where the first info level output can
// be found. The log message will then be printed on that output,
// as well as all outputs in nodes that follow in the list (which
// can be additional info level outputs and/or debug and trace outputs).
//
// Each instance keeps track of the number of current readers of the list.
// To remove a node from the list the node must first be unlinked,
// and the memory for that node can be freed whenever the removing
// thread observes an active reader count of 0 (after unlinking it).
class LogOutputList {
 private:
  struct LogOutputNode : public CHeapObj<mtLogging> {
    LogOutput*      _value;
    LogOutputNode*  _next;
    LogLevelType    _level;
  };

  LogOutputNode*  _level_start[LogLevel::Count];
  volatile jint   _active_readers;

  LogOutputNode* find(const LogOutput* output) const;
  void remove_output(LogOutputNode* node);
  void add_output(LogOutput* output, LogLevelType level);
  void update_output_level(LogOutputNode* node, LogLevelType level);

  // Bookkeeping functions to keep track of number of active readers/iterators for the list.
  jint increase_readers();
  jint decrease_readers();

 public:
  LogOutputList() : _active_readers(0) {
    for (size_t i = 0; i < LogLevel::Count; i++) {
      _level_start[i] = NULL;
    }
  }

  // Test if the outputlist has an output for the given level.
  bool is_level(LogLevelType level) const {
    return _level_start[level] != NULL;
  }

  LogLevelType level_for(const LogOutput* output) const {
    LogOutputNode* node = this->find(output);
    if (node == NULL) {
      return LogLevel::Off;
    }
    return node->_level;
  }

  // Set (add/update/remove) the output to the specified level.
  void set_output_level(LogOutput* output, LogLevelType level);

  // Removes all outputs. Equivalent of set_output_level(out, Off)
  // for all outputs.
  void clear();
  void wait_until_no_readers() const;

  class Iterator {
    friend class LogOutputList;
   private:
    LogOutputNode*  _current;
    LogOutputList*  _list;
    Iterator(LogOutputList* list, LogOutputNode* start) : _current(start), _list(list) {
    }

   public:
    Iterator(const Iterator &itr) : _current(itr._current), _list(itr._list){
      itr._list->increase_readers();
    }

    Iterator& operator=(const Iterator& rhs) {
      _current = rhs._current;
      if (_list != rhs._list) {
        rhs._list->increase_readers();
        _list->decrease_readers();
        _list = rhs._list;
      }
      return *this;
    }

    ~Iterator() {
      _list->decrease_readers();
    }

    LogOutput* operator*() {
      return _current->_value;
    }

    void operator++(int) {
      _current = _current->_next;
    }

    bool operator!=(const LogOutputNode *ref) const {
      return _current != ref;
    }

    LogLevelType level() const {
      return _current->_level;
    }
  };

  Iterator iterator(LogLevelType level = LogLevel::Last) {
    increase_readers();
    return Iterator(this, _level_start[level]);
  }

  LogOutputNode* end() const {
    return NULL;
  }
};

#endif // SHARE_LOGGING_LOGOUTPUTLIST_HPP
