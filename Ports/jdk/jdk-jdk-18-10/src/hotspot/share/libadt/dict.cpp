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
#include "libadt/dict.hpp"
#include "utilities/powerOfTwo.hpp"

// Dictionaries - An Abstract Data Type

// %%%%% includes not needed with AVM framework - Ungar

#include <assert.h>

//------------------------------data-----------------------------------------
// String hash tables
#define MAXID 20
static const char shft[MAXID] = {1,2,3,4,5,6,7,1,2,3,4,5,6,7,1,2,3,4,5,6};
// Precomputed table of null character hashes
// xsum[0] = (1 << shft[0]) + 1;
// for(int i = 1; i < MAXID; i++) {
//   xsum[i] = (1 << shft[i]) + 1 + xsum[i - 1];
// }
static const short xsum[MAXID] = {3,8,17,34,67,132,261,264,269,278,295,328,393,522,525,530,539,556,589,654};

//------------------------------bucket---------------------------------------
class bucket : public ResourceObj {
public:
  uint _cnt, _max;              // Size of bucket
  void** _keyvals;              // Array of keys and values
};

//------------------------------Dict-----------------------------------------
// The dictionary is kept has a hash table.  The hash table is a even power
// of two, for nice modulo operations.  Each bucket in the hash table points
// to a linear list of key-value pairs; each key & value is just a (void *).
// The list starts with a count.  A hash lookup finds the list head, then a
// simple linear scan finds the key.  If the table gets too full, it's
// doubled in size; the total amount of EXTRA times all hash functions are
// computed for the doubling is no more than the current size - thus the
// doubling in size costs no more than a constant factor in speed.
Dict::Dict(CmpKey initcmp, Hash inithash) : _arena(Thread::current()->resource_area()),
  _hash(inithash), _cmp(initcmp) {

  _size = 16;                   // Size is a power of 2
  _cnt = 0;                     // Dictionary is empty
  _bin = (bucket*)_arena->AmallocWords(sizeof(bucket) * _size);
  memset((void*)_bin, 0, sizeof(bucket) * _size);
}

Dict::Dict(CmpKey initcmp, Hash inithash, Arena* arena, int size)
: _arena(arena), _hash(inithash), _cmp(initcmp) {
  // Size is a power of 2
  _size = MAX2(16, round_up_power_of_2(size));

  _cnt = 0;                     // Dictionary is empty
  _bin = (bucket*)_arena->AmallocWords(sizeof(bucket) * _size);
  memset((void*)_bin, 0, sizeof(bucket) * _size);
}

// Deep copy into arena of choice
Dict::Dict(const Dict &d, Arena* arena)
: _arena(arena), _size(d._size), _cnt(d._cnt), _hash(d._hash), _cmp(d._cmp) {
  _bin = (bucket*)_arena->AmallocWords(sizeof(bucket) * _size);
  memcpy((void*)_bin, (void*)d._bin, sizeof(bucket) * _size);
  for (uint i = 0; i < _size; i++) {
    if (!_bin[i]._keyvals) {
      continue;
    }
    _bin[i]._keyvals = (void**)_arena->AmallocWords(sizeof(void*) * _bin[i]._max * 2);
    memcpy(_bin[i]._keyvals, d._bin[i]._keyvals, _bin[i]._cnt * 2 * sizeof(void*));
  }
}

//------------------------------~Dict------------------------------------------
// Delete an existing dictionary.
Dict::~Dict() { }

