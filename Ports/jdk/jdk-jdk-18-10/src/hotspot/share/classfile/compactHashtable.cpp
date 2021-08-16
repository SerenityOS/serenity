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
#include "jvm.h"
#include "cds/archiveBuilder.hpp"
#include "cds/heapShared.inline.hpp"
#include "classfile/compactHashtable.hpp"
#include "classfile/javaClasses.hpp"
#include "logging/logMessage.hpp"
#include "memory/metadataFactory.hpp"
#include "runtime/arguments.hpp"
#include "runtime/globals.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/numberSeq.hpp"
#include <sys/stat.h>

#if INCLUDE_CDS
/////////////////////////////////////////////////////
//
// The compact hash table writer implementations
//
CompactHashtableWriter::CompactHashtableWriter(int num_entries,
                                               CompactHashtableStats* stats) {
  Arguments::assert_is_dumping_archive();
  assert(num_entries >= 0, "sanity");
  _num_buckets = calculate_num_buckets(num_entries);
  assert(_num_buckets > 0, "no buckets");

  _num_entries_written = 0;
  _buckets = NEW_C_HEAP_ARRAY(GrowableArray<Entry>*, _num_buckets, mtSymbol);
  for (int i=0; i<_num_buckets; i++) {
    _buckets[i] = new (ResourceObj::C_HEAP, mtSymbol) GrowableArray<Entry>(0, mtSymbol);
  }

  _stats = stats;
  _compact_buckets = NULL;
  _compact_entries = NULL;
  _num_empty_buckets = 0;
  _num_value_only_buckets = 0;
  _num_other_buckets = 0;
}

CompactHashtableWriter::~CompactHashtableWriter() {
  for (int index = 0; index < _num_buckets; index++) {
    GrowableArray<Entry>* bucket = _buckets[index];
    delete bucket;
  }

  FREE_C_HEAP_ARRAY(GrowableArray<Entry>*, _buckets);
}

size_t CompactHashtableWriter::estimate_size(int num_entries) {
  int num_buckets = calculate_num_buckets(num_entries);
  size_t bucket_bytes = ArchiveBuilder::ro_array_bytesize<u4>(num_buckets + 1);

  // In worst case, we have no VALUE_ONLY_BUCKET_TYPE, so each entry takes 2 slots
  int entries_space = 2 * num_entries;
  size_t entry_bytes = ArchiveBuilder::ro_array_bytesize<u4>(entries_space);

  return bucket_bytes
       + entry_bytes
       + SimpleCompactHashtable::calculate_header_size();
}

// Add a symbol entry to the temporary hash table
void CompactHashtableWriter::add(unsigned int hash, u4 value) {
  int index = hash % _num_buckets;
  _buckets[index]->append_if_missing(Entry(hash, value));
  _num_entries_written++;
}

void CompactHashtableWriter::allocate_table() {
  int entries_space = 0;
  for (int index = 0; index < _num_buckets; index++) {
    GrowableArray<Entry>* bucket = _buckets[index];
    int bucket_size = bucket->length();
    if (bucket_size == 1) {
      entries_space++;
    } else if (bucket_size > 1) {
      entries_space += 2 * bucket_size;
    }
  }

  if (entries_space & ~BUCKET_OFFSET_MASK) {
    vm_exit_during_initialization("CompactHashtableWriter::allocate_table: Overflow! "
                                  "Too many entries.");
  }

  _compact_buckets = ArchiveBuilder::new_ro_array<u4>(_num_buckets + 1);
  _compact_entries = ArchiveBuilder::new_ro_array<u4>(entries_space);

  _stats->bucket_count    = _num_buckets;
  _stats->bucket_bytes    = align_up(_compact_buckets->size() * BytesPerWord,
                                     SharedSpaceObjectAlignment);
  _stats->hashentry_count = _num_entries_written;
  _stats->hashentry_bytes = align_up(_compact_entries->size() * BytesPerWord,
                                     SharedSpaceObjectAlignment);
}

// Write the compact table's buckets
void CompactHashtableWriter::dump_table(NumberSeq* summary) {
  u4 offset = 0;
  for (int index = 0; index < _num_buckets; index++) {
    GrowableArray<Entry>* bucket = _buckets[index];
    int bucket_size = bucket->length();
    if (bucket_size == 1) {
      // bucket with one entry is compacted and only has the symbol offset
      _compact_buckets->at_put(index, BUCKET_INFO(offset, VALUE_ONLY_BUCKET_TYPE));

      Entry ent = bucket->at(0);
      _compact_entries->at_put(offset++, ent.value());
      _num_value_only_buckets++;
    } else {
      // regular bucket, each entry is a symbol (hash, offset) pair
      _compact_buckets->at_put(index, BUCKET_INFO(offset, REGULAR_BUCKET_TYPE));

      for (int i=0; i<bucket_size; i++) {
        Entry ent = bucket->at(i);
        _compact_entries->at_put(offset++, u4(ent.hash())); // write entry hash
        _compact_entries->at_put(offset++, ent.value());
      }
      if (bucket_size == 0) {
        _num_empty_buckets++;
      } else {
        _num_other_buckets++;
      }
    }
    summary->add(bucket_size);
  }

  // Mark the end of the buckets
  _compact_buckets->at_put(_num_buckets, BUCKET_INFO(offset, TABLEEND_BUCKET_TYPE));
  assert(offset == (u4)_compact_entries->length(), "sanity");
}


