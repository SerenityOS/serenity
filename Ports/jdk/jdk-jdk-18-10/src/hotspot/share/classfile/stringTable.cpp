/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/archiveBuilder.hpp"
#include "cds/heapShared.inline.hpp"
#include "classfile/altHashing.hpp"
#include "classfile/compactHashtable.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/vmClasses.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "oops/weakHandle.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/timerTrace.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "services/diagnosticCommand.hpp"
#include "utilities/concurrentHashTable.inline.hpp"
#include "utilities/concurrentHashTableTasks.inline.hpp"
#include "utilities/macros.hpp"
#include "utilities/resizeableResourceHash.hpp"
#include "utilities/utf8.hpp"

// We prefer short chains of avg 2
const double PREF_AVG_LIST_LEN = 2.0;
// 2^24 is max size
const size_t END_SIZE = 24;
// If a chain gets to 100 something might be wrong
const size_t REHASH_LEN = 100;
// If we have as many dead items as 50% of the number of bucket
const double CLEAN_DEAD_HIGH_WATER_MARK = 0.5;

#if INCLUDE_CDS_JAVA_HEAP
inline oop read_string_from_compact_hashtable(address base_address, u4 offset) {
  assert(sizeof(narrowOop) == sizeof(offset), "must be");
  narrowOop v = CompressedOops::narrow_oop_cast(offset);
  return HeapShared::decode_from_archive(v);
}

static CompactHashtable<
  const jchar*, oop,
  read_string_from_compact_hashtable,
  java_lang_String::equals
> _shared_table;
#endif

// --------------------------------------------------------------------------

typedef ConcurrentHashTable<StringTableConfig, mtSymbol> StringTableHash;
static StringTableHash* _local_table = NULL;

volatile bool StringTable::_has_work = false;
volatile bool StringTable::_needs_rehashing = false;
OopStorage*   StringTable::_oop_storage;

static size_t _current_size = 0;
static volatile size_t _items_count = 0;

volatile bool _alt_hash = false;
static uint64_t _alt_hash_seed = 0;

uintx hash_string(const jchar* s, int len, bool useAlt) {
  return  useAlt ?
    AltHashing::halfsiphash_32(_alt_hash_seed, s, len) :
    java_lang_String::hash_code(s, len);
}

class StringTableConfig : public StackObj {
 private:
 public:
  typedef WeakHandle Value;

  static uintx get_hash(Value const& value, bool* is_dead) {
    oop val_oop = value.peek();
    if (val_oop == NULL) {
      *is_dead = true;
      return 0;
    }
    *is_dead = false;
    ResourceMark rm;
    // All String oops are hashed as unicode
    int length;
    jchar* chars = java_lang_String::as_unicode_string_or_null(val_oop, length);
    if (chars != NULL) {
      return hash_string(chars, length, _alt_hash);
    }
    vm_exit_out_of_memory(length, OOM_MALLOC_ERROR, "get hash from oop");
    return 0;
  }
  // We use default allocation/deallocation but counted
  static void* allocate_node(void* context, size_t size, Value const& value) {
    StringTable::item_added();
    return AllocateHeap(size, mtSymbol);
  }
  static void free_node(void* context, void* memory, Value const& value) {
    value.release(StringTable::_oop_storage);
    FreeHeap(memory);
    StringTable::item_removed();
  }
};

class StringTableLookupJchar : StackObj {
 private:
  Thread* _thread;
  uintx _hash;
  int _len;
  const jchar* _str;
  Handle _found;

 public:
  StringTableLookupJchar(Thread* thread, uintx hash, const jchar* key, int len)
    : _thread(thread), _hash(hash), _len(len), _str(key) {
  }
  uintx get_hash() const {
    return _hash;
  }
  bool equals(WeakHandle* value, bool* is_dead) {
    oop val_oop = value->peek();
    if (val_oop == NULL) {
      // dead oop, mark this hash dead for cleaning
      *is_dead = true;
      return false;
    }
    bool equals = java_lang_String::equals(val_oop, _str, _len);
    if (!equals) {
      return false;
    }
    // Need to resolve weak handle and Handleize through possible safepoint.
     _found = Handle(_thread, value->resolve());
    return true;
  }
};

