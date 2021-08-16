/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUP_HPP
#define SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUP_HPP

// String Deduplication
//
// String deduplication aims to reduce the heap live-set by modifying equal
// instances of java.lang.String so they share the same backing byte array
// (the String's value).
//
// The deduplication process is divided in two main parts, 1) finding the
// objects to deduplicate, and 2) deduplicating those objects.
//
// The first part is done as part of a normal GC cycle when objects are
// marked or evacuated. At this time a check is applied on each object to
// determine whether it is a candidate for deduplication.  Candidates are
// added to the set of deduplication requests for later processing.
//
// The second part, processing the deduplication requests, is a concurrent
// phase.  This phase is executed by the deduplication thread, which takes
// candidates from the set of requests and tries to deduplicate them.
//
// A deduplication table is used to keep track of unique byte arrays used by
// String objects.  When deduplicating, a lookup is made in this table to
// see if there is already an equivalent byte array that was used by some
// other String.  If so, the String object is adjusted to point to that byte
// array, and the original array is released, allowing it to eventually be
// garbage collected.  If the lookup fails the byte array is instead
// inserted into the table so it can potentially be shared with other
// Strings in the future.
//
// The set of requests uses entries from a pair of weak OopStorage objects.
// One is used for requests, the other is being processed.  When processing
// completes, the roles of the storage objects are exchanged.  The GC adds
// entries referring to discovered candidates, allocating new OopStorage
// entries for the requests.  The deduplication processing thread does a
// concurrent iteration over the processing storage, deduplicating the
// Strings and releasing the OopStorage entries.  Two storage objects are
// used so there isn't any conflict between adding and removing entries by
// different threads.
//
// The deduplication table uses entries from another weak OopStorage to hold
// the byte arrays.  This permits reclamation of arrays that become unused.
// This is separate from the request storage objects because dead count
// tracking is used by the table implementation as part of resizing
// decisions and for deciding when to cleanup dead entries in the table.
// The usage pattern for the table is also very different from that of the
// request storages.  The request/processing storages are used in a way that
// supports bulk allocation and release of entries.
//
// Candidate selection criteria is GC specific.  This class provides some
// helper functions that may be of use when implementing candidate
// selection.
//
// Strings interned in the StringTable require special handling.  Once a
// String has been added to the StringTable, its byte array must not change.
// Doing so would counteract C2 optimizations on string literals.  But an
// interned string might later become a deduplication candidate through the
// normal GC discovery mechanism.  To prevent such modifications, the
// deduplication_forbidden flag of a String is set before interning it.  A
// String with that flag set may have its byte array added to the
// deduplication table, but will not have its byte array replaced by a
// different but equivalent array from the table.
//
// A GC must opt-in to support string deduplication. This primarily involves
// making deduplication requests. As the GC is processing objects it must
// determine which are candidates for deduplication, and add those objects
// to StringDedup::Requests objects. Typically, each GC marking/evacuation
// thread has its own Requests object. Once liveness analysis is complete,
// but before weak reference processing, the GC should flush or delete all
// of its Requests objects.
//
// For additional information on string deduplication, please see JEP 192,
// http://openjdk.java.net/jeps/192

#include "memory/allStatic.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/globalDefinitions.hpp"

class Klass;
class ThreadClosure;

// The StringDedup class provides the API for the deduplication mechanism.
// StringDedup::Requests and the StringDedup functions for candidate testing
// are all that a GC needs to use to support the string deduplication
// feature.  Other functions in the StringDedup class are called where
// needed, without requiring GC-specific code.
class StringDedup : public AllStatic {
  class Config;
  class Processor;
  class Stat;
  class StorageUse;
  class Table;

  static bool _initialized;
  static bool _enabled;

  static Processor* _processor;
  static Stat _cur_stat;
  static Stat _total_stat;

  static const Klass* _string_klass_or_null;
  static uint _enabled_age_threshold;
  static uint _enabled_age_limit;

public:
  class Requests;

  // Initialize and check command line arguments.
  // Returns true if configuration is valid, false otherwise.
  static bool ergo_initialize();

  // Initialize deduplication if enabled by command line arguments.
  static void initialize();

  // Returns true if string deduplication is enabled.
  static bool is_enabled() { return _enabled; }

  // Stop the deduplication processor thread.
  // precondition: is_enabled()
  static void stop();

  // Visit the deduplication processor thread.
  // precondition: is_enabled()
  static void threads_do(ThreadClosure* tc);

  // Marks the String as not being subject to deduplication.  This can be
  // used to prevent deduplication of Strings whose value array must remain
  // stable and cannot be replaced by a shared duplicate.  Must be called
  // before obtaining the value array; this function provides an acquire
  // barrier.
  // precondition: is_enabled()
  // precondition: java_string is a Java String object.
  static void forbid_deduplication(oop java_string);

  // Notify that a String is being added to the StringTable.
  // Implicity forbids deduplication of the String.
  // precondition: is_enabled()
  // precondition: java_string is a Java String object.
  static void notify_intern(oop java_string);

  // precondition: at safepoint
  static void verify();

  // Some predicates for use in testing whether an object is a candidate for
  // deduplication.  These functions combine an implicit is_enabled check
  // with another check in a single comparison.

  // Return true if k is String klass and deduplication is enabled.
  static bool is_enabled_string(const Klass* k) {
    return k == _string_klass_or_null;
  }

  // Return true if age == StringDeduplicationAgeThreshold and
  // deduplication is enabled.
  static bool is_threshold_age(uint age) {
    // Threshold is from option if enabled, or an impossible value (exceeds
    // markWord::max_age) if disabled.
    return age == _enabled_age_threshold;
  }

  // Return true if age < StringDeduplicationAgeThreshold and
  // deduplication is enabled.
  static bool is_below_threshold_age(uint age) {
    // Limit is from option if enabled, or 0 if disabled.
    return age < _enabled_age_limit;
  }
};

// GC requests for String deduplication.
//
// Each marking thread should have it's own Requests object.  When marking
// is completed the Requests object must be flushed (either explicitly or by
// the destructor).
class StringDedup::Requests {
  StorageUse* _storage_for_requests;
  oop** _buffer;
  size_t _index;
  bool _refill_failed;

  bool refill_buffer();

public:
  Requests();
  ~Requests();                  // Calls flush().

  // Request deduplication of java_string.
  // prerequisite: StringDedup::is_enabled()
  // prerequisite: java_string is a Java String
  void add(oop java_string);

  // Flush any buffered deduplication requests and release resources
  // used by this object.
  void flush();
};

#endif // SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUP_HPP
