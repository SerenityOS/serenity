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

#ifndef SHARE_CLASSFILE_COMPACTHASHTABLE_HPP
#define SHARE_CLASSFILE_COMPACTHASHTABLE_HPP

#include "oops/array.hpp"
#include "oops/symbol.hpp"
#include "runtime/globals.hpp"
#include "utilities/growableArray.hpp"


template <
  typename K,
  typename V,
  V (*DECODE)(address base_address, u4 offset),
  bool (*EQUALS)(V value, K key, int len)
  >
class CompactHashtable;
class NumberSeq;
class SimpleCompactHashtable;
class SerializeClosure;

// Stats for symbol tables in the CDS archive
class CompactHashtableStats {
public:
  int hashentry_count;
  int hashentry_bytes;
  int bucket_count;
  int bucket_bytes;

  CompactHashtableStats() :
    hashentry_count(0), hashentry_bytes(0),
    bucket_count(0), bucket_bytes(0) {}
};

#if INCLUDE_CDS
/////////////////////////////////////////////////////////////////////////
//
// The compact hash table writer. Used at dump time for writing out
// the compact table to the shared archive.
//
// At dump time, the CompactHashtableWriter obtains all entries from the
// symbol/string table and adds them to a new temporary hash table. The hash
// table size (number of buckets) is calculated using
// '(num_entries + bucket_size - 1) / bucket_size'. The default bucket
// size is 4 and can be changed by -XX:SharedSymbolTableBucketSize option.
// 4 is chosen because it produces smaller sized bucket on average for
// faster lookup. It also has relatively small number of empty buckets and
// good distribution of the entries.
//
// We use a simple hash function (hash % num_bucket) for the table.
// The new table is compacted when written out. Please see comments
// above the CompactHashtable class for the table layout detail. The bucket
// offsets are written to the archive as part of the compact table. The
// bucket offset is encoded in the low 30-bit (0-29) and the bucket type
// (regular or compact) are encoded in bit[31, 30]. For buckets with more
// than one entry, both hash and entry offset are written to the
// table. For buckets with only one entry, only the entry offset is written
// to the table and the buckets are tagged as compact in their type bits.
// Buckets without entry are skipped from the table. Their offsets are
// still written out for faster lookup.
//
class CompactHashtableWriter: public StackObj {
public:
  class Entry {
    unsigned int _hash;
    u4 _value;

  public:
    Entry() {}
    Entry(unsigned int hash, u4 val) : _hash(hash), _value(val) {}

    u4 value() {
      return _value;
    }
    unsigned int hash() {
      return _hash;
    }

    bool operator==(const CompactHashtableWriter::Entry& other) {
      return (_value == other._value && _hash == other._hash);
    }
  }; // class CompactHashtableWriter::Entry

private:
  int _num_entries_written;
  int _num_buckets;
  int _num_empty_buckets;
  int _num_value_only_buckets;
  int _num_other_buckets;
  GrowableArray<Entry>** _buckets;
  CompactHashtableStats* _stats;
  Array<u4>* _compact_buckets;
  Array<u4>* _compact_entries;

public:
  // This is called at dump-time only
  CompactHashtableWriter(int num_entries, CompactHashtableStats* stats);
  ~CompactHashtableWriter();

  void add(unsigned int hash, u4 value);

private:
  void allocate_table();
  void dump_table(NumberSeq* summary);
  static int calculate_num_buckets(int num_entries) {
    int num_buckets = num_entries / SharedSymbolTableBucketSize;
    // calculation of num_buckets can result in zero buckets, we need at least one
    return (num_buckets < 1) ? 1 : num_buckets;
  }

public:
  void dump(SimpleCompactHashtable *cht, const char* table_name);

  static size_t estimate_size(int num_entries);
};
#endif // INCLUDE_CDS

#define REGULAR_BUCKET_TYPE       0
#define VALUE_ONLY_BUCKET_TYPE    1
#define TABLEEND_BUCKET_TYPE      3
#define BUCKET_OFFSET_MASK        0x3FFFFFFF
#define BUCKET_OFFSET(info)       ((info) & BUCKET_OFFSET_MASK)
#define BUCKET_TYPE_SHIFT         30
#define BUCKET_TYPE(info)         (((info) & ~BUCKET_OFFSET_MASK) >> BUCKET_TYPE_SHIFT)
#define BUCKET_INFO(offset, type) (((type) << BUCKET_TYPE_SHIFT) | ((offset) & BUCKET_OFFSET_MASK))