//------------------------------doubhash---------------------------------------
// Double hash table size.  If can't do so, just suffer.  If can, then run
// thru old hash table, moving things to new table.  Note that since hash
// table doubled, exactly 1 new bit is exposed in the mask - so everything
// in the old table ends up on 1 of two lists in the new table; a hi and a
// lo list depending on the value of the bit.
void Dict::doubhash() {
  uint oldsize = _size;
  _size <<= 1;                  // Double in size
  _bin = (bucket*)_arena->Arealloc(_bin, sizeof(bucket) * oldsize, sizeof(bucket) * _size);
  memset((void*)(&_bin[oldsize]), 0, oldsize * sizeof(bucket));
  // Rehash things to spread into new table
  for (uint i = 0; i < oldsize; i++) { // For complete OLD table do
    bucket* b = &_bin[i];              // Handy shortcut for _bin[i]
    if (!b->_keyvals) continue;        // Skip empties fast

    bucket* nb = &_bin[i+oldsize];     // New bucket shortcut
    uint j = b->_max;                  // Trim new bucket to nearest power of 2
    while (j > b->_cnt) { j >>= 1; }   // above old bucket _cnt
    if (!j) { j = 1; }                 // Handle zero-sized buckets
    nb->_max = j << 1;
    // Allocate worst case space for key-value pairs
    nb->_keyvals = (void**)_arena->AmallocWords(sizeof(void* ) * nb->_max * 2);
    uint nbcnt = 0;

    for (j = 0; j < b->_cnt;) {        // Rehash all keys in this bucket
      void* key = b->_keyvals[j + j];
      if ((_hash(key) & (_size-1)) != i) { // Moving to hi bucket?
        nb->_keyvals[nbcnt + nbcnt] = key;
        nb->_keyvals[nbcnt + nbcnt + 1] = b->_keyvals[j + j + 1];
        nb->_cnt = nbcnt = nbcnt + 1;
        b->_cnt--;                     // Remove key/value from lo bucket
        b->_keyvals[j + j] = b->_keyvals[b->_cnt + b->_cnt];
        b->_keyvals[j + j + 1] = b->_keyvals[b->_cnt + b->_cnt + 1];
        // Don't increment j, hash compacted element also.
      } else {
        j++; // Iterate.
      }
    } // End of for all key-value pairs in bucket
  } // End of for all buckets
}

//------------------------------Insert----------------------------------------
// Insert or replace a key/value pair in the given dictionary.  If the
// dictionary is too full, it's size is doubled.  The prior value being
// replaced is returned (NULL if this is a 1st insertion of that key).  If
// an old value is found, it's swapped with the prior key-value pair on the
// list.  This moves a commonly searched-for value towards the list head.
void*Dict::Insert(void* key, void* val, bool replace) {
  uint hash = _hash(key);       // Get hash key
  uint i = hash & (_size - 1);  // Get hash key, corrected for size
  bucket* b = &_bin[i];
  for (uint j = 0; j < b->_cnt; j++) {
    if (!_cmp(key, b->_keyvals[j + j])) {
      if (!replace) {
        return b->_keyvals[j + j + 1];
      } else {
        void* prior = b->_keyvals[j + j + 1];
        b->_keyvals[j + j    ] = key;
        b->_keyvals[j + j + 1] = val;
        return prior;
      }
    }
  }
  if (++_cnt > _size) {         // Hash table is full
    doubhash();                 // Grow whole table if too full
    i = hash & (_size - 1);     // Rehash
    b = &_bin[i];
  }
  if (b->_cnt == b->_max) {     // Must grow bucket?
    if (!b->_keyvals) {
      b->_max = 2;              // Initial bucket size
      b->_keyvals = (void**)_arena->AmallocWords(sizeof(void*) * b->_max * 2);
    } else {
      b->_keyvals = (void**)_arena->Arealloc(b->_keyvals, sizeof(void*) * b->_max * 2, sizeof(void*) * b->_max * 4);
      b->_max <<= 1;            // Double bucket
    }
  }
  b->_keyvals[b->_cnt + b->_cnt    ] = key;
  b->_keyvals[b->_cnt + b->_cnt + 1] = val;
  b->_cnt++;
  return NULL;                  // Nothing found prior
}

//------------------------------Delete---------------------------------------
// Find & remove a value from dictionary. Return old value.
void* Dict::Delete(void* key) {
  uint i = _hash(key) & (_size - 1); // Get hash key, corrected for size
  bucket* b = &_bin[i];         // Handy shortcut
  for (uint j = 0; j < b->_cnt; j++) {
    if (!_cmp(key, b->_keyvals[j + j])) {
      void* prior = b->_keyvals[j + j + 1];
      b->_cnt--;                // Remove key/value from lo bucket
      b->_keyvals[j+j  ] = b->_keyvals[b->_cnt + b->_cnt    ];
      b->_keyvals[j+j+1] = b->_keyvals[b->_cnt + b->_cnt + 1];
      _cnt--;                   // One less thing in table
      return prior;
    }
  }
  return NULL;
}

