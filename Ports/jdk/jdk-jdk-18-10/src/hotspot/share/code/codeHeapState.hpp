/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2019 SAP SE. All rights reserved.
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

#ifndef SHARE_CODE_CODEHEAPSTATE_HPP
#define SHARE_CODE_CODEHEAPSTATE_HPP

#include "memory/heap.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

class CodeHeapState : public CHeapObj<mtCode> {

 public:
  enum compType {
    noComp = 0,     // must be! due to initialization by memset to zero
    c1,
    c2,
    jvmci,
    lastComp
  };

  enum blobType {
    noType = 0,             // must be! due to initialization by memset to zero
    // The nMethod_* values correspond to the CompiledMethod enum values.
    // We can't use the CompiledMethod values 1:1 because we depend on noType == 0.
    nMethod_inconstruction, // under construction. Very soon, the type will transition to "in_use".
                            // can't be observed while holding Compile_lock and CodeCache_lock simultaneously.
                            // left in here for completeness (and to document we spent a thought).
    nMethod_inuse,          // executable. This is the "normal" state for a nmethod.
    nMethod_notused,        // assumed inactive, marked not entrant. Could be revived if necessary.
    nMethod_notentrant,     // no new activations allowed, marked for deoptimization. Old activations may still exist.
                            // Will transition to "zombie" after all activations are gone.
    nMethod_zombie,         // No more activations exist, ready for purge (remove from code cache).
    nMethod_unloaded,       // No activations exist, should not be called. Transient state on the way to "zombie".
    nMethod_alive = nMethod_notentrant, // Combined state: nmethod may have activations, thus can't be purged.
    nMethod_dead  = nMethod_zombie,     // Combined state: nmethod does not have any activations.
    runtimeStub   = nMethod_unloaded + 1,
    ricochetStub,
    deoptimizationStub,
    uncommonTrapStub,
    exceptionStub,
    safepointStub,
    adapterBlob,
    mh_adapterBlob,
    bufferBlob,
    lastType
  };

 private:
  static void prepare_StatArray(outputStream* out, size_t nElem, size_t granularity, const char* heapName);
  static void prepare_FreeArray(outputStream* out, unsigned int nElem, const char* heapName);
  static void prepare_TopSizeArray(outputStream* out, unsigned int nElem, const char* heapName);
  static void prepare_SizeDistArray(outputStream* out, unsigned int nElem, const char* heapName);
  static void discard_StatArray(outputStream* out);
  static void discard_FreeArray(outputStream* out);
  static void discard_TopSizeArray(outputStream* out);
  static void discard_SizeDistArray(outputStream* out);

  static void update_SizeDistArray(outputStream* out, unsigned int len);

  static const char* get_heapName(CodeHeap* heap);
  static unsigned int findHeapIndex(outputStream* out, const char* heapName);
  static void get_HeapStatGlobals(outputStream* out, const char* heapName);
  static void set_HeapStatGlobals(outputStream* out, const char* heapName);

  static void printBox(outputStream* out, const char border, const char* text1, const char* text2);
  static void print_blobType_legend(outputStream* out);
  static void print_space_legend(outputStream* out);
  static void print_age_legend(outputStream* out);
  static void print_blobType_single(outputStream *ast, u2 /* blobType */ type);
  static void print_count_single(outputStream *ast, unsigned short count);
  static void print_space_single(outputStream *ast, unsigned short space);
  static void print_age_single(outputStream *ast, unsigned int age);
  static void print_line_delim(outputStream* out, bufferedStream *sst, char* low_bound, unsigned int ix, unsigned int gpl);
  static void print_line_delim(outputStream* out, outputStream *sst, char* low_bound, unsigned int ix, unsigned int gpl);
  static blobType get_cbType(CodeBlob* cb);
  static bool blob_access_is_safe(CodeBlob* this_blob);
  static bool nmethod_access_is_safe(nmethod* nm);
  static bool holding_required_locks();

 public:
  static void discard(outputStream* out, CodeHeap* heap);
  static void aggregate(outputStream* out, CodeHeap* heap, size_t granularity);
  static void print_usedSpace(outputStream* out, CodeHeap* heap);
  static void print_freeSpace(outputStream* out, CodeHeap* heap);
  static void print_count(outputStream* out, CodeHeap* heap);
  static void print_space(outputStream* out, CodeHeap* heap);
  static void print_age(outputStream* out, CodeHeap* heap);
  static void print_names(outputStream* out, CodeHeap* heap);
};

