/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

// Dictionaries - An Abstract Data Type

#include "adlc.hpp"

// #include "dict.hpp"


//------------------------------data-----------------------------------------
// String hash tables
#define MAXID 20
static char initflag = 0;       // True after 1st initialization
static char shft[MAXID + 1] = {1,2,3,4,5,6,7,1,2,3,4,5,6,7,1,2,3,4,5,6,7};
static short xsum[MAXID];

//------------------------------bucket---------------------------------------
class bucket {
public:
  int          _cnt, _max;      // Size of bucket
  const void **_keyvals;        // Array of keys and values
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
Dict::Dict(CmpKey initcmp, Hash inithash) : _hash(inithash), _cmp(initcmp), _arena(NULL) {
  init();
}

Dict::Dict(CmpKey initcmp, Hash inithash, Arena *arena) : _hash(inithash), _cmp(initcmp), _arena(arena) {
  init();
}

void Dict::init() {
  int i;

  // Precompute table of null character hashes
  if (!initflag) {              // Not initializated yet?
    xsum[0] = (short) ((1 << shft[0]) + 1);  // Initialize
    for( i = 1; i < MAXID; i++) {
      xsum[i] = (short) ((1 << shft[i]) + 1 + xsum[i-1]);
    }
    initflag = 1;               // Never again
  }

  _size = 16;                   // Size is a power of 2
  _cnt = 0;                     // Dictionary is empty
  _bin = (bucket*)_arena->AmallocWords(sizeof(bucket) * _size);
  memset(_bin, 0, sizeof(bucket) * _size);
}

//------------------------------~Dict------------------------------------------
// Delete an existing dictionary.
Dict::~Dict() {
}

//------------------------------Clear----------------------------------------
// Zap to empty; ready for re-use
void Dict::Clear() {
  _cnt = 0;                     // Empty contents
  for( int i=0; i<_size; i++ )
    _bin[i]._cnt = 0;           // Empty buckets, but leave allocated
  // Leave _size & _bin alone, under the assumption that dictionary will
  // grow to this size again.
}

//------------------------------doubhash---------------------------------------
// Double hash table size.  If can't do so, just suffer.  If can, then run
// thru old hash table, moving things to new table.  Note that since hash
// table doubled, exactly 1 new bit is exposed in the mask - so everything
// in the old table ends up on 1 of two lists in the new table; a hi and a
// lo list depending on the value of the bit.
void Dict::doubhash(void) {
  int oldsize = _size;
  _size <<= 1;                  // Double in size
  _bin = (bucket*)_arena->Arealloc( _bin, sizeof(bucket)*oldsize, sizeof(bucket)*_size );
  memset( &_bin[oldsize], 0, oldsize*sizeof(bucket) );
  // Rehash things to spread into new table
  for( int i=0; i < oldsize; i++) { // For complete OLD table do
    bucket *b = &_bin[i];       // Handy shortcut for _bin[i]
    if( !b->_keyvals ) continue;        // Skip empties fast

    bucket *nb = &_bin[i+oldsize];  // New bucket shortcut
    int j = b->_max;                // Trim new bucket to nearest power of 2
    while( j > b->_cnt ) j >>= 1;   // above old bucket _cnt
    if( !j ) j = 1;             // Handle zero-sized buckets
    nb->_max = j<<1;
    // Allocate worst case space for key-value pairs
    nb->_keyvals = (const void**)_arena->AmallocWords( sizeof(void *)*nb->_max*2 );
    int nbcnt = 0;

    for( j=0; j<b->_cnt; j++ ) {  // Rehash all keys in this bucket
      const void *key = b->_keyvals[j+j];
      if( (_hash( key ) & (_size-1)) != i ) { // Moving to hi bucket?
        nb->_keyvals[nbcnt+nbcnt] = key;
        nb->_keyvals[nbcnt+nbcnt+1] = b->_keyvals[j+j+1];
        nb->_cnt = nbcnt = nbcnt+1;
        b->_cnt--;              // Remove key/value from lo bucket
        b->_keyvals[j+j  ] = b->_keyvals[b->_cnt+b->_cnt  ];
        b->_keyvals[j+j+1] = b->_keyvals[b->_cnt+b->_cnt+1];
        j--;                    // Hash compacted element also
      }
    } // End of for all key-value pairs in bucket
  } // End of for all buckets


}

//------------------------------Dict-----------------------------------------
// Deep copy a dictionary.
Dict::Dict( const Dict &d ) : _size(d._size), _cnt(d._cnt), _hash(d._hash),_cmp(d._cmp), _arena(d._arena) {
  _bin = (bucket*)_arena->AmallocWords(sizeof(bucket)*_size);
  memcpy( _bin, d._bin, sizeof(bucket)*_size );
  for( int i=0; i<_size; i++ ) {
    if( !_bin[i]._keyvals ) continue;
    _bin[i]._keyvals=(const void**)_arena->AmallocWords( sizeof(void *)*_bin[i]._max*2);
    memcpy( _bin[i]._keyvals, d._bin[i]._keyvals,_bin[i]._cnt*2*sizeof(void*));
  }
}

//------------------------------Dict-----------------------------------------
// Deep copy a dictionary.
Dict &Dict::operator =( const Dict &d ) {
  if( _size < d._size ) {       // If must have more buckets
    _arena = d._arena;
    _bin = (bucket*)_arena->Arealloc( _bin, sizeof(bucket)*_size, sizeof(bucket)*d._size );
    memset( &_bin[_size], 0, (d._size-_size)*sizeof(bucket) );
    _size = d._size;
  }
  for( int i=0; i<_size; i++ ) // All buckets are empty
    _bin[i]._cnt = 0;           // But leave bucket allocations alone
  _cnt = d._cnt;
  *(Hash*)(&_hash) = d._hash;
  *(CmpKey*)(&_cmp) = d._cmp;
  for(int k=0; k<_size; k++ ) {
    bucket *b = &d._bin[k];     // Shortcut to source bucket
    for( int j=0; j<b->_cnt; j++ )
      Insert( b->_keyvals[j+j], b->_keyvals[j+j+1] );
  }
  return *this;
}

//------------------------------Insert---------------------------------------
// Insert or replace a key/value pair in the given dictionary.  If the
// dictionary is too full, it's size is doubled.  The prior value being
// replaced is returned (NULL if this is a 1st insertion of that key).  If
// an old value is found, it's swapped with the prior key-value pair on the
// list.  This moves a commonly searched-for value towards the list head.
const void *Dict::Insert(const void *key, const void *val) {
  int hash = _hash( key );      // Get hash key
  int i = hash & (_size-1);     // Get hash key, corrected for size
  bucket *b = &_bin[i];         // Handy shortcut
  for( int j=0; j<b->_cnt; j++ )
    if( !_cmp(key,b->_keyvals[j+j]) ) {
      const void *prior = b->_keyvals[j+j+1];
      b->_keyvals[j+j  ] = key; // Insert current key-value
      b->_keyvals[j+j+1] = val;
      return prior;             // Return prior
    }

  if( ++_cnt > _size ) {        // Hash table is full
    doubhash();                 // Grow whole table if too full
    i = hash & (_size-1);       // Rehash
    b = &_bin[i];               // Handy shortcut
  }
  if( b->_cnt == b->_max ) {    // Must grow bucket?
    if( !b->_keyvals ) {
      b->_max = 2;              // Initial bucket size
      b->_keyvals = (const void**)_arena->AmallocWords( sizeof(void *)*b->_max*2 );
    } else {
      b->_keyvals = (const void**)_arena->Arealloc( b->_keyvals, sizeof(void *)*b->_max*2, sizeof(void *)*b->_max*4 );
      b->_max <<= 1;            // Double bucket
    }
  }
  b->_keyvals[b->_cnt+b->_cnt  ] = key;
  b->_keyvals[b->_cnt+b->_cnt+1] = val;
  b->_cnt++;
  return NULL;                  // Nothing found prior
}

//------------------------------Delete---------------------------------------
// Find & remove a value from dictionary. Return old value.
const void *Dict::Delete(void *key) {
  int i = _hash( key ) & (_size-1);     // Get hash key, corrected for size
  bucket *b = &_bin[i];         // Handy shortcut
  for( int j=0; j<b->_cnt; j++ )
    if( !_cmp(key,b->_keyvals[j+j]) ) {
      const void *prior = b->_keyvals[j+j+1];
      b->_cnt--;                // Remove key/value from lo bucket
      b->_keyvals[j+j  ] = b->_keyvals[b->_cnt+b->_cnt  ];
      b->_keyvals[j+j+1] = b->_keyvals[b->_cnt+b->_cnt+1];
      _cnt--;                   // One less thing in table
      return prior;
    }
  return NULL;
}

//------------------------------FindDict-------------------------------------
// Find a key-value pair in the given dictionary.  If not found, return NULL.
// If found, move key-value pair towards head of list.
const void *Dict::operator [](const void *key) const {
  int i = _hash( key ) & (_size-1);     // Get hash key, corrected for size
  bucket *b = &_bin[i];         // Handy shortcut
  for( int j=0; j<b->_cnt; j++ )
    if( !_cmp(key,b->_keyvals[j+j]) )
      return b->_keyvals[j+j+1];
  return NULL;
}

//------------------------------CmpDict--------------------------------------
// CmpDict compares two dictionaries; they must have the same keys (their
// keys must match using CmpKey) and they must have the same values (pointer
// comparison).  If so 1 is returned, if not 0 is returned.
int Dict::operator ==(const Dict &d2) const {
  if( _cnt != d2._cnt ) return 0;
  if( _hash != d2._hash ) return 0;
  if( _cmp != d2._cmp ) return 0;
  for( int i=0; i < _size; i++) {       // For complete hash table do
    bucket *b = &_bin[i];       // Handy shortcut
    if( b->_cnt != d2._bin[i]._cnt ) return 0;
    if( memcmp(b->_keyvals, d2._bin[i]._keyvals, b->_cnt*2*sizeof(void*) ) )
      return 0;                 // Key-value pairs must match
  }
  return 1;                     // All match, is OK
}


//------------------------------print----------------------------------------
static void printvoid(const void* x) { printf("%p", x);  }
void Dict::print() {
  print(printvoid, printvoid);
}
void Dict::print(PrintKeyOrValue print_key, PrintKeyOrValue print_value) {
  for( int i=0; i < _size; i++) {       // For complete hash table do
    bucket *b = &_bin[i];       // Handy shortcut
    for( int j=0; j<b->_cnt; j++ ) {
      print_key(  b->_keyvals[j+j  ]);
      printf(" -> ");
      print_value(b->_keyvals[j+j+1]);
      printf("\n");
    }
  }
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
int hashstr(const void *t) {
  char c, k = 0;
  int sum = 0;
  const char *s = (const char *)t;

  while (((c = s[k]) != '\0') && (k < MAXID-1)) { // Get characters till nul
    c = (char) ((c << 1) + 1);    // Characters are always odd!
    sum += c + (c << shft[k++]);  // Universal hash function
  }
  assert(k < (MAXID), "Exceeded maximum name length");
  return (int)((sum+xsum[k]) >> 1); // Hash key, un-modulo'd table size
}

//------------------------------hashptr--------------------------------------
// Slimey cheap hash function; no guaranteed performance.  Better than the
// default for pointers, especially on MS-DOS machines.
int hashptr(const void *key) {
#ifdef __TURBOC__
    return (int)((intptr_t)key >> 16);
#else  // __TURBOC__
    return (int)((intptr_t)key >> 2);
#endif
}

// Slimey cheap hash function; no guaranteed performance.
int hashkey(const void *key) {
  return (int)((intptr_t)key);
}

//------------------------------Key Comparator Functions---------------------
int cmpstr(const void *k1, const void *k2) {
  return strcmp((const char *)k1,(const char *)k2);
}

// Cheap key comparator.
int cmpkey(const void *key1, const void *key2) {
  if (key1 == key2) return 0;
  intptr_t delta = (intptr_t)key1 - (intptr_t)key2;
  if (delta > 0) return 1;
  return -1;
}

//=============================================================================
//------------------------------reset------------------------------------------
// Create an iterator and initialize the first variables.
void DictI::reset( const Dict *dict ) {
  _d = dict;                    // The dictionary
  _i = (int)-1;         // Before the first bin
  _j = 0;                       // Nothing left in the current bin
  ++(*this);                    // Step to first real value
}

//------------------------------next-------------------------------------------
// Find the next key-value pair in the dictionary, or return a NULL key and
// value.
void DictI::operator ++(void) {
  if( _j-- ) {                  // Still working in current bin?
    _key   = _d->_bin[_i]._keyvals[_j+_j];
    _value = _d->_bin[_i]._keyvals[_j+_j+1];
    return;
  }

  while( ++_i < _d->_size ) {   // Else scan for non-zero bucket
    _j = _d->_bin[_i]._cnt;
    if( !_j ) continue;
    _j--;
    _key   = _d->_bin[_i]._keyvals[_j+_j];
    _value = _d->_bin[_i]._keyvals[_j+_j+1];
    return;
  }
  _key = _value = NULL;
}
