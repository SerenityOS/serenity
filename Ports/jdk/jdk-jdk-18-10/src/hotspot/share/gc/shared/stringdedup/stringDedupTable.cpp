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

#include "precompiled.hpp"
#include "classfile/altHashing.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shared/stringdedup/stringDedupConfig.hpp"
#include "gc/shared/stringdedup/stringDedupStat.hpp"
#include "gc/shared/stringdedup/stringDedupTable.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "oops/access.hpp"
#include "oops/oopsHierarchy.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "oops/weakHandle.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"

//////////////////////////////////////////////////////////////////////////////
// StringDedup::Table::Bucket
//
// A bucket is a pair of vectors, one containing hash codes, the other
// containing values.  An "entry" is a corresponding pair of elements from
// the vectors.  The size of the table is the size of either vector.
//
// The capacity of the vectors is explicitly controlled, based on the size.
// Given N > 0 and 2^N <= size < 2^(N+1), then capacity = 2^N + k * 2^(N-1)
// for the smallest integer k in [0,2] such that size <= capacity.  That is,
// use a power of 2 or the midpoint between consecutive powers of 2 that is
// minimally at least size.
//
// The main benefit of this representation is that it uses less space than a
// more traditional linked-list of entry nodes representation.  Such a
// representation requires 24 bytes per entry (64 bit platform) for the next
// pointer (8 bytes), the value (8 bytes), and the hash code (4 bytes, but
// padded to 8 because of alignment requirements).  The pair of vectors uses
// 12 bytes per entry, but has overhead for excess capacity so that adding
// an entry takes amortized constant time.  That excess capacity increases
// the per entry storage requirement, but it's still better than the linked
// list representation.
//
// The per-bucket cost of a pair of vectors is higher than having a bucket
// be the head of a linked list of nodes.  We ameliorate this by allowing
// buckets to be somewhat longer than is usually desired for a hashtable.
// The lookup performance for string deduplication is not that critical, and
// searching a vector of hash codes of moderate length should be pretty
// fast.  By using a good hash function, having different values hash to the
// same hash code should be uncommon, making the part of the search of a
// bucket for a given hash code more effective.
//
// The reason to record the hash codes with the values is that comparisons
// are expensive, and recomputing the hash code when resizing is also
// expensive.  A closed hashing implementation with just the values would be
// more space efficient.

class StringDedup::Table::Bucket {
  GrowableArrayCHeap<uint, mtStringDedup> _hashes;
  GrowableArrayCHeap<TableValue, mtStringDedup> _values;

  void adjust_capacity(int new_capacity);
  void expand_if_full();

public:
  // precondition: reserve == 0 or is the result of needed_capacity.
  Bucket(int reserve = 0);

  ~Bucket() {
    while (!_values.is_empty()) {
      _values.pop().release(_table_storage);
    }
  }

  static int needed_capacity(int size);

  const GrowableArrayView<uint>& hashes() const { return _hashes; }
  const GrowableArrayView<TableValue>& values() const { return _values; }

  bool is_empty() const { return _hashes.length() == 0; }
  int length() const { return _hashes.length(); }

  void add(uint hash_code, TableValue value) {
    expand_if_full();
    _hashes.push(hash_code);
    _values.push(value);
  }

  void delete_at(int index) {
    _values.at(index).release(_table_storage);
    _hashes.delete_at(index);
    _values.delete_at(index);
  }

  void pop_norelease() {
    _hashes.pop();
    _values.pop();
  }

  void shrink();

  TableValue find(typeArrayOop obj, uint hash_code) const;

  void verify(size_t bucket_index, size_t bucket_count) const;
};

StringDedup::Table::Bucket::Bucket(int reserve) :
  _hashes(reserve), _values(reserve)
{
  assert(reserve == needed_capacity(reserve),
         "reserve %d not computed properly", reserve);
}

// Choose the least power of 2 or half way between two powers of 2,
// such that number of entries <= target.
int StringDedup::Table::Bucket::needed_capacity(int needed) {
  if (needed == 0) return 0;
  int high = round_up_power_of_2(needed);
  int low = high - high/4;
  return (needed <= low) ? low : high;
}