class StringTableLookupOop : public StackObj {
 private:
  Thread* _thread;
  uintx _hash;
  Handle _find;
  Handle _found;  // Might be a different oop with the same value that's already
                  // in the table, which is the point.
 public:
  StringTableLookupOop(Thread* thread, uintx hash, Handle handle)
    : _thread(thread), _hash(hash), _find(handle) { }

  uintx get_hash() const {
    return _hash;
  }

  bool equals(WeakHandle* value, bool* is_dead) {
    oop val_oop = value->peek();
    if (val_oop == NULL) {
      // dead oop, mark this hash dead for cleaning
      *is_dead = true;
      return false;
    }
    bool equals = java_lang_String::equals(_find(), val_oop);
    if (!equals) {
      return false;
    }
    // Need to resolve weak handle and Handleize through possible safepoint.
    _found = Handle(_thread, value->resolve());
    return true;
  }
};

static size_t ceil_log2(size_t val) {
  size_t ret;
  for (ret = 1; ((size_t)1 << ret) < val; ++ret);
  return ret;
}

void StringTable::create_table() {
  size_t start_size_log_2 = ceil_log2(StringTableSize);
  _current_size = ((size_t)1) << start_size_log_2;
  log_trace(stringtable)("Start size: " SIZE_FORMAT " (" SIZE_FORMAT ")",
                         _current_size, start_size_log_2);
  _local_table = new StringTableHash(start_size_log_2, END_SIZE, REHASH_LEN);
  _oop_storage = OopStorageSet::create_weak("StringTable Weak", mtSymbol);
  _oop_storage->register_num_dead_callback(&gc_notification);
}

size_t StringTable::item_added() {
  return Atomic::add(&_items_count, (size_t)1);
}

void StringTable::item_removed() {
  Atomic::add(&_items_count, (size_t)-1);
}

double StringTable::get_load_factor() {
  return double(_items_count)/double(_current_size);
}

double StringTable::get_dead_factor(size_t num_dead) {
  return double(num_dead)/double(_current_size);
}

size_t StringTable::table_size() {
  return ((size_t)1) << _local_table->get_size_log2(Thread::current());
}

void StringTable::trigger_concurrent_work() {
  MutexLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
  Atomic::store(&_has_work, true);
  Service_lock->notify_all();
}

// Probing
oop StringTable::lookup(Symbol* symbol) {
  ResourceMark rm;
  int length;
  jchar* chars = symbol->as_unicode(length);
  return lookup(chars, length);
}

oop StringTable::lookup(const jchar* name, int len) {
  unsigned int hash = java_lang_String::hash_code(name, len);
  oop string = lookup_shared(name, len, hash);
  if (string != NULL) {
    return string;
  }
  if (_alt_hash) {
    hash = hash_string(name, len, true);
  }
  return do_lookup(name, len, hash);
}

class StringTableGet : public StackObj {
  Thread* _thread;
  Handle  _return;
 public:
  StringTableGet(Thread* thread) : _thread(thread) {}
  void operator()(WeakHandle* val) {
    oop result = val->resolve();
    assert(result != NULL, "Result should be reachable");
    _return = Handle(_thread, result);
  }
  oop get_res_oop() {
    return _return();
  }
};

oop StringTable::do_lookup(const jchar* name, int len, uintx hash) {
  Thread* thread = Thread::current();
  StringTableLookupJchar lookup(thread, hash, name, len);
  StringTableGet stg(thread);
  bool rehash_warning;
  _local_table->get(thread, lookup, stg, &rehash_warning);
  update_needs_rehash(rehash_warning);
  return stg.get_res_oop();
}