// Write the compact table
void CompactHashtableWriter::dump(SimpleCompactHashtable *cht, const char* table_name) {
  NumberSeq summary;
  allocate_table();
  dump_table(&summary);

  int table_bytes = _stats->bucket_bytes + _stats->hashentry_bytes;
  address base_address = address(SharedBaseAddress);
  cht->init(base_address,  _num_entries_written, _num_buckets,
            _compact_buckets->data(), _compact_entries->data());

  LogMessage(cds, hashtables) msg;
  if (msg.is_info()) {
    double avg_cost = 0.0;
    if (_num_entries_written > 0) {
      avg_cost = double(table_bytes)/double(_num_entries_written);
    }
    msg.info("Shared %s table stats -------- base: " PTR_FORMAT,
                         table_name, (intptr_t)base_address);
    msg.info("Number of entries       : %9d", _num_entries_written);
    msg.info("Total bytes used        : %9d", table_bytes);
    msg.info("Average bytes per entry : %9.3f", avg_cost);
    msg.info("Average bucket size     : %9.3f", summary.avg());
    msg.info("Variance of bucket size : %9.3f", summary.variance());
    msg.info("Std. dev. of bucket size: %9.3f", summary.sd());
    msg.info("Maximum bucket size     : %9d", (int)summary.maximum());
    msg.info("Empty buckets           : %9d", _num_empty_buckets);
    msg.info("Value_Only buckets      : %9d", _num_value_only_buckets);
    msg.info("Other buckets           : %9d", _num_other_buckets);
  }
}

/////////////////////////////////////////////////////////////
//
// The CompactHashtable implementation
//

void SimpleCompactHashtable::init(address base_address, u4 entry_count, u4 bucket_count, u4* buckets, u4* entries) {
  _bucket_count = bucket_count;
  _entry_count = entry_count;
  _base_address = base_address;
  _buckets = buckets;
  _entries = entries;
}

size_t SimpleCompactHashtable::calculate_header_size() {
  // We have 5 fields. Each takes up sizeof(intptr_t). See WriteClosure::do_u4
  size_t bytes = sizeof(intptr_t) * 5;
  return bytes;
}

void SimpleCompactHashtable::serialize_header(SerializeClosure* soc) {
  // NOTE: if you change this function, you MUST change the number 5 in
  // calculate_header_size() accordingly.
  soc->do_u4(&_entry_count);
  soc->do_u4(&_bucket_count);
  soc->do_ptr((void**)&_buckets);
  soc->do_ptr((void**)&_entries);
  if (soc->reading()) {
    _base_address = (address)SharedBaseAddress;
  }
}
#endif // INCLUDE_CDS

#ifndef O_BINARY       // if defined (Win32) use binary files.
#define O_BINARY 0     // otherwise do nothing.
#endif

////////////////////////////////////////////////////////
//
// HashtableTextDump
//
HashtableTextDump::HashtableTextDump(const char* filename) : _fd(-1) {
  struct stat st;
  if (os::stat(filename, &st) != 0) {
    quit("Unable to get hashtable dump file size", filename);
  }
  _size = st.st_size;
  _fd = os::open(filename, O_RDONLY | O_BINARY, 0);
  if (_fd < 0) {
    quit("Unable to open hashtable dump file", filename);
  }
  _base = os::map_memory(_fd, filename, 0, NULL, _size, true, false);
  if (_base == NULL) {
    quit("Unable to map hashtable dump file", filename);
  }
  _p = _base;
  _end = _base + st.st_size;
  _filename = filename;
  _prefix_type = Unknown;
  _line_no = 1;
}

HashtableTextDump::~HashtableTextDump() {
  os::unmap_memory((char*)_base, _size);
  if (_fd >= 0) {
    close(_fd);
  }
}

void HashtableTextDump::quit(const char* err, const char* msg) {
  vm_exit_during_initialization(err, msg);
}

void HashtableTextDump::corrupted(const char *p, const char* msg) {
  char info[100];
  jio_snprintf(info, sizeof(info),
               "%s. Corrupted at line %d (file pos %d)",
               msg, _line_no, (int)(p - _base));
  quit(info, _filename);
}

bool HashtableTextDump::skip_newline() {
  if (_p[0] == '\r' && _p[1] == '\n') {
    _p += 2;
  } else if (_p[0] == '\n') {
    _p += 1;
  } else {
    corrupted(_p, "Unexpected character");
  }
  _line_no++;
  return true;
}

int HashtableTextDump::skip(char must_be_char) {
  corrupted_if(remain() < 1, "Truncated");
  corrupted_if(*_p++ != must_be_char, "Unexpected character");
  return 0;
}