void StringDedup::Table::Bucket::adjust_capacity(int new_capacity) {
  GrowableArrayCHeap<uint, mtStringDedup> new_hashes{new_capacity};
  GrowableArrayCHeap<TableValue, mtStringDedup> new_values{new_capacity};
  while (!_hashes.is_empty()) {
    new_hashes.push(_hashes.pop());
    new_values.push(_values.pop());
  }
  _hashes.swap(&new_hashes);
  _values.swap(&new_values);
}

void StringDedup::Table::Bucket::expand_if_full() {
  if (_hashes.length() == _hashes.max_length()) {
    adjust_capacity(needed_capacity(_hashes.max_length() + 1));
  }
}

void StringDedup::Table::Bucket::shrink() {
  if (_hashes.is_empty()) {
    _hashes.clear_and_deallocate();
    _values.clear_and_deallocate();
  } else {
    int target = needed_capacity(_hashes.length());
    if (target < _hashes.max_length()) {
      adjust_capacity(target);
    }
  }
}

StringDedup::Table::TableValue
StringDedup::Table::Bucket::find(typeArrayOop obj, uint hash_code) const {
  int index = 0;
  for (uint cur_hash : _hashes) {
    if (cur_hash == hash_code) {
      typeArrayOop value = cast_from_oop<typeArrayOop>(_values.at(index).peek());
      if ((value != nullptr) &&
          java_lang_String::value_equals(obj, value)) {
        return _values.at(index);
      }
    }
    ++index;
  }
  return TableValue();
}

void StringDedup::Table::Bucket::verify(size_t bucket_index,
                                        size_t bucket_count) const {
  int entry_count = _hashes.length();
  guarantee(entry_count == _values.length(),
            "hash/value length mismatch: %zu: %d, %d",
            bucket_index, entry_count, _values.length());
  for (uint hash_code : _hashes) {
    size_t hash_index = hash_code % bucket_count;
    guarantee(bucket_index == hash_index,
              "entry in wrong bucket: %zu, %u", bucket_index, hash_code);
  }
  size_t index = 0;
  for (TableValue tv : _values) {
    guarantee(!tv.is_empty(), "entry missing value: %zu:%zu", bucket_index, index);
    const oop* p = tv.ptr_raw();
    OopStorage::EntryStatus status = _table_storage->allocation_status(p);
    guarantee(OopStorage::ALLOCATED_ENTRY == status,
              "bad value: %zu:%zu -> " PTR_FORMAT, bucket_index, index, p2i(p));
    // Don't check object is oop_or_null; duplicates OopStorage verify.
    ++index;
  }
}

//////////////////////////////////////////////////////////////////////////////
// Tracking dead entries
//
// Keeping track of the number of dead entries in a table is complicated by
// the possibility that a GC could be changing the set while we're removing
// dead entries.
//
// If a dead count report is received while cleaning, further cleaning may
// reduce the number of dead entries.  With STW reference processing one
// could maintain an accurate dead count by deducting cleaned entries.  But
// that doesn't work for concurrent reference processsing.  In that case the
// dead count being reported may include entries that have already been
// removed by concurrent cleaning.
//
// It seems worse to unnecessarily resize or clean than to delay either.  So
// we track whether the reported dead count is good, and only consider
// resizing or cleaning when we have a good idea of the benefit.

enum class StringDedup::Table::DeadState {
  // This is the initial state.  This state is also selected when a dead
  // count report is received and the state is wait1.  The reported dead
  // count is considered good.  It might be lower than actual because of an
  // in-progress concurrent reference processing.  It might also increase
  // immediately due to a new GC.  Oh well to both of those.
  good,
  // This state is selected when a dead count report is received and the
  // state is wait2.  Current value of dead count may be inaccurate because
  // of reference processing that was started before or during the most
  // recent cleaning and finished after.  Wait for the next report.
  wait1,
  // This state is selected when a cleaning operation completes. Current
  // value of dead count is inaccurate because we haven't had a report
  // since the last cleaning.
  wait2,
  // Currently cleaning the table.
  cleaning
};