/////////////////////////////////////////////////////////////////////////////
//
// CompactHashtable is used to store the CDS archive's symbol/string tables.
//
// Because these tables are read-only (no entries can be added/deleted) at run-time
// and tend to have large number of entries, we try to minimize the footprint
// cost per entry.
//
// The CompactHashtable is split into two arrays
//
//   u4 buckets[num_buckets+1]; // bit[31,30]: type; bit[29-0]: offset
//   u4 entries[<variable size>]
//
// The size of buckets[] is 'num_buckets + 1'. Each entry of
// buckets[] is a 32-bit encoding of the bucket type and bucket offset,
// with the type in the left-most 2-bit and offset in the remaining 30-bit.
// The last entry is a special type. It contains the end of the last
// bucket.
//
// There are two types of buckets, regular buckets and value_only buckets. The
// value_only buckets have '01' in their highest 2-bit, and regular buckets have
// '00' in their highest 2-bit.
//
// For normal buckets, each entry is 8 bytes in the entries[]:
//   u4 hash;    /* symbol/string hash */
//   union {
//     u4 offset;  /* Symbol* sym = (Symbol*)(base_address + offset) */
//     narrowOop str; /* String narrowOop encoding */
//   }
//
//
// For value_only buckets, each entry has only the 4-byte 'offset' in the entries[].
//
// Example -- note that the second bucket is a VALUE_ONLY_BUCKET_TYPE so the hash code
//            is skipped.
// buckets[0, 4, 5, ....]
//         |  |  |
//         |  |  +---+
//         |  |      |
//         |  +----+ |
//         v       v v
// entries[H,O,H,O,O,H,O,H,O.....]
//
// See CompactHashtable::lookup() for how the table is searched at runtime.
// See CompactHashtableWriter::dump() for how the table is written at CDS
// dump time.
//
class SimpleCompactHashtable {
protected:
  address  _base_address;
  u4  _bucket_count;
  u4  _entry_count;
  u4* _buckets;
  u4* _entries;

public:
  SimpleCompactHashtable() {
    _entry_count = 0;
    _bucket_count = 0;
    _buckets = 0;
    _entries = 0;
  }

  void reset() {
    _bucket_count = 0;
    _entry_count = 0;
    _buckets = 0;
    _entries = 0;
  }

  void init(address base_address, u4 entry_count, u4 bucket_count, u4* buckets, u4* entries);

  // Read/Write the table's header from/to the CDS archive
  void serialize_header(SerializeClosure* soc) NOT_CDS_RETURN;

  inline bool empty() const {
    return (_entry_count == 0);
  }

  inline size_t entry_count() const {
    return _entry_count;
  }

  static size_t calculate_header_size();
};

template <
  typename K,
  typename V,
  V (*DECODE)(address base_address, u4 offset),
  bool (*EQUALS)(V value, K key, int len)
  >
class CompactHashtable : public SimpleCompactHashtable {
  friend class VMStructs;

  V decode(u4 offset) const {
    return DECODE(_base_address, offset);
  }

public:
  // Lookup a value V from the compact table using key K
  inline V lookup(K key, unsigned int hash, int len) const {
    if (_entry_count > 0) {
      int index = hash % _bucket_count;
      u4 bucket_info = _buckets[index];
      u4 bucket_offset = BUCKET_OFFSET(bucket_info);
      int bucket_type = BUCKET_TYPE(bucket_info);
      u4* entry = _entries + bucket_offset;

      if (bucket_type == VALUE_ONLY_BUCKET_TYPE) {
        V value = decode(entry[0]);
        if (EQUALS(value, key, len)) {
          return value;
        }
      } else {
        // This is a regular bucket, which has more than one
        // entries. Each entry is a pair of entry (hash, offset).
        // Seek until the end of the bucket.
        u4* entry_max = _entries + BUCKET_OFFSET(_buckets[index + 1]);
        while (entry < entry_max) {
          unsigned int h = (unsigned int)(entry[0]);
          if (h == hash) {
            V value = decode(entry[1]);
            if (EQUALS(value, key, len)) {
              return value;
            }
          }
          entry += 2;
        }
      }
    }
    return NULL;
  }