//----------------
//  StatElement
//----------------
//  Each analysis granule is represented by an instance of
//  this StatElement struct. It collects and aggregates all
//  information describing the allocated contents of the granule.
//  Free (unallocated) contents is not considered (see FreeBlk for that).
//  All StatElements of a heap segment are stored in the related StatArray.
//  Current size: 40 bytes + 8 bytes class header.
class StatElement : public CHeapObj<mtCode> {
  public:
    // A note on ages: The compilation_id easily overflows unsigned short in large systems
    unsigned int       t1_age;      // oldest compilation_id of tier1 nMethods.
    unsigned int       t2_age;      // oldest compilation_id of tier2 nMethods.
    unsigned int       tx_age;      // oldest compilation_id of inactive/not entrant nMethods.
    unsigned short     t1_space;    // in units of _segment_size to "prevent" overflow
    unsigned short     t2_space;    // in units of _segment_size to "prevent" overflow
    unsigned short     tx_space;    // in units of _segment_size to "prevent" overflow
    unsigned short     dead_space;  // in units of _segment_size to "prevent" overflow
    unsigned short     stub_space;  // in units of _segment_size to "prevent" overflow
    unsigned short     t1_count;
    unsigned short     t2_count;
    unsigned short     tx_count;
    unsigned short     dead_count;
    unsigned short     stub_count;
    CompLevel          level;       // optimization level (see globalDefinitions.hpp)
    //---<  replaced the correct enum typing with u2 to save space.
    u2                 compiler;    // compiler which generated this blob. Type is CodeHeapState::compType
    u2                 type;        // used only if granularity == segment_size. Type is CodeHeapState::blobType
};

//-----------
//  FreeBlk
//-----------
//  Each free block in the code heap is represented by an instance
//  of this FreeBlk struct. It collects all information we need to
//  know about each free block.
//  All FreeBlks of a heap segment are stored in the related FreeArray.
struct FreeBlk : public CHeapObj<mtCode> {
  HeapBlock*     start;       // address of free block
  unsigned int   len;          // length of free block

  unsigned int   gap;          // gap to next free block
  unsigned int   index;        // sequential number of free block
  unsigned short n_gapBlocks;  // # used blocks in gap
  bool           stubs_in_gap; // The occupied space between this and the next free block contains (unmovable) stubs or blobs.
};

//--------------
//  TopSizeBlk
//--------------
//  The n largest blocks in the code heap are represented in an instance
//  of this TopSizeBlk struct. It collects all information we need to
//  know about those largest blocks.
//  All TopSizeBlks of a heap segment are stored in the related TopSizeArray.
struct TopSizeBlk : public CHeapObj<mtCode> {
  HeapBlock*     start;        // address of block
  const char*    blob_name;    // name of blob (mostly: name_and_sig of nmethod)
  unsigned int   len;          // length of block, in _segment_size units. Will never overflow int.

  unsigned int   index;        // ordering index, 0 is largest block
                               // contains array index of next smaller block
                               // -1 indicates end of list

  unsigned int   nm_size;      // nmeethod total size (if nmethod, 0 otherwise)
  int            temperature;  // nmethod temperature (if nmethod, 0 otherwise)
  CompLevel      level;        // optimization level (see globalDefinitions.hpp)
  u2             compiler;     // compiler which generated this blob
  u2             type;         // blob type
};

//---------------------------
//  SizeDistributionElement
//---------------------------
//  During CodeHeap analysis, each allocated code block is associated with a
//  SizeDistributionElement according to its size. Later on, the array of
//  SizeDistributionElements is used to print a size distribution bar graph.
//  All SizeDistributionElements of a heap segment are stored in the related SizeDistributionArray.
struct SizeDistributionElement : public CHeapObj<mtCode> {
                               // Range is [rangeStart..rangeEnd).
  unsigned int   rangeStart;   // start of length range, in _segment_size units.
  unsigned int   rangeEnd;     // end   of length range, in _segment_size units.
  unsigned int   lenSum;       // length of block, in _segment_size units. Will never overflow int.

  unsigned int   count;        // number of blocks assigned to this range.
};

//----------------
//  CodeHeapStat
//----------------
//  Because we have to deal with multiple CodeHeaps, we need to
//  collect "global" information in a segment-specific way as well.
//  Thats what the CodeHeapStat and CodeHeapStatArray are used for.
//  Before a heap segment is processed, the contents of the CodeHeapStat
//  element is copied to the global variables (get_HeapStatGlobals).
//  When processing is done, the possibly modified global variables are
//  copied back (set_HeapStatGlobals) to the CodeHeapStat element.
struct CodeHeapStat {
    StatElement*                     StatArray;
    struct FreeBlk*                  FreeArray;
    struct TopSizeBlk*               TopSizeArray;
    struct SizeDistributionElement*  SizeDistributionArray;
    const char*                      heapName;
    size_t                           segment_size;
    // StatElement data
    size_t        alloc_granules;
    size_t        granule_size;
    bool          segment_granules;
    unsigned int  nBlocks_t1;
    unsigned int  nBlocks_t2;
    unsigned int  nBlocks_alive;
    unsigned int  nBlocks_dead;
    unsigned int  nBlocks_unloaded;
    unsigned int  nBlocks_stub;
    // FreeBlk data
    unsigned int  alloc_freeBlocks;
    // UsedBlk data
    unsigned int  alloc_topSizeBlocks;
    unsigned int  used_topSizeBlocks;
    // method hotness data. Temperature range is [-reset_val..+reset_val]
    int           avgTemp;
    int           maxTemp;
    int           minTemp;
};

#endif // SHARE_CODE_CODEHEAPSTATE_HPP