void StringDedup::Table::num_dead_callback(size_t num_dead) {
  // Lock while modifying dead count and state.
  MonitorLocker ml(StringDedup_lock, Mutex::_no_safepoint_check_flag);

  switch (Atomic::load(&_dead_state)) {
  case DeadState::good:
    Atomic::store(&_dead_count, num_dead);
    break;

  case DeadState::wait1:
    // Set count first, so dedup thread gets this or a later value if it
    // sees the good state.
    Atomic::store(&_dead_count, num_dead);
    Atomic::release_store(&_dead_state, DeadState::good);
    break;

  case DeadState::wait2:
    Atomic::release_store(&_dead_state, DeadState::wait1);
    break;

  case DeadState::cleaning:
    break;
  }

  // Wake up a possibly sleeping dedup thread.  This callback is invoked at
  // the end of a GC, so there may be new requests waiting.
  ml.notify_all();
}

//////////////////////////////////////////////////////////////////////////////
// StringDedup::Table::CleanupState

class StringDedup::Table::CleanupState : public CHeapObj<mtStringDedup> {
  NONCOPYABLE(CleanupState);

protected:
  CleanupState() = default;

public:
  virtual ~CleanupState() = default;
  virtual bool step() = 0;
  virtual TableValue find(typeArrayOop obj, uint hash_code) const = 0;
  virtual void report_end() const = 0;
  virtual Stat::Phase phase() const = 0;
  virtual void verify() const = 0;
};

//////////////////////////////////////////////////////////////////////////////
// StringDedup::Table::Resizer

class StringDedup::Table::Resizer final : public CleanupState {
  Bucket* _buckets;
  size_t _number_of_buckets;
  size_t _bucket_index;
  size_t _shrink_index;

public:
  Resizer(bool grow_only, Bucket* buckets, size_t number_of_buckets) :
    _buckets(buckets),
    _number_of_buckets(number_of_buckets),
    _bucket_index(0),
    // Disable bucket shrinking if grow_only requested.
    _shrink_index(grow_only ? Table::_number_of_buckets : 0)
  {
    Table::_need_bucket_shrinking = !grow_only;
  }

  virtual ~Resizer() {
    free_buckets(_buckets, _number_of_buckets);
  }

  virtual bool step();

  virtual TableValue find(typeArrayOop obj, uint hash_code) const {
    return _buckets[hash_code % _number_of_buckets].find(obj, hash_code);
  }

  virtual void report_end() const {
    _cur_stat.report_resize_table_end();
  }

  virtual Stat::Phase phase() const {
    return Stat::Phase::resize_table;
  }

  virtual void verify() const;
};

bool StringDedup::Table::Resizer::step() {
  if (_bucket_index < _number_of_buckets) {
    Bucket& bucket = _buckets[_bucket_index];
    if (bucket.is_empty()) {
      bucket.shrink();          // Eagerly release old bucket memory.
      ++_bucket_index;
      return true;              // Continue transferring with next bucket.
    } else {
      uint hash_code = bucket.hashes().last();
      TableValue tv = bucket.values().last();
      bucket.pop_norelease();
      if (tv.peek() != nullptr) {
        Table::add(tv, hash_code);
      } else {
        tv.release(_table_storage);
        _cur_stat.inc_deleted();
      }
      return true;              // Continue transferring current bucket.
    }
  } else if (_shrink_index < Table::_number_of_buckets) {
    // When the new buckets were created, space was reserved based on the
    // expected number of entries per bucket.  But that might be off for any
    // given bucket.  Some will have exceeded that and have been grown as
    // needed by the insertions.  But some might be less and can be shrunk.
    Table::_buckets[_shrink_index++].shrink();
    return true;                // Continue shrinking with next bucket.
  } else {
    return false;               // All buckets transferred and shrunk, so done.
  }
}

void StringDedup::Table::Resizer::verify() const {
  for (size_t i = 0; i < _number_of_buckets; ++i) {
    _buckets[i].verify(i, _number_of_buckets);
  }
}