// Interning
oop StringTable::intern(Symbol* symbol, TRAPS) {
  if (symbol == NULL) return NULL;
  ResourceMark rm(THREAD);
  int length;
  jchar* chars = symbol->as_unicode(length);
  Handle string;
  oop result = intern(string, chars, length, CHECK_NULL);
  return result;
}

oop StringTable::intern(oop string, TRAPS) {
  if (string == NULL) return NULL;
  ResourceMark rm(THREAD);
  int length;
  Handle h_string (THREAD, string);
  jchar* chars = java_lang_String::as_unicode_string(string, length,
                                                     CHECK_NULL);
  oop result = intern(h_string, chars, length, CHECK_NULL);
  return result;
}

oop StringTable::intern(const char* utf8_string, TRAPS) {
  if (utf8_string == NULL) return NULL;
  ResourceMark rm(THREAD);
  int length = UTF8::unicode_length(utf8_string);
  jchar* chars = NEW_RESOURCE_ARRAY(jchar, length);
  UTF8::convert_to_unicode(utf8_string, chars, length);
  Handle string;
  oop result = intern(string, chars, length, CHECK_NULL);
  return result;
}

oop StringTable::intern(Handle string_or_null_h, const jchar* name, int len, TRAPS) {
  // shared table always uses java_lang_String::hash_code
  unsigned int hash = java_lang_String::hash_code(name, len);
  oop found_string = lookup_shared(name, len, hash);
  if (found_string != NULL) {
    return found_string;
  }
  if (_alt_hash) {
    hash = hash_string(name, len, true);
  }
  found_string = do_lookup(name, len, hash);
  if (found_string != NULL) {
    return found_string;
  }
  return do_intern(string_or_null_h, name, len, hash, THREAD);
}

oop StringTable::do_intern(Handle string_or_null_h, const jchar* name,
                           int len, uintx hash, TRAPS) {
  HandleMark hm(THREAD);  // cleanup strings created
  Handle string_h;

  if (!string_or_null_h.is_null()) {
    string_h = string_or_null_h;
  } else {
    string_h = java_lang_String::create_from_unicode(name, len, CHECK_NULL);
  }

  assert(java_lang_String::equals(string_h(), name, len),
         "string must be properly initialized");
  assert(len == java_lang_String::length(string_h()), "Must be same length");

  // Notify deduplication support that the string is being interned.  A string
  // must never be deduplicated after it has been interned.  Doing so interferes
  // with compiler optimizations done on e.g. interned string literals.
  if (StringDedup::is_enabled()) {
    StringDedup::notify_intern(string_h());
  }

  StringTableLookupOop lookup(THREAD, hash, string_h);
  StringTableGet stg(THREAD);

  bool rehash_warning;
  do {
    // Callers have already looked up the String using the jchar* name, so just go to add.
    WeakHandle wh(_oop_storage, string_h);
    // The hash table takes ownership of the WeakHandle, even if it's not inserted.
    if (_local_table->insert(THREAD, lookup, wh, &rehash_warning)) {
      update_needs_rehash(rehash_warning);
      return wh.resolve();
    }
    // In case another thread did a concurrent add, return value already in the table.
    // This could fail if the String got gc'ed concurrently, so loop back until success.
    if (_local_table->get(THREAD, lookup, stg, &rehash_warning)) {
      update_needs_rehash(rehash_warning);
      return stg.get_res_oop();
    }
  } while(true);
}

// Concurrent work
void StringTable::grow(JavaThread* jt) {
  StringTableHash::GrowTask gt(_local_table);
  if (!gt.prepare(jt)) {
    return;
  }
  log_trace(stringtable)("Started to grow");
  {
    TraceTime timer("Grow", TRACETIME_LOG(Debug, stringtable, perf));
    while (gt.do_task(jt)) {
      gt.pause(jt);
      {
        ThreadBlockInVM tbivm(jt);
      }
      gt.cont(jt);
    }
  }
  gt.done(jt);
  _current_size = table_size();
  log_debug(stringtable)("Grown to size:" SIZE_FORMAT, _current_size);
}

struct StringTableDoDelete : StackObj {
  void operator()(WeakHandle* val) {
    /* do nothing */
  }
};