  template <class ITER>
  inline void iterate(ITER* iter) const {
    for (u4 i = 0; i < _bucket_count; i++) {
      u4 bucket_info = _buckets[i];
      u4 bucket_offset = BUCKET_OFFSET(bucket_info);
      int bucket_type = BUCKET_TYPE(bucket_info);
      u4* entry = _entries + bucket_offset;

      if (bucket_type == VALUE_ONLY_BUCKET_TYPE) {
        iter->do_value(decode(entry[0]));
      } else {
        u4*entry_max = _entries + BUCKET_OFFSET(_buckets[i + 1]);
        while (entry < entry_max) {
          iter->do_value(decode(entry[1]));
          entry += 2;
        }
      }
    }
  }

  void print_table_statistics(outputStream* st, const char* name) {
    st->print_cr("%s statistics:", name);
    int total_entries = 0;
    int max_bucket = 0;
    for (u4 i = 0; i < _bucket_count; i++) {
      u4 bucket_info = _buckets[i];
      int bucket_type = BUCKET_TYPE(bucket_info);
      int bucket_size;

      if (bucket_type == VALUE_ONLY_BUCKET_TYPE) {
        bucket_size = 1;
      } else {
        bucket_size = (BUCKET_OFFSET(_buckets[i + 1]) - BUCKET_OFFSET(bucket_info)) / 2;
      }
      total_entries += bucket_size;
      if (max_bucket < bucket_size) {
        max_bucket = bucket_size;
      }
    }
    st->print_cr("Number of buckets       : %9d", _bucket_count);
    st->print_cr("Number of entries       : %9d", total_entries);
    st->print_cr("Maximum bucket size     : %9d", max_bucket);
  }
};

////////////////////////////////////////////////////////////////////////
//
// OffsetCompactHashtable -- This is used to store many types of objects
// in the CDS archive. On 64-bit platforms, we save space by using a 32-bit
// offset from the CDS base address.

template <typename V>
inline V read_value_from_compact_hashtable(address base_address, u4 offset) {
  return (V)(base_address + offset);
}

template <
  typename K,
  typename V,
  bool (*EQUALS)(V value, K key, int len)
  >
class OffsetCompactHashtable : public CompactHashtable<
    K, V, read_value_from_compact_hashtable<V>, EQUALS> {
};


////////////////////////////////////////////////////////////////////////
//
// Read/Write the contents of a hashtable textual dump (created by
// SymbolTable::dump and StringTable::dump).
// Because the dump file may be big (hundred of MB in extreme cases),
// we use mmap for fast access when reading it.
//
class HashtableTextDump {
  int _fd;
  const char* _base;
  const char* _p;
  const char* _end;
  const char* _filename;
  size_t      _size;
  int         _prefix_type;
  int         _line_no;
public:
  HashtableTextDump(const char* filename);
  ~HashtableTextDump();

  enum {
    SymbolPrefix = 1 << 0,
    StringPrefix = 1 << 1,
    Unknown = 1 << 2
  };

  void quit(const char* err, const char* msg);

  inline int remain() {
    return (int)(_end - _p);
  }
  int last_line_no() {
    return _line_no - 1;
  }

  void corrupted(const char *p, const char *msg);

  inline void corrupted_if(bool cond, const char *msg) {
    if (cond) {
      corrupted(_p, msg);
    }
  }

  bool skip_newline();
  int skip(char must_be_char);
  void skip_past(char c);
  void check_version(const char* ver);

  inline void get_num(char delim, int *num) {
    const char* p   = _p;
    const char* end = _end;
    u8 n = 0;

    while (p < end) {
      char c = *p++;
      if ('0' <= c && c <= '9') {
        n = n * 10 + (c - '0');
        if (n > (u8)INT_MAX) {
          corrupted(_p, "Num overflow");
        }
      } else if (c == delim) {
        _p = p;
        *num = (int)n;
        return;
      } else {
        // Not [0-9], not 'delim'
        corrupted(_p, "Unrecognized format");;
      }
    }

    corrupted(_end, "Incorrect format");
    ShouldNotReachHere();
  }

  void scan_prefix_type();
  int scan_prefix(int* utf8_length);
  int scan_string_prefix();
  int scan_symbol_prefix();

  jchar unescape(const char* from, const char* end, int count);
  void get_utf8(char* utf8_buffer, int utf8_length);
  static void put_utf8(outputStream* st, const char* utf8_string, int utf8_length);
};

#endif // SHARE_CLASSFILE_COMPACTHASHTABLE_HPP