//////////////////////////////////////////////////////////////////////////////
// StringDedup::Table::Cleaner

class StringDedup::Table::Cleaner final : public CleanupState {
  size_t _bucket_index;
  int _entry_index;

public:
  Cleaner() : _bucket_index(0), _entry_index(0) {
    Table::_need_bucket_shrinking = false;
  }

  virtual ~Cleaner() = default;

  virtual bool step();

  virtual TableValue find(typeArrayOop obj, uint hash_code) const {
    return TableValue();
  }

  virtual void report_end() const {
    _cur_stat.report_cleanup_table_end();
  }

  virtual Stat::Phase phase() const {
    return Stat::Phase::cleanup_table;
  }

  virtual void verify() const {} // Nothing to do here.
};

bool StringDedup::Table::Cleaner::step() {
  if (_bucket_index == Table::_number_of_buckets) {
    return false;               // All buckets processed, so done.
  }
  Bucket& bucket = Table::_buckets[_bucket_index];
  const GrowableArrayView<TableValue>& values = bucket.values();
  assert(_entry_index <= values.length(), "invariant");
  if (_entry_index == values.length()) {
    // End of current bucket.  Shrink the bucket if oversized for current
    // usage, and continue at the start of the next bucket.
    bucket.shrink();
    ++_bucket_index;
    _entry_index = 0;
  } else if (values.at(_entry_index).peek() == nullptr) {
    // Current entry is dead.  Remove and continue at same index.
    bucket.delete_at(_entry_index);
    --Table::_number_of_entries;
    _cur_stat.inc_deleted();
  } else {
    // Current entry is live.  Continue with the next entry.
    ++_entry_index;
  }
  return true;
}

//////////////////////////////////////////////////////////////////////////////
// StringDedup::Table

OopStorage* StringDedup::Table::_table_storage;
StringDedup::Table::Bucket* StringDedup::Table::_buckets;
size_t StringDedup::Table::_number_of_buckets;
size_t StringDedup::Table::_number_of_entries = 0;
size_t StringDedup::Table::_grow_threshold;
StringDedup::Table::CleanupState* StringDedup::Table::_cleanup_state = nullptr;
bool StringDedup::Table::_need_bucket_shrinking = false;
volatile size_t StringDedup::Table::_dead_count = 0;
volatile StringDedup::Table::DeadState StringDedup::Table::_dead_state = DeadState::good;

void StringDedup::Table::initialize_storage() {
  assert(_table_storage == nullptr, "storage already created");
  _table_storage = OopStorageSet::create_weak("StringDedup Table Weak", mtStringDedup);
}

void StringDedup::Table::initialize() {
  size_t num_buckets = Config::initial_table_size();
  _buckets = make_buckets(num_buckets);
  _number_of_buckets = num_buckets;
  _grow_threshold = Config::grow_threshold(num_buckets);
  _table_storage->register_num_dead_callback(num_dead_callback);
}

StringDedup::Table::Bucket*
StringDedup::Table::make_buckets(size_t number_of_buckets, size_t reserve) {
  Bucket* buckets = NEW_C_HEAP_ARRAY(Bucket, number_of_buckets, mtStringDedup);
  for (size_t i = 0; i < number_of_buckets; ++i) {
    // Cast because GrowableArray uses int for sizes and such.
    ::new (&buckets[i]) Bucket(static_cast<int>(reserve));
  }
  return buckets;
}

void StringDedup::Table::free_buckets(Bucket* buckets, size_t number_of_buckets) {
  while (number_of_buckets > 0) {
    buckets[--number_of_buckets].~Bucket();
  }
  FREE_C_HEAP_ARRAY(Bucket, buckets);
}

// Compute the hash code for obj using halfsiphash_32.  As this is a high
// quality hash function that is resistant to hashtable flooding, very
// unbalanced bucket chains should be rare, and duplicate hash codes within
// a bucket should be very rare.
uint StringDedup::Table::compute_hash(typeArrayOop obj) {
  int length = obj->length();
  uint64_t hash_seed = Config::hash_seed();
  const uint8_t* data = static_cast<uint8_t*>(obj->base(T_BYTE));
  return AltHashing::halfsiphash_32(hash_seed, data, length);
}