struct StringTableDeleteCheck : StackObj {
  long _count;
  long _item;
  StringTableDeleteCheck() : _count(0), _item(0) {}
  bool operator()(WeakHandle* val) {
    ++_item;
    oop tmp = val->peek();
    if (tmp == NULL) {
      ++_count;
      return true;
    } else {
      return false;
    }
  }
};

void StringTable::clean_dead_entries(JavaThread* jt) {
  StringTableHash::BulkDeleteTask bdt(_local_table);
  if (!bdt.prepare(jt)) {
    return;
  }

  StringTableDeleteCheck stdc;
  StringTableDoDelete stdd;
  {
    TraceTime timer("Clean", TRACETIME_LOG(Debug, stringtable, perf));
    while(bdt.do_task(jt, stdc, stdd)) {
      bdt.pause(jt);
      {
        ThreadBlockInVM tbivm(jt);
      }
      bdt.cont(jt);
    }
    bdt.done(jt);
  }
  log_debug(stringtable)("Cleaned %ld of %ld", stdc._count, stdc._item);
}

void StringTable::gc_notification(size_t num_dead) {
  log_trace(stringtable)("Uncleaned items:" SIZE_FORMAT, num_dead);

  if (has_work()) {
    return;
  }

  double load_factor = StringTable::get_load_factor();
  double dead_factor = StringTable::get_dead_factor(num_dead);
  // We should clean/resize if we have more dead than alive,
  // more items than preferred load factor or
  // more dead items than water mark.
  if ((dead_factor > load_factor) ||
      (load_factor > PREF_AVG_LIST_LEN) ||
      (dead_factor > CLEAN_DEAD_HIGH_WATER_MARK)) {
    log_debug(stringtable)("Concurrent work triggered, live factor: %g dead factor: %g",
                           load_factor, dead_factor);
    trigger_concurrent_work();
  }
}

bool StringTable::has_work() {
  return Atomic::load_acquire(&_has_work);
}

void StringTable::do_concurrent_work(JavaThread* jt) {
  double load_factor = get_load_factor();
  log_debug(stringtable, perf)("Concurrent work, live factor: %g", load_factor);
  // We prefer growing, since that also removes dead items
  if (load_factor > PREF_AVG_LIST_LEN && !_local_table->is_max_size_reached()) {
    grow(jt);
  } else {
    clean_dead_entries(jt);
  }
  Atomic::release_store(&_has_work, false);
}

// Rehash
bool StringTable::do_rehash() {
  if (!_local_table->is_safepoint_safe()) {
    return false;
  }

  // We use current size, not max size.
  size_t new_size = _local_table->get_size_log2(Thread::current());
  StringTableHash* new_table = new StringTableHash(new_size, END_SIZE, REHASH_LEN);
  // Use alt hash from now on
  _alt_hash = true;
  if (!_local_table->try_move_nodes_to(Thread::current(), new_table)) {
    _alt_hash = false;
    delete new_table;
    return false;
  }

  // free old table
  delete _local_table;
  _local_table = new_table;

  return true;
}

void StringTable::rehash_table() {
  static bool rehashed = false;
  log_debug(stringtable)("Table imbalanced, rehashing called.");

  // Grow instead of rehash.
  if (get_load_factor() > PREF_AVG_LIST_LEN &&
      !_local_table->is_max_size_reached()) {
    log_debug(stringtable)("Choosing growing over rehashing.");
    trigger_concurrent_work();
    _needs_rehashing = false;
    return;
  }
  // Already rehashed.
  if (rehashed) {
    log_warning(stringtable)("Rehashing already done, still long lists.");
    trigger_concurrent_work();
    _needs_rehashing = false;
    return;
  }

  _alt_hash_seed = AltHashing::compute_seed();
  {
    if (do_rehash()) {
      rehashed = true;
    } else {
      log_info(stringtable)("Resizes in progress rehashing skipped.");
    }
  }
  _needs_rehashing = false;
}