void HashtableTextDump::skip_past(char c) {
  for (;;) {
    corrupted_if(remain() < 1, "Truncated");
    if (*_p++ == c) {
      return;
    }
  }
}

void HashtableTextDump::check_version(const char* ver) {
  int len = (int)strlen(ver);
  corrupted_if(remain() < len, "Truncated");
  if (strncmp(_p, ver, len) != 0) {
    quit("wrong version of hashtable dump file", _filename);
  }
  _p += len;
  skip_newline();
}

void HashtableTextDump::scan_prefix_type() {
  _p++;
  if (strncmp(_p, "SECTION: String", 15) == 0) {
    _p += 15;
    _prefix_type = StringPrefix;
  } else if (strncmp(_p, "SECTION: Symbol", 15) == 0) {
    _p += 15;
    _prefix_type = SymbolPrefix;
  } else {
    _prefix_type = Unknown;
  }
  skip_newline();
}

int HashtableTextDump::scan_prefix(int* utf8_length) {
  if (*_p == '@') {
    scan_prefix_type();
  }

  switch (_prefix_type) {
  case SymbolPrefix:
    *utf8_length = scan_symbol_prefix(); break;
  case StringPrefix:
    *utf8_length = scan_string_prefix(); break;
  default:
    tty->print_cr("Shared input data type: Unknown.");
    corrupted(_p, "Unknown data type");
  }

  return _prefix_type;
}

int HashtableTextDump::scan_string_prefix() {
  // Expect /[0-9]+: /
  int utf8_length = 0;
  get_num(':', &utf8_length);
  if (*_p != ' ') {
    corrupted(_p, "Wrong prefix format for string");
  }
  _p++;
  return utf8_length;
}

int HashtableTextDump::scan_symbol_prefix() {
  // Expect /[0-9]+ (-|)[0-9]+: /
  int utf8_length = 0;
  get_num(' ', &utf8_length);
  if (*_p == '-') {
    _p++;
  }
  int ref_num;
  get_num(':', &ref_num);
  if (*_p != ' ') {
    corrupted(_p, "Wrong prefix format for symbol");
  }
  _p++;
  return utf8_length;
}

jchar HashtableTextDump::unescape(const char* from, const char* end, int count) {
  jchar value = 0;

  corrupted_if(from + count > end, "Truncated");

  for (int i=0; i<count; i++) {
    char c = *from++;
    switch (c) {
    case '0': case '1': case '2': case '3': case '4':
    case '5': case '6': case '7': case '8': case '9':
      value = (value << 4) + c - '0';
      break;
    case 'a': case 'b': case 'c':
    case 'd': case 'e': case 'f':
      value = (value << 4) + 10 + c - 'a';
      break;
    case 'A': case 'B': case 'C':
    case 'D': case 'E': case 'F':
      value = (value << 4) + 10 + c - 'A';
      break;
    default:
      ShouldNotReachHere();
    }
  }
  return value;
}

void HashtableTextDump::get_utf8(char* utf8_buffer, int utf8_length) {
  // cache in local vars
  const char* from = _p;
  const char* end = _end;
  char* to = utf8_buffer;
  int n = utf8_length;

  for (; n > 0 && from < end; n--) {
    if (*from != '\\') {
      *to++ = *from++;
    } else {
      corrupted_if(from + 2 > end, "Truncated");
      char c = from[1];
      from += 2;
      switch (c) {
      case 'x':
        {
          jchar value = unescape(from, end, 2);
          from += 2;
          assert(value <= 0xff, "sanity");
          *to++ = (char)(value & 0xff);
        }
        break;
      case 't':  *to++ = '\t'; break;
      case 'n':  *to++ = '\n'; break;
      case 'r':  *to++ = '\r'; break;
      case '\\': *to++ = '\\'; break;
      default:
        corrupted(_p, "Unsupported character");
      }
    }
  }
  corrupted_if(n > 0, "Truncated"); // expected more chars but file has ended
  _p = from;
  skip_newline();
}

// NOTE: the content is NOT the same as
// UTF8::as_quoted_ascii(const char* utf8_str, int utf8_length, char* buf, int buflen).
// We want to escape \r\n\t so that output [1] is more readable; [2] can be more easily
// parsed by scripts; [3] quickly processed by HashtableTextDump::get_utf8()
void HashtableTextDump::put_utf8(outputStream* st, const char* utf8_string, int utf8_length) {
  const char *c = utf8_string;
  const char *end = c + utf8_length;
  for (; c < end; c++) {
    switch (*c) {
    case '\t': st->print("\\t"); break;
    case '\r': st->print("\\r"); break;
    case '\n': st->print("\\n"); break;
    case '\\': st->print("\\\\"); break;
    default:
      if (isprint(*c)) {
        st->print("%c", *c);
      } else {
        st->print("\\x%02x", ((unsigned int)*c) & 0xff);
      }
    }
  }
}