size_t StringDedup::Table::hash_to_index(uint hash_code) {
  return hash_code % _number_of_buckets;
}

void StringDedup::Table::add(TableValue tv, uint hash_code) {
  _buckets[hash_to_index(hash_code)].add(hash_code, tv);
  ++_number_of_entries;
}

bool StringDedup::Table::is_dead_count_good_acquire() {
  return Atomic::load_acquire(&_dead_state) == DeadState::good;
}

// Should be consistent with cleanup_start_if_needed.
bool StringDedup::Table::is_grow_needed() {
  return is_dead_count_good_acquire() &&
         ((_number_of_entries - Atomic::load(&_dead_count)) > _grow_threshold);
}

// Should be consistent with cleanup_start_if_needed.
bool StringDedup::Table::is_dead_entry_removal_needed() {
  return is_dead_count_good_acquire() &&
         Config::should_cleanup_table(_number_of_entries, Atomic::load(&_dead_count));
}

StringDedup::Table::TableValue
StringDedup::Table::find(typeArrayOop obj, uint hash_code) {
  assert(obj != nullptr, "precondition");
  if (_cleanup_state != nullptr) {
    TableValue tv = _cleanup_state->find(obj, hash_code);
    if (!tv.is_empty()) return tv;
  }
  return _buckets[hash_to_index(hash_code)].find(obj, hash_code);
}

void StringDedup::Table::install(typeArrayOop obj, uint hash_code) {
  add(TableValue(_table_storage, obj), hash_code);
  _cur_stat.inc_new(obj->size() * HeapWordSize);
}

#if INCLUDE_CDS_JAVA_HEAP

// Try to look up the string's value array in the shared string table.  This
// is only worthwhile if sharing is enabled, both at build-time and at
// runtime.  But it's complicated because we can't trust the is_latin1 value
// of the string we're deduplicating.  GC requests can provide us with
// access to a String that is incompletely constructed; the value could be
// set before the coder.
bool StringDedup::Table::try_deduplicate_shared(oop java_string) {
  typeArrayOop value = java_lang_String::value(java_string);
  assert(value != nullptr, "precondition");
  assert(TypeArrayKlass::cast(value->klass())->element_type() == T_BYTE, "precondition");
  int length = value->length();
  static_assert(sizeof(jchar) == 2 * sizeof(jbyte), "invariant");
  assert(((length & 1) == 0) || CompactStrings, "invariant");
  if ((length & 1) == 0) {
    // If the length of the byte array is even, then the value array could be
    // either non-latin1 or a compact latin1 that happens to have an even length.
    // For the former case we want to look for a matching shared string.  But
    // for the latter we can still do a lookup, treating the value array as
    // non-latin1, and deduplicating if we find a match.  For deduplication we
    // only care if the arrays consist of the same sequence of bytes.
    const jchar* chars = static_cast<jchar*>(value->base(T_CHAR));
    oop found = StringTable::lookup_shared(chars, length >> 1);
    // If found is latin1, then it's byte array differs from the unicode
    // table key, so not actually a match to value.
    if ((found != nullptr) &&
        !java_lang_String::is_latin1(found) &&
        try_deduplicate_found_shared(java_string, found)) {
      return true;
    }
    // That didn't work.  Try as compact latin1.
  }
  // If not using compact strings then don't need to check further.
  if (!CompactStrings) return false;
  // Treat value as compact latin1 and try to deduplicate against that.
  // This works even if java_string is not latin1, but has a byte array with
  // the same sequence of bytes as a compact latin1 shared string.
  ResourceMark rm(Thread::current());
  jchar* chars = NEW_RESOURCE_ARRAY_RETURN_NULL(jchar, length);
  if (chars == nullptr) {
    _cur_stat.inc_skipped_shared();
    return true;
  }
  for (int i = 0; i < length; ++i) {
    chars[i] = value->byte_at(i) & 0xff;
  }
  oop found = StringTable::lookup_shared(chars, length);
  if (found == nullptr) return false;
  assert(java_lang_String::is_latin1(found), "invariant");
  return try_deduplicate_found_shared(java_string, found);
}