// Statistics
static int literal_size(oop obj) {
  // NOTE: this would over-count if (pre-JDK8)
  // java_lang_Class::has_offset_field() is true and the String.value array is
  // shared by several Strings. However, starting from JDK8, the String.value
  // array is not shared anymore.
  if (obj == NULL) {
    return 0;
  } else if (obj->klass() == vmClasses::String_klass()) {
    return (obj->size() + java_lang_String::value(obj)->size()) * HeapWordSize;
  } else {
    return obj->size();
  }
}

struct SizeFunc : StackObj {
  size_t operator()(WeakHandle* val) {
    oop s = val->peek();
    if (s == NULL) {
      // Dead
      return 0;
    }
    return literal_size(s);
  };
};

TableStatistics StringTable::get_table_statistics() {
  static TableStatistics ts;
  SizeFunc sz;
  ts = _local_table->statistics_get(Thread::current(), sz, ts);
  return ts;
}

void StringTable::print_table_statistics(outputStream* st,
                                         const char* table_name) {
  SizeFunc sz;
  _local_table->statistics_to(Thread::current(), sz, st, table_name);
}

// Verification
class VerifyStrings : StackObj {
 public:
  bool operator()(WeakHandle* val) {
    oop s = val->peek();
    if (s != NULL) {
      assert(java_lang_String::length(s) >= 0, "Length on string must work.");
    }
    return true;
  };
};

// This verification is part of Universe::verify() and needs to be quick.
void StringTable::verify() {
  Thread* thr = Thread::current();
  VerifyStrings vs;
  if (!_local_table->try_scan(thr, vs)) {
    log_info(stringtable)("verify unavailable at this moment");
  }
}

// Verification and comp
class VerifyCompStrings : StackObj {
  static unsigned string_hash(oop const& str) {
    return java_lang_String::hash_code_noupdate(str);
  }
  static bool string_equals(oop const& a, oop const& b) {
    return java_lang_String::equals(a, b);
  }

  ResizeableResourceHashtable<oop, bool,
                              ResourceObj::C_HEAP, mtInternal,
                              string_hash, string_equals> _table;
 public:
  size_t _errors;
  VerifyCompStrings() : _table(unsigned(_items_count / 8) + 1), _errors(0) {}
  bool operator()(WeakHandle* val) {
    oop s = val->resolve();
    if (s == NULL) {
      return true;
    }
    bool created;
    _table.put_if_absent(s, true, &created);
    assert(created, "Duplicate strings");
    if (!created) {
      _errors++;
    }
    return true;
  };
};

size_t StringTable::verify_and_compare_entries() {
  Thread* thr = Thread::current();
  VerifyCompStrings vcs;
  if (!_local_table->try_scan(thr, vcs)) {
    log_info(stringtable)("verify unavailable at this moment");
  }
  return vcs._errors;
}

// Dumping
class PrintString : StackObj {
  Thread* _thr;
  outputStream* _st;
 public:
  PrintString(Thread* thr, outputStream* st) : _thr(thr), _st(st) {}
  bool operator()(WeakHandle* val) {
    oop s = val->peek();
    if (s == NULL) {
      return true;
    }
    typeArrayOop value     = java_lang_String::value_no_keepalive(s);
    int          length    = java_lang_String::length(s);
    bool         is_latin1 = java_lang_String::is_latin1(s);

    if (length <= 0) {
      _st->print("%d: ", length);
    } else {
      ResourceMark rm(_thr);
      int utf8_length = length;
      char* utf8_string;

      if (!is_latin1) {
        jchar* chars = value->char_at_addr(0);
        utf8_string = UNICODE::as_utf8(chars, utf8_length);
      } else {
        jbyte* bytes = value->byte_at_addr(0);
        utf8_string = UNICODE::as_utf8(bytes, utf8_length);
      }

      _st->print("%d: ", utf8_length);
      HashtableTextDump::put_utf8(_st, utf8_string, utf8_length);
    }
    _st->cr();
    return true;
  };
};