//------------------------------FindDict-------------------------------------
// Find a key-value pair in the given dictionary.  If not found, return NULL.
// If found, move key-value pair towards head of list.
void* Dict::operator [](const void* key) const {
  uint i = _hash(key) & (_size - 1); // Get hash key, corrected for size
  bucket* b = &_bin[i];         // Handy shortcut
  for (uint j = 0; j < b->_cnt; j++) {
    if (!_cmp(key, b->_keyvals[j + j])) {
      return b->_keyvals[j + j + 1];
    }
  }
  return NULL;
}

//------------------------------print------------------------------------------
// Handier print routine
void Dict::print() {
  DictI i(this); // Moved definition in iterator here because of g++.
  tty->print("Dict@" INTPTR_FORMAT "[%d] = {", p2i(this), _cnt);
  for (; i.test(); ++i) {
    tty->print("(" INTPTR_FORMAT "," INTPTR_FORMAT "),", p2i(i._key), p2i(i._value));
  }
  tty->print_cr("}");
}

//------------------------------Hashing Functions----------------------------
// Convert string to hash key.  This algorithm implements a universal hash
// function with the multipliers frozen (ok, so it's not universal).  The
// multipliers (and allowable characters) are all odd, so the resultant sum
// is odd - guaranteed not divisible by any power of two, so the hash tables
// can be any power of two with good results.  Also, I choose multipliers
// that have only 2 bits set (the low is always set to be odd) so
// multiplication requires only shifts and adds.  Characters are required to
// be in the range 0-127 (I double & add 1 to force oddness).  Keys are
// limited to MAXID characters in length.  Experimental evidence on 150K of
// C text shows excellent spreading of values for any size hash table.
int hashstr(const void* t) {
  char c, k = 0;
  int32_t sum = 0;
  const char* s = (const char*)t;

  while (((c = *s++) != '\0') && (k < MAXID-1)) { // Get characters till null or MAXID-1
    c = (c << 1) + 1;             // Characters are always odd!
    sum += c + (c << shft[k++]);  // Universal hash function
  }
  return (int)((sum + xsum[k]) >> 1); // Hash key, un-modulo'd table size
}

//------------------------------hashptr--------------------------------------
// Slimey cheap hash function; no guaranteed performance.  Better than the
// default for pointers, especially on MS-DOS machines.
int hashptr(const void* key) {
  return ((intptr_t)key >> 2);
}

// Slimey cheap hash function; no guaranteed performance.
int hashkey(const void* key) {
  return (intptr_t)key;
}

//------------------------------Key Comparator Functions---------------------
int32_t cmpstr(const void* k1, const void* k2) {
  return strcmp((const char*)k1, (const char*)k2);
}

// Cheap key comparator.
int32_t cmpkey(const void* key1, const void* key2) {
  if (key1 == key2) {
    return 0;
  }
  intptr_t delta = (intptr_t)key1 - (intptr_t)key2;
  if (delta > 0) {
    return 1;
  }
  return -1;
}

//=============================================================================
//------------------------------reset------------------------------------------
// Create an iterator and initialize the first variables.
void DictI::reset(const Dict* dict) {
  _d = dict;
  _i = (uint)-1;                // Before the first bin
  _j = 0;                       // Nothing left in the current bin
  ++(*this);                    // Step to first real value
}

//------------------------------next-------------------------------------------
// Find the next key-value pair in the dictionary, or return a NULL key and
// value.
void DictI::operator ++(void) {
  if (_j--) {                   // Still working in current bin?
    _key   = _d->_bin[_i]._keyvals[_j + _j];
    _value = _d->_bin[_i]._keyvals[_j + _j + 1];
    return;
  }

  while (++_i < _d->_size) {   // Else scan for non-zero bucket
    _j = _d->_bin[_i]._cnt;
    if (!_j) {
      continue;
    }
    _j--;
    _key   = _d->_bin[_i]._keyvals[_j+_j];
    _value = _d->_bin[_i]._keyvals[_j+_j+1];
    return;
  }
  _key = _value = NULL;
}
