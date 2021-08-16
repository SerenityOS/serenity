/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017, 2021 SAP SE. All rights reserved.
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
#ifndef SHARE_MEMORY_METASPACE_HPP
#define SHARE_MEMORY_METASPACE_HPP

#include "memory/allocation.hpp"
#include "runtime/globals.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"

class ClassLoaderData;
class MetaspaceShared;
class MetaspaceTracer;
class Mutex;
class outputStream;
class ReservedSpace;

////////////////// Metaspace ///////////////////////

// Namespace for important central static functions
// (auxiliary stuff goes into MetaspaceUtils)
class Metaspace : public AllStatic {

  friend class MetaspaceShared;

public:
  enum MetadataType {
    ClassType,
    NonClassType,
    MetadataTypeCount
  };
  enum MetaspaceType {
    ZeroMetaspaceType = 0,
    StandardMetaspaceType = ZeroMetaspaceType,
    BootMetaspaceType = StandardMetaspaceType + 1,
    ClassMirrorHolderMetaspaceType = BootMetaspaceType + 1,
    ReflectionMetaspaceType = ClassMirrorHolderMetaspaceType + 1,
    MetaspaceTypeCount
  };

private:

  static const MetaspaceTracer* _tracer;

  static bool _initialized;

public:

  static const MetaspaceTracer* tracer() { return _tracer; }

 private:

#ifdef _LP64

  // Reserve a range of memory at an address suitable for en/decoding narrow
  // Klass pointers (see: CompressedClassPointers::is_valid_base()).
  // The returned address shall both be suitable as a compressed class pointers
  //  base, and aligned to Metaspace::reserve_alignment (which is equal to or a
  //  multiple of allocation granularity).
  // On error, returns an unreserved space.
  static ReservedSpace reserve_address_space_for_compressed_classes(size_t size);

  // Given a prereserved space, use that to set up the compressed class space list.
  static void initialize_class_space(ReservedSpace rs);

  // Returns true if class space has been setup (initialize_class_space).
  static bool class_space_is_initialized();

#endif

 public:

  static void ergo_initialize();
  static void global_initialize();
  static void post_initialize();

  // Alignment, in bytes, of metaspace mappings
  static size_t reserve_alignment()       { return reserve_alignment_words() * BytesPerWord; }
  // Alignment, in words, of metaspace mappings
  static size_t reserve_alignment_words();

  // The granularity at which Metaspace is committed and uncommitted.
  // (Todo: Why does this have to be exposed?)
  static size_t commit_alignment()        { return commit_alignment_words() * BytesPerWord; }
  static size_t commit_alignment_words();

  // The largest possible single allocation
  static size_t max_allocation_word_size();

  static MetaWord* allocate(ClassLoaderData* loader_data, size_t word_size,
                            MetaspaceObj::Type type, TRAPS);

  // Non-TRAPS version of allocate which can be called by a non-Java thread, that returns
  // NULL on failure.
  static MetaWord* allocate(ClassLoaderData* loader_data, size_t word_size,
                            MetaspaceObj::Type type);

  static bool contains(const void* ptr);
  static bool contains_non_shared(const void* ptr);

  // Free empty virtualspaces
  static void purge();

  static void report_metadata_oome(ClassLoaderData* loader_data, size_t word_size,
                                   MetaspaceObj::Type type, MetadataType mdtype, TRAPS);

  static const char* metadata_type_name(Metaspace::MetadataType mdtype);

  static void print_compressed_class_space(outputStream* st) NOT_LP64({});

  // Return TRUE only if UseCompressedClassPointers is True.
  static bool using_class_space() {
    return NOT_LP64(false) LP64_ONLY(UseCompressedClassPointers);
  }

  static bool is_class_space_allocation(MetadataType mdType) {
    return mdType == ClassType && using_class_space();
  }

  static bool initialized();

};


#endif // SHARE_MEMORY_METASPACE_HPP