void StringTable::dump(outputStream* st, bool verbose) {
  if (!verbose) {
    print_table_statistics(st, "StringTable");
  } else {
    Thread* thr = Thread::current();
    ResourceMark rm(thr);
    st->print_cr("VERSION: 1.1");
    PrintString ps(thr, st);
    if (!_local_table->try_scan(thr, ps)) {
      st->print_cr("dump unavailable at this moment");
    }
  }
}

// Utility for dumping strings
StringtableDCmd::StringtableDCmd(outputStream* output, bool heap) :
                                 DCmdWithParser(output, heap),
  _verbose("-verbose", "Dump the content of each string in the table",
           "BOOLEAN", false, "false") {
  _dcmdparser.add_dcmd_option(&_verbose);
}

void StringtableDCmd::execute(DCmdSource source, TRAPS) {
  VM_DumpHashtable dumper(output(), VM_DumpHashtable::DumpStrings,
                         _verbose.value());
  VMThread::execute(&dumper);
}

// Sharing
#if INCLUDE_CDS_JAVA_HEAP
size_t StringTable::shared_entry_count() {
  return _shared_table.entry_count();
}

oop StringTable::lookup_shared(const jchar* name, int len, unsigned int hash) {
  assert(hash == java_lang_String::hash_code(name, len),
         "hash must be computed using java_lang_String::hash_code");
  return _shared_table.lookup(name, hash, len);
}

oop StringTable::lookup_shared(const jchar* name, int len) {
  return _shared_table.lookup(name, java_lang_String::hash_code(name, len), len);
}

oop StringTable::create_archived_string(oop s) {
  assert(DumpSharedSpaces, "this function is only used with -Xshare:dump");
  assert(java_lang_String::is_instance(s), "sanity");
  assert(!HeapShared::is_archived_object_during_dumptime(s), "sanity");

  oop new_s = NULL;
  typeArrayOop v = java_lang_String::value_no_keepalive(s);
  typeArrayOop new_v = (typeArrayOop)HeapShared::archive_object(v);
  if (new_v == NULL) {
    return NULL;
  }
  new_s = HeapShared::archive_object(s);
  if (new_s == NULL) {
    return NULL;
  }

  // adjust the pointer to the 'value' field in the new String oop
  java_lang_String::set_value_raw(new_s, new_v);
  // Prevent string deduplication from changing the 'value' field to
  // something not in the archive before building the archive.  Also marks
  // the shared string when loaded.
  java_lang_String::set_deduplication_forbidden(new_s);
  return new_s;
}

class CopyToArchive : StackObj {
  CompactHashtableWriter* _writer;
public:
  CopyToArchive(CompactHashtableWriter* writer) : _writer(writer) {}
  bool do_entry(oop s, bool value_ignored) {
    assert(s != NULL, "sanity");
    unsigned int hash = java_lang_String::hash_code(s);
    oop new_s = StringTable::create_archived_string(s);
    if (new_s == NULL) {
      return true;
    }

    // add to the compact table
    _writer->add(hash, CompressedOops::narrow_oop_value(new_s));
    return true;
  }
};

void StringTable::write_to_archive(const DumpedInternedStrings* dumped_interned_strings) {
  assert(HeapShared::is_heap_object_archiving_allowed(), "must be");

  _shared_table.reset();
  CompactHashtableWriter writer(_items_count, ArchiveBuilder::string_stats());

  // Copy the interned strings into the "string space" within the java heap
  CopyToArchive copier(&writer);
  dumped_interned_strings->iterate(&copier);

  writer.dump(&_shared_table, "string");
}

void StringTable::serialize_shared_table_header(SerializeClosure* soc) {
  _shared_table.serialize_header(soc);

  if (soc->writing()) {
    // Sanity. Make sure we don't use the shared table at dump time
    _shared_table.reset();
  } else if (!HeapShared::closed_regions_mapped()) {
    _shared_table.reset();
  }
}

#endif //INCLUDE_CDS_JAVA_HEAP