bool StringDedup::Table::try_deduplicate_found_shared(oop java_string, oop found) {
  _cur_stat.inc_known_shared();
  typeArrayOop found_value = java_lang_String::value(found);
  if (found_value == java_lang_String::value(java_string)) {
    // String's value already matches what's in the table.
    return true;
  } else if (deduplicate_if_permitted(java_string, found_value)) {
    // If java_string has the same coder as found then it won't have
    // deduplication_forbidden set; interning would have found the matching
    // shared string.  But if they have different coders but happen to have
    // the same sequence of bytes in their value arrays, then java_string
    // could have been interned and marked deduplication-forbidden.
    _cur_stat.inc_deduped(found_value->size() * HeapWordSize);
    return true;
  } else {
    // Must be a mismatch between java_string and found string encodings,
    // and java_string has been marked deduplication_forbidden, so is
    // (being) interned in the StringTable.  Return false to allow
    // additional processing that might still lead to some benefit for
    // deduplication.
    return false;
  }
}

#else // if !INCLUDE_CDS_JAVA_HEAP

bool StringDedup::Table::try_deduplicate_shared(oop java_string) {
  ShouldNotReachHere();         // Call is guarded.
  return false;
}

// Undefined because unreferenced.
// bool StringDedup::Table::try_deduplicate_found_shared(oop java_string, oop found);

#endif // INCLUDE_CDS_JAVA_HEAP

bool StringDedup::Table::deduplicate_if_permitted(oop java_string,
                                                  typeArrayOop value) {
  // The non-dedup check and value assignment must be under lock.
  MutexLocker ml(StringDedupIntern_lock, Mutex::_no_safepoint_check_flag);
  if (java_lang_String::deduplication_forbidden(java_string)) {
    return false;
  } else {
    java_lang_String::set_value(java_string, value); // Dedup!
    return true;
  }
}

void StringDedup::Table::deduplicate(oop java_string) {
  assert(java_lang_String::is_instance(java_string), "precondition");
  _cur_stat.inc_inspected();
  if ((StringTable::shared_entry_count() > 0) &&
      try_deduplicate_shared(java_string)) {
    return;                     // Done if deduplicated against shared StringTable.
  }
  typeArrayOop value = java_lang_String::value(java_string);
  uint hash_code = compute_hash(value);
  TableValue tv = find(value, hash_code);
  if (tv.is_empty()) {
    // Not in table.  Create a new table entry.
    install(value, hash_code);
  } else {
    _cur_stat.inc_known();
    typeArrayOop found = cast_from_oop<typeArrayOop>(tv.resolve());
    assert(found != nullptr, "invariant");
    // Deduplicate if value array differs from what's in the table.
    if (found != value) {
      if (deduplicate_if_permitted(java_string, found)) {
        _cur_stat.inc_deduped(found->size() * HeapWordSize);
      } else {
        // If string marked deduplication_forbidden then we can't update its
        // value.  Instead, replace the array in the table with the new one,
        // as java_string is probably in the StringTable.  That makes it a
        // good target for future deduplications as it is probably intended
        // to live for some time.
        tv.replace(value);
        _cur_stat.inc_replaced();
      }
    }
  }
}

bool StringDedup::Table::cleanup_start_if_needed(bool grow_only, bool force) {
  assert(_cleanup_state == nullptr, "cleanup already in progress");
  if (!is_dead_count_good_acquire()) return false;
  // If dead count is good then we can read it once and use it below
  // without needing any locking.  The recorded count could increase
  // after the read, but that's okay.
  size_t dead_count = Atomic::load(&_dead_count);
  // This assertion depends on dead state tracking.  Otherwise, concurrent
  // reference processing could detect some, but a cleanup operation could
  // remove them before they are reported.
  assert(dead_count <= _number_of_entries, "invariant");
  size_t adjusted = _number_of_entries - dead_count;
  if (force || Config::should_grow_table(_number_of_buckets, adjusted)) {
    return start_resizer(grow_only, adjusted);
  } else if (grow_only) {
    return false;
  } else if (Config::should_shrink_table(_number_of_buckets, adjusted)) {
    return start_resizer(false /* grow_only */, adjusted);
  } else if (_need_bucket_shrinking ||
             Config::should_cleanup_table(_number_of_entries, dead_count)) {
    // Remove dead entries and shrink buckets if needed.
    return start_cleaner(_number_of_entries, dead_count);
  } else {
    // No cleanup needed.
    return false;
  }
}

