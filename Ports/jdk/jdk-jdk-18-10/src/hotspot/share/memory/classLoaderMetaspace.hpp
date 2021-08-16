/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_MEMORY_CLASSLOADERMETASPACE_HPP
#define SHARE_MEMORY_CLASSLOADERMETASPACE_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {
  struct ClmsStats;
  class MetaspaceArena;
}

// A ClassLoaderMetaspace manages MetaspaceArena(s) for a CLD.
//
// A CLD owns one MetaspaceArena if UseCompressedClassPointers is false. Otherwise
// it owns two - one for the Klass* objects from the class space, one for the other
// types of MetaspaceObjs from the non-class space.
//
// +------+       +----------------------+       +-------------------+
// | CLD  | --->  | ClassLoaderMetaspace | ----> | (non class) Arena |
// +------+       +----------------------+  |    +-------------------+     allocation top
//                                          |       |                        v
//                                          |       + chunk -- chunk ... -- chunk
//                                          |
//                                          |    +-------------------+
//                                          +--> | (class) Arena     |
//                                               +-------------------+
//                                                  |
//                                                  + chunk ... chunk
//                                                               ^
//                                                               alloc top
//
class ClassLoaderMetaspace : public CHeapObj<mtClass> {

  // A reference to an outside lock, held by the CLD.
  Mutex* const _lock;

  const Metaspace::MetaspaceType _space_type;

  // Arena for allocations from non-class  metaspace
  //  (resp. for all allocations if -XX:-UseCompressedClassPointers).
  metaspace::MetaspaceArena* _non_class_space_arena;

  // Arena for allocations from class space
  //  (NULL if -XX:-UseCompressedClassPointers).
  metaspace::MetaspaceArena* _class_space_arena;

  Mutex* lock() const                             { return _lock; }
  metaspace::MetaspaceArena* non_class_space_arena() const   { return _non_class_space_arena; }
  metaspace::MetaspaceArena* class_space_arena() const       { return _class_space_arena; }

public:

  ClassLoaderMetaspace(Mutex* lock, Metaspace::MetaspaceType space_type);

  ~ClassLoaderMetaspace();

  Metaspace::MetaspaceType space_type() const { return _space_type; }

  // Allocate word_size words from Metaspace.
  MetaWord* allocate(size_t word_size, Metaspace::MetadataType mdType);

  // Attempt to expand the GC threshold to be good for at least another word_size words
  // and allocate. Returns NULL if failure. Used during Metaspace GC.
  MetaWord* expand_and_allocate(size_t word_size, Metaspace::MetadataType mdType);

  // Prematurely returns a metaspace allocation to the _block_freelists
  // because it is not needed anymore.
  void deallocate(MetaWord* ptr, size_t word_size, bool is_class);

  // Update statistics. This walks all in-use chunks.
  void add_to_statistics(metaspace::ClmsStats* out) const;

  DEBUG_ONLY(void verify() const;)

  // This only exists for JFR and jcmd VM.classloader_stats. We may want to
  //  change this. Capacity as a stat is of questionable use since it may
  //  contain committed and uncommitted areas. For now we do this to maintain
  //  backward compatibility with JFR.
  void calculate_jfr_stats(size_t* p_used_bytes, size_t* p_capacity_bytes) const;

}; // end: ClassLoaderMetaspace


#endif // SHARE_MEMORY_CLASSLOADERMETASPACE_HPP
