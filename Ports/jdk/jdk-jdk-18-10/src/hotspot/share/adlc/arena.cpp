/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "adlc.hpp"

void* AllocateHeap(size_t size) {
  unsigned char* ptr = (unsigned char*) malloc(size);
  if (ptr == NULL && size != 0) {
    fprintf(stderr, "Error: Out of memory in ADLC\n"); // logging can cause crash!
    fflush(stderr);
    exit(1);
  }
  return ptr;
}

void* ReAllocateHeap(void* old_ptr, size_t size) {
  unsigned char* ptr = (unsigned char*) realloc(old_ptr, size);
  if (ptr == NULL && size != 0) {
    fprintf(stderr, "Error: Out of memory in ADLC\n"); // logging can cause crash!
    fflush(stderr);
    exit(1);
  }
  return ptr;
}

void* Chunk::operator new(size_t requested_size, size_t length) throw() {
  return CHeapObj::operator new(requested_size + length);
}

void  Chunk::operator delete(void* p, size_t length) {
  CHeapObj::operator delete(p);
}

Chunk::Chunk(size_t length) {
  _next = NULL;         // Chain on the linked list
  _len  = length;       // Save actual size
}

//------------------------------chop-------------------------------------------
void Chunk::chop() {
  Chunk *k = this;
  while( k ) {
    Chunk *tmp = k->_next;
    // clear out this chunk (to detect allocation bugs)
    memset(k, 0xBE, k->_len);
    free(k);                    // Free chunk (was malloc'd)
    k = tmp;
  }
}

void Chunk::next_chop() {
  _next->chop();
  _next = NULL;
}

//------------------------------Arena------------------------------------------
Arena::Arena( size_t init_size ) {
  init_size = (init_size+3) & ~3;
  _first = _chunk = new (init_size) Chunk(init_size);
  _hwm = _chunk->bottom();      // Save the cached hwm, max
  _max = _chunk->top();
  set_size_in_bytes(init_size);
}

Arena::Arena() {
  _first = _chunk = new (Chunk::init_size) Chunk(Chunk::init_size);
  _hwm = _chunk->bottom();      // Save the cached hwm, max
  _max = _chunk->top();
  set_size_in_bytes(Chunk::init_size);
}

Arena::Arena( Arena *a )
: _chunk(a->_chunk), _hwm(a->_hwm), _max(a->_max), _first(a->_first) {
  set_size_in_bytes(a->size_in_bytes());
}

//------------------------------used-------------------------------------------
// Total of all Chunks in arena
size_t Arena::used() const {
  size_t sum = _chunk->_len - (_max-_hwm); // Size leftover in this Chunk
  Chunk *k = _first;
  while( k != _chunk) {         // Whilst have Chunks in a row
    sum += k->_len;             // Total size of this Chunk
    k = k->_next;               // Bump along to next Chunk
  }
  return sum;                   // Return total consumed space.
}

//------------------------------grow-------------------------------------------
// Grow a new Chunk
void* Arena::grow( size_t x ) {
  // Get minimal required size.  Either real big, or even bigger for giant objs
  size_t len = max(x, Chunk::size);

  Chunk *k = _chunk;            // Get filled-up chunk address
  _chunk = new (len) Chunk(len);

  if( k ) k->_next = _chunk;    // Append new chunk to end of linked list
  else _first = _chunk;
  _hwm  = _chunk->bottom();     // Save the cached hwm, max
  _max =  _chunk->top();
  set_size_in_bytes(size_in_bytes() + len);
  void* result = _hwm;
  _hwm += x;
  return result;
}

//------------------------------calloc-----------------------------------------
// Allocate zeroed storage in Arena
void *Arena::Acalloc( size_t items, size_t x ) {
  size_t z = items*x;   // Total size needed
  void *ptr = Amalloc(z);       // Get space
  memset( ptr, 0, z );          // Zap space
  return ptr;                   // Return space
}

//------------------------------realloc----------------------------------------
// Reallocate storage in Arena.
void *Arena::Arealloc( void *old_ptr, size_t old_size, size_t new_size ) {
  char *c_old = (char*)old_ptr; // Handy name
  // Stupid fast special case
  if( new_size <= old_size ) {  // Shrink in-place
    if( c_old+old_size == _hwm) // Attempt to free the excess bytes
      _hwm = c_old+new_size;    // Adjust hwm
    return c_old;
  }

  // See if we can resize in-place
  if( (c_old+old_size == _hwm) &&       // Adjusting recent thing
      (c_old+new_size <= _max) ) {      // Still fits where it sits
    _hwm = c_old+new_size;      // Adjust hwm
    return c_old;               // Return old pointer
  }

  // Oops, got to relocate guts
  void *new_ptr = Amalloc(new_size);
  memcpy( new_ptr, c_old, old_size );
  Afree(c_old,old_size);        // Mostly done to keep stats accurate
  return new_ptr;
}

//------------------------------reset------------------------------------------
// Reset this Arena to empty, and return this Arenas guts in a new Arena.
Arena *Arena::reset(void) {
  Arena *a = new Arena(this);   // New empty arena
  _first = _chunk = NULL;       // Normal, new-arena initialization
  _hwm = _max = NULL;
  return a;                     // Return Arena with guts
}

//------------------------------contains---------------------------------------
// Determine if pointer belongs to this Arena or not.
bool Arena::contains( const void *ptr ) const {
  if( (void*)_chunk->bottom() <= ptr && ptr < (void*)_hwm )
    return true;                // Check for in this chunk
  for( Chunk *c = _first; c; c = c->_next )
    if( (void*)c->bottom() <= ptr && ptr < (void*)c->top())
      return true;              // Check for every chunk in Arena
  return false;                 // Not in any Chunk, so not in Arena
}

//-----------------------------------------------------------------------------
// CHeapObj

void* CHeapObj::operator new(size_t size) throw() {
  return (void *) AllocateHeap(size);
}

void CHeapObj::operator delete(void* p){
 free(p);
}