void StringDedup::Table::set_dead_state_cleaning() {
  MutexLocker ml(StringDedup_lock, Mutex::_no_safepoint_check_flag);
  Atomic::store(&_dead_count, size_t(0));
  Atomic::store(&_dead_state, DeadState::cleaning);
}

bool StringDedup::Table::start_resizer(bool grow_only, size_t number_of_entries) {
  size_t new_size = Config::desired_table_size(number_of_entries);
  _cur_stat.report_resize_table_start(new_size, _number_of_buckets, number_of_entries);
  _cleanup_state = new Resizer(grow_only, _buckets, _number_of_buckets);
  size_t reserve = Bucket::needed_capacity(checked_cast<int>(number_of_entries / new_size));
  _buckets = make_buckets(new_size, reserve);
  _number_of_buckets = new_size;
  _number_of_entries = 0;
  _grow_threshold = Config::grow_threshold(new_size);
  set_dead_state_cleaning();
  return true;
}

bool StringDedup::Table::start_cleaner(size_t number_of_entries, size_t dead_count) {
  _cur_stat.report_cleanup_table_start(number_of_entries, dead_count);
  _cleanup_state = new Cleaner();
  set_dead_state_cleaning();
  return true;
}

bool StringDedup::Table::cleanup_step() {
  assert(_cleanup_state != nullptr, "precondition");
  return _cleanup_state->step();
}

void StringDedup::Table::cleanup_end() {
  assert(_cleanup_state != nullptr, "precondition");
  _cleanup_state->report_end();
  delete _cleanup_state;
  _cleanup_state = nullptr;
  MutexLocker ml(StringDedup_lock, Mutex::_no_safepoint_check_flag);
  Atomic::store(&_dead_state, DeadState::wait2);
}

StringDedup::Stat::Phase StringDedup::Table::cleanup_phase() {
  assert(_cleanup_state != nullptr, "precondition");
  return _cleanup_state->phase();
}

void StringDedup::Table::verify() {
  size_t total_count = 0;
  for (size_t i = 0; i < _number_of_buckets; ++i) {
    _buckets[i].verify(i, _number_of_buckets);
    total_count += _buckets[i].length();
  }
  guarantee(total_count == _number_of_entries,
            "number of values mismatch: %zu counted, %zu recorded",
            total_count, _number_of_entries);
  if (_cleanup_state != nullptr) {
    _cleanup_state->verify();
  }
}

void StringDedup::Table::log_statistics() {
  size_t dead_count;
  int dead_state;
  {
    MutexLocker ml(StringDedup_lock, Mutex::_no_safepoint_check_flag);
    dead_count = _dead_count;
    dead_state = static_cast<int>(_dead_state);
  }
  log_debug(stringdedup)("Table: %zu values in %zu buckets, %zu dead (%d)",
                         _number_of_entries, _number_of_buckets,
                         dead_count, dead_state);
  LogStreamHandle(Trace, stringdedup) log;
  if (log.is_enabled()) {
    ResourceMark rm;
    GrowableArray<size_t> counts;
    for (size_t i = 0; i < _number_of_buckets; ++i) {
      int length = _buckets[i].length();
      size_t count = counts.at_grow(length);
      counts.at_put(length, count + 1);
    }
    log.print_cr("Table bucket distribution:");
    for (int i = 0; i < counts.length(); ++i) {
      size_t count = counts.at(i);
      if (count != 0) {
        log.print_cr("  %4d: %zu", i, count);
      }
    }
  }
}
