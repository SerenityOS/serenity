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

#include "precompiled.hpp"
#include "code/codeHeapState.hpp"
#include "compiler/compileBroker.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/sweeper.hpp"
#include "utilities/powerOfTwo.hpp"

// -------------------------
// |  General Description  |
// -------------------------
// The CodeHeap state analytics are divided in two parts.
// The first part examines the entire CodeHeap and aggregates all
// information that is believed useful/important.
//
// Aggregation condenses the information of a piece of the CodeHeap
// (4096 bytes by default) into an analysis granule. These granules
// contain enough detail to gain initial insight while keeping the
// internal structure sizes in check.
//
// The second part, which consists of several, independent steps,
// prints the previously collected information with emphasis on
// various aspects.
//
// The CodeHeap is a living thing. Therefore, protection against concurrent
// modification (by acquiring the CodeCache_lock) is necessary. It has
// to be provided by the caller of the analysis functions.
// If the CodeCache_lock is not held, the analysis functions may print
// less detailed information or may just do nothing. It is by intention
// that an unprotected invocation is not abnormally terminated.
//
// Data collection and printing is done on an "on request" basis.
// While no request is being processed, there is no impact on performance.
// The CodeHeap state analytics do have some memory footprint.
// The "aggregate" step allocates some data structures to hold the aggregated
// information for later output. These data structures live until they are
// explicitly discarded (function "discard") or until the VM terminates.
// There is one exception: the function "all" does not leave any data
// structures allocated.
//
// Requests for real-time, on-the-fly analysis can be issued via
//   jcmd <pid> Compiler.CodeHeap_Analytics [<function>] [<granularity>]
//
// If you are (only) interested in how the CodeHeap looks like after running
// a sample workload, you can use the command line option
//   -XX:+PrintCodeHeapAnalytics
// It will cause a full analysis to be written to tty. In addition, a full
// analysis will be written the first time a "CodeCache full" condition is
// detected.
//
// The command line option produces output identical to the jcmd function
//   jcmd <pid> Compiler.CodeHeap_Analytics all 4096
// ---------------------------------------------------------------------------------

// With this declaration macro, it is possible to switch between
//  - direct output into an argument-passed outputStream and
//  - buffered output into a bufferedStream with subsequent flush
//    of the filled buffer to the outputStream.
#define USE_BUFFEREDSTREAM

// There are instances when composing an output line or a small set of
// output lines out of many tty->print() calls creates significant overhead.
// Writing to a bufferedStream buffer first has a significant advantage:
// It uses noticeably less cpu cycles and reduces (when writing to a
// network file) the required bandwidth by at least a factor of ten. Observed on MacOS.
// That clearly makes up for the increased code complexity.
//
// Conversion of existing code is easy and straightforward, if the code already
// uses a parameterized output destination, e.g. "outputStream st".
//  - rename the formal parameter to any other name, e.g. out_st.
//  - at a suitable place in your code, insert
//      BUFFEREDSTEAM_DECL(buf_st, out_st)
// This will provide all the declarations necessary. After that, all
// buf_st->print() (and the like) calls will be directed to a bufferedStream object.
// Once a block of output (a line or a small set of lines) is composed, insert
//      BUFFEREDSTREAM_FLUSH(termstring)
// to flush the bufferedStream to the final destination out_st. termstring is just
// an arbitrary string (e.g. "\n") which is appended to the bufferedStream before
// being written to out_st. Be aware that the last character written MUST be a '\n'.
// Otherwise, buf_st->position() does not correspond to out_st->position() any longer.
//      BUFFEREDSTREAM_FLUSH_LOCKED(termstring)
// does the same thing, protected by the ttyLocker lock.
//      BUFFEREDSTREAM_FLUSH_IF(termstring, remSize)
// does a flush only if the remaining buffer space is less than remSize.
//
// To activate, #define USE_BUFFERED_STREAM before including this header.
// If not activated, output will directly go to the originally used outputStream
// with no additional overhead.
//
#if defined(USE_BUFFEREDSTREAM)
// All necessary declarations to print via a bufferedStream
// This macro must be placed before any other BUFFEREDSTREAM*
// macro in the function.
#define BUFFEREDSTREAM_DECL_SIZE(_anyst, _outst, _capa)       \
    ResourceMark         _rm;                                 \
    /* _anyst  name of the stream as used in the code */      \
    /* _outst  stream where final output will go to   */      \
    /* _capa   allocated capacity of stream buffer    */      \
    size_t           _nflush = 0;                             \
    size_t     _nforcedflush = 0;                             \
    size_t      _nsavedflush = 0;                             \
    size_t     _nlockedflush = 0;                             \
    size_t     _nflush_bytes = 0;                             \
    size_t         _capacity = _capa;                         \
    bufferedStream   _sstobj(_capa);                          \
    bufferedStream*  _sstbuf = &_sstobj;                      \
    outputStream*    _outbuf = _outst;                        \
    bufferedStream*   _anyst = &_sstobj; /* any stream. Use this to just print - no buffer flush.  */

// Same as above, but with fixed buffer size.
#define BUFFEREDSTREAM_DECL(_anyst, _outst)                   \
    BUFFEREDSTREAM_DECL_SIZE(_anyst, _outst, 4*K);

// Flush the buffer contents unconditionally.
// No action if the buffer is empty.
#define BUFFEREDSTREAM_FLUSH(_termString)                     \
    if (((_termString) != NULL) && (strlen(_termString) > 0)){\
      _sstbuf->print("%s", _termString);                      \
    }                                                         \
    if (_sstbuf != _outbuf) {                                 \
      if (_sstbuf->size() != 0) {                             \
        _nforcedflush++; _nflush_bytes += _sstbuf->size();    \
        _outbuf->print("%s", _sstbuf->as_string());           \
        _sstbuf->reset();                                     \
      }                                                       \
    }

// Flush the buffer contents if the remaining capacity is
// less than the given threshold.
#define BUFFEREDSTREAM_FLUSH_IF(_termString, _remSize)        \
    if (((_termString) != NULL) && (strlen(_termString) > 0)){\
      _sstbuf->print("%s", _termString);                      \
    }                                                         \
    if (_sstbuf != _outbuf) {                                 \
      if ((_capacity - _sstbuf->size()) < (size_t)(_remSize)){\
        _nflush++; _nforcedflush--;                           \
        BUFFEREDSTREAM_FLUSH("")                              \
      } else {                                                \
        _nsavedflush++;                                       \
      }                                                       \
    }

// Flush the buffer contents if the remaining capacity is less
// than the calculated threshold (256 bytes + capacity/16)
// That should suffice for all reasonably sized output lines.
#define BUFFEREDSTREAM_FLUSH_AUTO(_termString)                \
    BUFFEREDSTREAM_FLUSH_IF(_termString, 256+(_capacity>>4))

#define BUFFEREDSTREAM_FLUSH_LOCKED(_termString)              \
    { ttyLocker ttyl;/* keep this output block together */    \
      _nlockedflush++;                                        \
      BUFFEREDSTREAM_FLUSH(_termString)                       \
    }

// #define BUFFEREDSTREAM_FLUSH_STAT()                           \
//     if (_sstbuf != _outbuf) {                                 \
//       _outbuf->print_cr("%ld flushes (buffer full), %ld forced, %ld locked, %ld bytes total, %ld flushes saved", _nflush, _nforcedflush, _nlockedflush, _nflush_bytes, _nsavedflush); \
//    }

#define BUFFEREDSTREAM_FLUSH_STAT()
#else
#define BUFFEREDSTREAM_DECL_SIZE(_anyst, _outst, _capa)       \
    size_t       _capacity = _capa;                           \
    outputStream*  _outbuf = _outst;                          \
    outputStream*  _anyst  = _outst;   /* any stream. Use this to just print - no buffer flush.  */

#define BUFFEREDSTREAM_DECL(_anyst, _outst)                   \
    BUFFEREDSTREAM_DECL_SIZE(_anyst, _outst, 4*K)

#define BUFFEREDSTREAM_FLUSH(_termString)                     \
    if (((_termString) != NULL) && (strlen(_termString) > 0)){\
      _outbuf->print("%s", _termString);                      \
    }

#define BUFFEREDSTREAM_FLUSH_IF(_termString, _remSize)        \
    BUFFEREDSTREAM_FLUSH(_termString)

#define BUFFEREDSTREAM_FLUSH_AUTO(_termString)                \
    BUFFEREDSTREAM_FLUSH(_termString)

#define BUFFEREDSTREAM_FLUSH_LOCKED(_termString)              \
    BUFFEREDSTREAM_FLUSH(_termString)

#define BUFFEREDSTREAM_FLUSH_STAT()
#endif
#define HEX32_FORMAT  "0x%x"  // just a helper format string used below multiple times

const char  blobTypeChar[] = {' ', 'C', 'N', 'I', 'X', 'Z', 'U', 'R', '?', 'D', 'T', 'E', 'S', 'A', 'M', 'B', 'L' };
const char* blobTypeName[] = {"noType"
                             ,     "nMethod (under construction), cannot be observed"
                             ,          "nMethod (active)"
                             ,               "nMethod (inactive)"
                             ,                    "nMethod (deopt)"
                             ,                         "nMethod (zombie)"
                             ,                              "nMethod (unloaded)"
                             ,                                   "runtime stub"
                             ,                                        "ricochet stub"
                             ,                                             "deopt stub"
                             ,                                                  "uncommon trap stub"
                             ,                                                       "exception stub"
                             ,                                                            "safepoint stub"
                             ,                                                                 "adapter blob"
                             ,                                                                      "MH adapter blob"
                             ,                                                                           "buffer blob"
                             ,                                                                                "lastType"
                             };
const char* compTypeName[] = { "none", "c1", "c2", "jvmci" };

// Be prepared for ten different CodeHeap segments. Should be enough for a few years.
const  unsigned int        nSizeDistElements = 31;  // logarithmic range growth, max size: 2**32
const  unsigned int        maxTopSizeBlocks  = 100;
const  unsigned int        tsbStopper        = 2 * maxTopSizeBlocks;
const  unsigned int        maxHeaps          = 10;
static unsigned int        nHeaps            = 0;
static struct CodeHeapStat CodeHeapStatArray[maxHeaps];

// static struct StatElement *StatArray      = NULL;
static StatElement* StatArray             = NULL;
static int          log2_seg_size         = 0;
static size_t       seg_size              = 0;
static size_t       alloc_granules        = 0;
static size_t       granule_size          = 0;
static bool         segment_granules      = false;
static unsigned int nBlocks_t1            = 0;  // counting "in_use" nmethods only.
static unsigned int nBlocks_t2            = 0;  // counting "in_use" nmethods only.
static unsigned int nBlocks_alive         = 0;  // counting "not_used" and "not_entrant" nmethods only.
static unsigned int nBlocks_dead          = 0;  // counting "zombie" and "unloaded" methods only.
static unsigned int nBlocks_unloaded      = 0;  // counting "unloaded" nmethods only. This is a transient state.
static unsigned int nBlocks_stub          = 0;

static struct FreeBlk*          FreeArray = NULL;
static unsigned int      alloc_freeBlocks = 0;

static struct TopSizeBlk*    TopSizeArray = NULL;
static unsigned int   alloc_topSizeBlocks = 0;
static unsigned int    used_topSizeBlocks = 0;

static struct SizeDistributionElement*  SizeDistributionArray = NULL;

// nMethod temperature (hotness) indicators.
static int                     avgTemp    = 0;
static int                     maxTemp    = 0;
static int                     minTemp    = 0;

static unsigned int  latest_compilation_id   = 0;
static volatile bool initialization_complete = false;

const char* CodeHeapState::get_heapName(CodeHeap* heap) {
  if (SegmentedCodeCache) {
    return heap->name();
  } else {
    return "CodeHeap";
  }
}

// returns the index for the heap being processed.
unsigned int CodeHeapState::findHeapIndex(outputStream* out, const char* heapName) {
  if (heapName == NULL) {
    return maxHeaps;
  }
  if (SegmentedCodeCache) {
    // Search for a pre-existing entry. If found, return that index.
    for (unsigned int i = 0; i < nHeaps; i++) {
      if (CodeHeapStatArray[i].heapName != NULL && strcmp(heapName, CodeHeapStatArray[i].heapName) == 0) {
        return i;
      }
    }

    // check if there are more code heap segments than we can handle.
    if (nHeaps == maxHeaps) {
      out->print_cr("Too many heap segments for current limit(%d).", maxHeaps);
      return maxHeaps;
    }

    // allocate new slot in StatArray.
    CodeHeapStatArray[nHeaps].heapName = heapName;
    return nHeaps++;
  } else {
    nHeaps = 1;
    CodeHeapStatArray[0].heapName = heapName;
    return 0; // This is the default index if CodeCache is not segmented.
  }
}

void CodeHeapState::get_HeapStatGlobals(outputStream* out, const char* heapName) {
  unsigned int ix = findHeapIndex(out, heapName);
  if (ix < maxHeaps) {
    StatArray             = CodeHeapStatArray[ix].StatArray;
    seg_size              = CodeHeapStatArray[ix].segment_size;
    log2_seg_size         = seg_size == 0 ? 0 : exact_log2(seg_size);
    alloc_granules        = CodeHeapStatArray[ix].alloc_granules;
    granule_size          = CodeHeapStatArray[ix].granule_size;
    segment_granules      = CodeHeapStatArray[ix].segment_granules;
    nBlocks_t1            = CodeHeapStatArray[ix].nBlocks_t1;
    nBlocks_t2            = CodeHeapStatArray[ix].nBlocks_t2;
    nBlocks_alive         = CodeHeapStatArray[ix].nBlocks_alive;
    nBlocks_dead          = CodeHeapStatArray[ix].nBlocks_dead;
    nBlocks_unloaded      = CodeHeapStatArray[ix].nBlocks_unloaded;
    nBlocks_stub          = CodeHeapStatArray[ix].nBlocks_stub;
    FreeArray             = CodeHeapStatArray[ix].FreeArray;
    alloc_freeBlocks      = CodeHeapStatArray[ix].alloc_freeBlocks;
    TopSizeArray          = CodeHeapStatArray[ix].TopSizeArray;
    alloc_topSizeBlocks   = CodeHeapStatArray[ix].alloc_topSizeBlocks;
    used_topSizeBlocks    = CodeHeapStatArray[ix].used_topSizeBlocks;
    SizeDistributionArray = CodeHeapStatArray[ix].SizeDistributionArray;
    avgTemp               = CodeHeapStatArray[ix].avgTemp;
    maxTemp               = CodeHeapStatArray[ix].maxTemp;
    minTemp               = CodeHeapStatArray[ix].minTemp;
  } else {
    StatArray             = NULL;
    seg_size              = 0;
    log2_seg_size         = 0;
    alloc_granules        = 0;
    granule_size          = 0;
    segment_granules      = false;
    nBlocks_t1            = 0;
    nBlocks_t2            = 0;
    nBlocks_alive         = 0;
    nBlocks_dead          = 0;
    nBlocks_unloaded      = 0;
    nBlocks_stub          = 0;
    FreeArray             = NULL;
    alloc_freeBlocks      = 0;
    TopSizeArray          = NULL;
    alloc_topSizeBlocks   = 0;
    used_topSizeBlocks    = 0;
    SizeDistributionArray = NULL;
    avgTemp               = 0;
    maxTemp               = 0;
    minTemp               = 0;
  }
}

void CodeHeapState::set_HeapStatGlobals(outputStream* out, const char* heapName) {
  unsigned int ix = findHeapIndex(out, heapName);
  if (ix < maxHeaps) {
    CodeHeapStatArray[ix].StatArray             = StatArray;
    CodeHeapStatArray[ix].segment_size          = seg_size;
    CodeHeapStatArray[ix].alloc_granules        = alloc_granules;
    CodeHeapStatArray[ix].granule_size          = granule_size;
    CodeHeapStatArray[ix].segment_granules      = segment_granules;
    CodeHeapStatArray[ix].nBlocks_t1            = nBlocks_t1;
    CodeHeapStatArray[ix].nBlocks_t2            = nBlocks_t2;
    CodeHeapStatArray[ix].nBlocks_alive         = nBlocks_alive;
    CodeHeapStatArray[ix].nBlocks_dead          = nBlocks_dead;
    CodeHeapStatArray[ix].nBlocks_unloaded      = nBlocks_unloaded;
    CodeHeapStatArray[ix].nBlocks_stub          = nBlocks_stub;
    CodeHeapStatArray[ix].FreeArray             = FreeArray;
    CodeHeapStatArray[ix].alloc_freeBlocks      = alloc_freeBlocks;
    CodeHeapStatArray[ix].TopSizeArray          = TopSizeArray;
    CodeHeapStatArray[ix].alloc_topSizeBlocks   = alloc_topSizeBlocks;
    CodeHeapStatArray[ix].used_topSizeBlocks    = used_topSizeBlocks;
    CodeHeapStatArray[ix].SizeDistributionArray = SizeDistributionArray;
    CodeHeapStatArray[ix].avgTemp               = avgTemp;
    CodeHeapStatArray[ix].maxTemp               = maxTemp;
    CodeHeapStatArray[ix].minTemp               = minTemp;
  }
}

//---<  get a new statistics array  >---
void CodeHeapState::prepare_StatArray(outputStream* out, size_t nElem, size_t granularity, const char* heapName) {
  if (StatArray == NULL) {
    StatArray      = new StatElement[nElem];
    //---<  reset some counts  >---
    alloc_granules = nElem;
    granule_size   = granularity;
  }

  if (StatArray == NULL) {
    //---<  just do nothing if allocation failed  >---
    out->print_cr("Statistics could not be collected for %s, probably out of memory.", heapName);
    out->print_cr("Current granularity is " SIZE_FORMAT " bytes. Try a coarser granularity.", granularity);
    alloc_granules = 0;
    granule_size   = 0;
  } else {
    //---<  initialize statistics array  >---
    memset((void*)StatArray, 0, nElem*sizeof(StatElement));
  }
}

//---<  get a new free block array  >---
void CodeHeapState::prepare_FreeArray(outputStream* out, unsigned int nElem, const char* heapName) {
  if (FreeArray == NULL) {
    FreeArray      = new FreeBlk[nElem];
    //---<  reset some counts  >---
    alloc_freeBlocks = nElem;
  }

  if (FreeArray == NULL) {
    //---<  just do nothing if allocation failed  >---
    out->print_cr("Free space analysis cannot be done for %s, probably out of memory.", heapName);
    alloc_freeBlocks = 0;
  } else {
    //---<  initialize free block array  >---
    memset((void*)FreeArray, 0, alloc_freeBlocks*sizeof(FreeBlk));
  }
}

//---<  get a new TopSizeArray  >---
void CodeHeapState::prepare_TopSizeArray(outputStream* out, unsigned int nElem, const char* heapName) {
  if (TopSizeArray == NULL) {
    TopSizeArray   = new TopSizeBlk[nElem];
    //---<  reset some counts  >---
    alloc_topSizeBlocks = nElem;
    used_topSizeBlocks  = 0;
  }

  if (TopSizeArray == NULL) {
    //---<  just do nothing if allocation failed  >---
    out->print_cr("Top-%d list of largest CodeHeap blocks can not be collected for %s, probably out of memory.", nElem, heapName);
    alloc_topSizeBlocks = 0;
  } else {
    //---<  initialize TopSizeArray  >---
    memset((void*)TopSizeArray, 0, nElem*sizeof(TopSizeBlk));
    used_topSizeBlocks  = 0;
  }
}

//---<  get a new SizeDistributionArray  >---
void CodeHeapState::prepare_SizeDistArray(outputStream* out, unsigned int nElem, const char* heapName) {
  if (SizeDistributionArray == NULL) {
    SizeDistributionArray = new SizeDistributionElement[nElem];
  }

  if (SizeDistributionArray == NULL) {
    //---<  just do nothing if allocation failed  >---
    out->print_cr("Size distribution can not be collected for %s, probably out of memory.", heapName);
  } else {
    //---<  initialize SizeDistArray  >---
    memset((void*)SizeDistributionArray, 0, nElem*sizeof(SizeDistributionElement));
    // Logarithmic range growth. First range starts at _segment_size.
    SizeDistributionArray[log2_seg_size-1].rangeEnd = 1U;
    for (unsigned int i = log2_seg_size; i < nElem; i++) {
      SizeDistributionArray[i].rangeStart = 1U << (i     - log2_seg_size);
      SizeDistributionArray[i].rangeEnd   = 1U << ((i+1) - log2_seg_size);
    }
  }
}

//---<  get a new SizeDistributionArray  >---
void CodeHeapState::update_SizeDistArray(outputStream* out, unsigned int len) {
  if (SizeDistributionArray != NULL) {
    for (unsigned int i = log2_seg_size-1; i < nSizeDistElements; i++) {
      if ((SizeDistributionArray[i].rangeStart <= len) && (len < SizeDistributionArray[i].rangeEnd)) {
        SizeDistributionArray[i].lenSum += len;
        SizeDistributionArray[i].count++;
        break;
      }
    }
  }
}

void CodeHeapState::discard_StatArray(outputStream* out) {
  if (StatArray != NULL) {
    delete StatArray;
    StatArray        = NULL;
    alloc_granules   = 0;
    granule_size     = 0;
  }
}

void CodeHeapState::discard_FreeArray(outputStream* out) {
  if (FreeArray != NULL) {
    delete[] FreeArray;
    FreeArray        = NULL;
    alloc_freeBlocks = 0;
  }
}

void CodeHeapState::discard_TopSizeArray(outputStream* out) {
  if (TopSizeArray != NULL) {
    for (unsigned int i = 0; i < alloc_topSizeBlocks; i++) {
      if (TopSizeArray[i].blob_name != NULL) {
        os::free((void*)TopSizeArray[i].blob_name);
      }
    }
    delete[] TopSizeArray;
    TopSizeArray        = NULL;
    alloc_topSizeBlocks = 0;
    used_topSizeBlocks  = 0;
  }
}

void CodeHeapState::discard_SizeDistArray(outputStream* out) {
  if (SizeDistributionArray != NULL) {
    delete[] SizeDistributionArray;
    SizeDistributionArray = NULL;
  }
}

// Discard all allocated internal data structures.
// This should be done after an analysis session is completed.
void CodeHeapState::discard(outputStream* out, CodeHeap* heap) {
  if (!initialization_complete) {
    return;
  }

  if (nHeaps > 0) {
    for (unsigned int ix = 0; ix < nHeaps; ix++) {
      get_HeapStatGlobals(out, CodeHeapStatArray[ix].heapName);
      discard_StatArray(out);
      discard_FreeArray(out);
      discard_TopSizeArray(out);
      discard_SizeDistArray(out);
      set_HeapStatGlobals(out, CodeHeapStatArray[ix].heapName);
      CodeHeapStatArray[ix].heapName = NULL;
    }
    nHeaps = 0;
  }
}

void CodeHeapState::aggregate(outputStream* out, CodeHeap* heap, size_t granularity) {
  unsigned int nBlocks_free    = 0;
  unsigned int nBlocks_used    = 0;
  unsigned int nBlocks_zomb    = 0;
  unsigned int nBlocks_disconn = 0;
  unsigned int nBlocks_notentr = 0;

  //---<  max & min of TopSizeArray  >---
  //  it is sufficient to have these sizes as 32bit unsigned ints.
  //  The CodeHeap is limited in size to 4GB. Furthermore, the sizes
  //  are stored in _segment_size units, scaling them down by a factor of 64 (at least).
  unsigned int  currMax          = 0;
  unsigned int  currMin          = 0;
  unsigned int  currMin_ix       = 0;
  unsigned long total_iterations = 0;

  bool  done             = false;
  const int min_granules = 256;
  const int max_granules = 512*K; // limits analyzable CodeHeap (with segment_granules) to 32M..128M
                                  // results in StatArray size of 24M (= max_granules * 48 Bytes per element)
                                  // For a 1GB CodeHeap, the granule size must be at least 2kB to not violate the max_granles limit.
  const char* heapName   = get_heapName(heap);
  BUFFEREDSTREAM_DECL(ast, out)

  if (!initialization_complete) {
    memset(CodeHeapStatArray, 0, sizeof(CodeHeapStatArray));
    initialization_complete = true;

    printBox(ast, '=', "C O D E   H E A P   A N A L Y S I S   (general remarks)", NULL);
    ast->print_cr("   The code heap analysis function provides deep insights into\n"
                  "   the inner workings and the internal state of the Java VM's\n"
                  "   code cache - the place where all the JVM generated machine\n"
                  "   code is stored.\n"
                  "   \n"
                  "   This function is designed and provided for support engineers\n"
                  "   to help them understand and solve issues in customer systems.\n"
                  "   It is not intended for use and interpretation by other persons.\n"
                  "   \n");
    BUFFEREDSTREAM_FLUSH("")
  }
  get_HeapStatGlobals(out, heapName);


  // Since we are (and must be) analyzing the CodeHeap contents under the CodeCache_lock,
  // all heap information is "constant" and can be safely extracted/calculated before we
  // enter the while() loop. Actually, the loop will only be iterated once.
  char*  low_bound     = heap->low_boundary();
  size_t size          = heap->capacity();
  size_t res_size      = heap->max_capacity();
  seg_size             = heap->segment_size();
  log2_seg_size        = seg_size == 0 ? 0 : exact_log2(seg_size);  // This is a global static value.

  if (seg_size == 0) {
    printBox(ast, '-', "Heap not fully initialized yet, segment size is zero for segment ", heapName);
    BUFFEREDSTREAM_FLUSH("")
    return;
  }

  if (!holding_required_locks()) {
    printBox(ast, '-', "Must be at safepoint or hold Compile_lock and CodeCache_lock when calling aggregate function for ", heapName);
    BUFFEREDSTREAM_FLUSH("")
    return;
  }

  // Calculate granularity of analysis (and output).
  //   The CodeHeap is managed (allocated) in segments (units) of CodeCacheSegmentSize.
  //   The CodeHeap can become fairly large, in particular in productive real-life systems.
  //
  //   It is often neither feasible nor desirable to aggregate the data with the highest possible
  //   level of detail, i.e. inspecting and printing each segment on its own.
  //
  //   The granularity parameter allows to specify the level of detail available in the analysis.
  //   It must be a positive multiple of the segment size and should be selected such that enough
  //   detail is provided while, at the same time, the printed output does not explode.
  //
  //   By manipulating the granularity value, we enforce that at least min_granules units
  //   of analysis are available. We also enforce an upper limit of max_granules units to
  //   keep the amount of allocated storage in check.
  //
  //   Finally, we adjust the granularity such that each granule covers at most 64k-1 segments.
  //   This is necessary to prevent an unsigned short overflow while accumulating space information.
  //
  assert(granularity > 0, "granularity should be positive.");

  if (granularity > size) {
    granularity = size;
  }
  if (size/granularity < min_granules) {
    granularity = size/min_granules;                                   // at least min_granules granules
  }
  granularity = granularity & (~(seg_size - 1));                       // must be multiple of seg_size
  if (granularity < seg_size) {
    granularity = seg_size;                                            // must be at least seg_size
  }
  if (size/granularity > max_granules) {
    granularity = size/max_granules;                                   // at most max_granules granules
  }
  granularity = granularity & (~(seg_size - 1));                       // must be multiple of seg_size
  if (granularity>>log2_seg_size >= (1L<<sizeof(unsigned short)*8)) {
    granularity = ((1L<<(sizeof(unsigned short)*8))-1)<<log2_seg_size; // Limit: (64k-1) * seg_size
  }
  segment_granules = granularity == seg_size;
  size_t granules  = (size + (granularity-1))/granularity;

  printBox(ast, '=', "C O D E   H E A P   A N A L Y S I S   (used blocks) for segment ", heapName);
  ast->print_cr("   The aggregate step takes an aggregated snapshot of the CodeHeap.\n"
                "   Subsequent print functions create their output based on this snapshot.\n"
                "   The CodeHeap is a living thing, and every effort has been made for the\n"
                "   collected data to be consistent. Only the method names and signatures\n"
                "   are retrieved at print time. That may lead to rare cases where the\n"
                "   name of a method is no longer available, e.g. because it was unloaded.\n");
  ast->print_cr("   CodeHeap committed size " SIZE_FORMAT "K (" SIZE_FORMAT "M), reserved size " SIZE_FORMAT "K (" SIZE_FORMAT "M), %d%% occupied.",
                size/(size_t)K, size/(size_t)M, res_size/(size_t)K, res_size/(size_t)M, (unsigned int)(100.0*size/res_size));
  ast->print_cr("   CodeHeap allocation segment size is " SIZE_FORMAT " bytes. This is the smallest possible granularity.", seg_size);
  ast->print_cr("   CodeHeap (committed part) is mapped to " SIZE_FORMAT " granules of size " SIZE_FORMAT " bytes.", granules, granularity);
  ast->print_cr("   Each granule takes " SIZE_FORMAT " bytes of C heap, that is " SIZE_FORMAT "K in total for statistics data.", sizeof(StatElement), (sizeof(StatElement)*granules)/(size_t)K);
  ast->print_cr("   The number of granules is limited to %dk, requiring a granules size of at least %d bytes for a 1GB heap.", (unsigned int)(max_granules/K), (unsigned int)(G/max_granules));
  BUFFEREDSTREAM_FLUSH("\n")


  while (!done) {
    //---<  reset counters with every aggregation  >---
    nBlocks_t1       = 0;
    nBlocks_t2       = 0;
    nBlocks_alive    = 0;
    nBlocks_dead     = 0;
    nBlocks_unloaded = 0;
    nBlocks_stub     = 0;

    nBlocks_free     = 0;
    nBlocks_used     = 0;
    nBlocks_zomb     = 0;
    nBlocks_disconn  = 0;
    nBlocks_notentr  = 0;

    //---<  discard old arrays if size does not match  >---
    if (granules != alloc_granules) {
      discard_StatArray(out);
      discard_TopSizeArray(out);
    }

    //---<  allocate arrays if they don't yet exist, initialize  >---
    prepare_StatArray(out, granules, granularity, heapName);
    if (StatArray == NULL) {
      set_HeapStatGlobals(out, heapName);
      return;
    }
    prepare_TopSizeArray(out, maxTopSizeBlocks, heapName);
    prepare_SizeDistArray(out, nSizeDistElements, heapName);

    latest_compilation_id = CompileBroker::get_compilation_id();
    unsigned int highest_compilation_id = 0;
    size_t       usedSpace     = 0;
    size_t       t1Space       = 0;
    size_t       t2Space       = 0;
    size_t       aliveSpace    = 0;
    size_t       disconnSpace  = 0;
    size_t       notentrSpace  = 0;
    size_t       deadSpace     = 0;
    size_t       unloadedSpace = 0;
    size_t       stubSpace     = 0;
    size_t       freeSpace     = 0;
    size_t       maxFreeSize   = 0;
    HeapBlock*   maxFreeBlock  = NULL;
    bool         insane        = false;

    int64_t hotnessAccumulator = 0;
    unsigned int n_methods     = 0;
    avgTemp       = 0;
    minTemp       = (int)(res_size > M ? (res_size/M)*2 : 1);
    maxTemp       = -minTemp;

    for (HeapBlock *h = heap->first_block(); h != NULL && !insane; h = heap->next_block(h)) {
      unsigned int hb_len     = (unsigned int)h->length();  // despite being size_t, length can never overflow an unsigned int.
      size_t       hb_bytelen = ((size_t)hb_len)<<log2_seg_size;
      unsigned int ix_beg     = (unsigned int)(((char*)h-low_bound)/granule_size);
      unsigned int ix_end     = (unsigned int)(((char*)h-low_bound+(hb_bytelen-1))/granule_size);
      unsigned int compile_id = 0;
      CompLevel    comp_lvl   = CompLevel_none;
      compType     cType      = noComp;
      blobType     cbType     = noType;

      //---<  some sanity checks  >---
      // Do not assert here, just check, print error message and return.
      // This is a diagnostic function. It is not supposed to tear down the VM.
      if ((char*)h <  low_bound) {
        insane = true; ast->print_cr("Sanity check: HeapBlock @%p below low bound (%p)", (char*)h, low_bound);
      }
      if ((char*)h >  (low_bound + res_size)) {
        insane = true; ast->print_cr("Sanity check: HeapBlock @%p outside reserved range (%p)", (char*)h, low_bound + res_size);
      }
      if ((char*)h >  (low_bound + size)) {
        insane = true; ast->print_cr("Sanity check: HeapBlock @%p outside used range (%p)", (char*)h, low_bound + size);
      }
      if (ix_end   >= granules) {
        insane = true; ast->print_cr("Sanity check: end index (%d) out of bounds (" SIZE_FORMAT ")", ix_end, granules);
      }
      if (size     != heap->capacity()) {
        insane = true; ast->print_cr("Sanity check: code heap capacity has changed (" SIZE_FORMAT "K to " SIZE_FORMAT "K)", size/(size_t)K, heap->capacity()/(size_t)K);
      }
      if (ix_beg   >  ix_end) {
        insane = true; ast->print_cr("Sanity check: end index (%d) lower than begin index (%d)", ix_end, ix_beg);
      }
      if (insane) {
        BUFFEREDSTREAM_FLUSH("")
        continue;
      }

      if (h->free()) {
        nBlocks_free++;
        freeSpace    += hb_bytelen;
        if (hb_bytelen > maxFreeSize) {
          maxFreeSize   = hb_bytelen;
          maxFreeBlock  = h;
        }
      } else {
        update_SizeDistArray(out, hb_len);
        nBlocks_used++;
        usedSpace    += hb_bytelen;
        CodeBlob* cb  = (CodeBlob*)heap->find_start(h);
        cbType = get_cbType(cb);  // Will check for cb == NULL and other safety things.
        if (cbType != noType) {
          const char* blob_name  = os::strdup(cb->name());
          unsigned int nm_size   = 0;
          int temperature        = 0;
          nmethod*  nm = cb->as_nmethod_or_null();
          if (nm != NULL) { // no is_readable check required, nm = (nmethod*)cb.
            ResourceMark rm;
            Method* method = nm->method();
            if (nm->is_in_use()) {
              blob_name = os::strdup(method->name_and_sig_as_C_string());
            }
            if (nm->is_not_entrant()) {
              blob_name = os::strdup(method->name_and_sig_as_C_string());
            }

            nm_size    = nm->total_size();
            compile_id = nm->compile_id();
            comp_lvl   = (CompLevel)(nm->comp_level());
            if (nm->is_compiled_by_c1()) {
              cType = c1;
            }
            if (nm->is_compiled_by_c2()) {
              cType = c2;
            }
            if (nm->is_compiled_by_jvmci()) {
              cType = jvmci;
            }
            switch (cbType) {
              case nMethod_inuse: { // only for executable methods!!!
                // space for these cbs is accounted for later.
                temperature = nm->hotness_counter();
                hotnessAccumulator += temperature;
                n_methods++;
                maxTemp = (temperature > maxTemp) ? temperature : maxTemp;
                minTemp = (temperature < minTemp) ? temperature : minTemp;
                break;
              }
              case nMethod_notused:
                nBlocks_alive++;
                nBlocks_disconn++;
                aliveSpace     += hb_bytelen;
                disconnSpace   += hb_bytelen;
                break;
              case nMethod_notentrant:  // equivalent to nMethod_alive
                nBlocks_alive++;
                nBlocks_notentr++;
                aliveSpace     += hb_bytelen;
                notentrSpace   += hb_bytelen;
                break;
              case nMethod_unloaded:
                nBlocks_unloaded++;
                unloadedSpace  += hb_bytelen;
                break;
              case nMethod_dead:
                nBlocks_dead++;
                deadSpace      += hb_bytelen;
                break;
              default:
                break;
            }
          }

          //------------------------------------------
          //---<  register block in TopSizeArray  >---
          //------------------------------------------
          if (alloc_topSizeBlocks > 0) {
            if (used_topSizeBlocks == 0) {
              TopSizeArray[0].start       = h;
              TopSizeArray[0].blob_name   = blob_name;
              TopSizeArray[0].len         = hb_len;
              TopSizeArray[0].index       = tsbStopper;
              TopSizeArray[0].nm_size     = nm_size;
              TopSizeArray[0].temperature = temperature;
              TopSizeArray[0].compiler    = cType;
              TopSizeArray[0].level       = comp_lvl;
              TopSizeArray[0].type        = cbType;
              currMax    = hb_len;
              currMin    = hb_len;
              currMin_ix = 0;
              used_topSizeBlocks++;
              blob_name  = NULL; // indicate blob_name was consumed
            // This check roughly cuts 5000 iterations (JVM98, mixed, dbg, termination stats):
            } else if ((used_topSizeBlocks < alloc_topSizeBlocks) && (hb_len < currMin)) {
              //---<  all blocks in list are larger, but there is room left in array  >---
              TopSizeArray[currMin_ix].index = used_topSizeBlocks;
              TopSizeArray[used_topSizeBlocks].start       = h;
              TopSizeArray[used_topSizeBlocks].blob_name   = blob_name;
              TopSizeArray[used_topSizeBlocks].len         = hb_len;
              TopSizeArray[used_topSizeBlocks].index       = tsbStopper;
              TopSizeArray[used_topSizeBlocks].nm_size     = nm_size;
              TopSizeArray[used_topSizeBlocks].temperature = temperature;
              TopSizeArray[used_topSizeBlocks].compiler    = cType;
              TopSizeArray[used_topSizeBlocks].level       = comp_lvl;
              TopSizeArray[used_topSizeBlocks].type        = cbType;
              currMin    = hb_len;
              currMin_ix = used_topSizeBlocks;
              used_topSizeBlocks++;
              blob_name  = NULL; // indicate blob_name was consumed
            } else {
              // This check cuts total_iterations by a factor of 6 (JVM98, mixed, dbg, termination stats):
              //   We don't need to search the list if we know beforehand that the current block size is
              //   smaller than the currently recorded minimum and there is no free entry left in the list.
              if (!((used_topSizeBlocks == alloc_topSizeBlocks) && (hb_len <= currMin))) {
                if (currMax < hb_len) {
                  currMax = hb_len;
                }
                unsigned int i;
                unsigned int prev_i  = tsbStopper;
                unsigned int limit_i =  0;
                for (i = 0; i != tsbStopper; i = TopSizeArray[i].index) {
                  if (limit_i++ >= alloc_topSizeBlocks) {
                    insane = true; break; // emergency exit
                  }
                  if (i >= used_topSizeBlocks)  {
                    insane = true; break; // emergency exit
                  }
                  total_iterations++;
                  if (TopSizeArray[i].len < hb_len) {
                    //---<  We want to insert here, element <i> is smaller than the current one  >---
                    if (used_topSizeBlocks < alloc_topSizeBlocks) { // still room for a new entry to insert
                      // old entry gets moved to the next free element of the array.
                      // That's necessary to keep the entry for the largest block at index 0.
                      // This move might cause the current minimum to be moved to another place
                      if (i == currMin_ix) {
                        assert(TopSizeArray[i].len == currMin, "sort error");
                        currMin_ix = used_topSizeBlocks;
                      }
                      memcpy((void*)&TopSizeArray[used_topSizeBlocks], (void*)&TopSizeArray[i], sizeof(TopSizeBlk));
                      TopSizeArray[i].start       = h;
                      TopSizeArray[i].blob_name   = blob_name;
                      TopSizeArray[i].len         = hb_len;
                      TopSizeArray[i].index       = used_topSizeBlocks;
                      TopSizeArray[i].nm_size     = nm_size;
                      TopSizeArray[i].temperature = temperature;
                      TopSizeArray[i].compiler    = cType;
                      TopSizeArray[i].level       = comp_lvl;
                      TopSizeArray[i].type        = cbType;
                      used_topSizeBlocks++;
                      blob_name  = NULL; // indicate blob_name was consumed
                    } else { // no room for new entries, current block replaces entry for smallest block
                      //---<  Find last entry (entry for smallest remembered block)  >---
                      // We either want to insert right before the smallest entry, which is when <i>
                      // indexes the smallest entry. We then just overwrite the smallest entry.
                      // What's more likely:
                      // We want to insert somewhere in the list. The smallest entry (@<j>) then falls off the cliff.
                      // The element at the insert point <i> takes it's slot. The second-smallest entry now becomes smallest.
                      // Data of the current block is filled in at index <i>.
                      unsigned int      j  = i;
                      unsigned int prev_j  = tsbStopper;
                      unsigned int limit_j = 0;
                      while (TopSizeArray[j].index != tsbStopper) {
                        if (limit_j++ >= alloc_topSizeBlocks) {
                          insane = true; break; // emergency exit
                        }
                        if (j >= used_topSizeBlocks)  {
                          insane = true; break; // emergency exit
                        }
                        total_iterations++;
                        prev_j = j;
                        j      = TopSizeArray[j].index;
                      }
                      if (!insane) {
                        if (TopSizeArray[j].blob_name != NULL) {
                          os::free((void*)TopSizeArray[j].blob_name);
                        }
                        if (prev_j == tsbStopper) {
                          //---<  Above while loop did not iterate, we already are the min entry  >---
                          //---<  We have to just replace the smallest entry                      >---
                          currMin    = hb_len;
                          currMin_ix = j;
                          TopSizeArray[j].start       = h;
                          TopSizeArray[j].blob_name   = blob_name;
                          TopSizeArray[j].len         = hb_len;
                          TopSizeArray[j].index       = tsbStopper; // already set!!
                          TopSizeArray[i].nm_size     = nm_size;
                          TopSizeArray[i].temperature = temperature;
                          TopSizeArray[j].compiler    = cType;
                          TopSizeArray[j].level       = comp_lvl;
                          TopSizeArray[j].type        = cbType;
                        } else {
                          //---<  second-smallest entry is now smallest  >---
                          TopSizeArray[prev_j].index = tsbStopper;
                          currMin    = TopSizeArray[prev_j].len;
                          currMin_ix = prev_j;
                          //---<  previously smallest entry gets overwritten  >---
                          memcpy((void*)&TopSizeArray[j], (void*)&TopSizeArray[i], sizeof(TopSizeBlk));
                          TopSizeArray[i].start       = h;
                          TopSizeArray[i].blob_name   = blob_name;
                          TopSizeArray[i].len         = hb_len;
                          TopSizeArray[i].index       = j;
                          TopSizeArray[i].nm_size     = nm_size;
                          TopSizeArray[i].temperature = temperature;
                          TopSizeArray[i].compiler    = cType;
                          TopSizeArray[i].level       = comp_lvl;
                          TopSizeArray[i].type        = cbType;
                        }
                        blob_name  = NULL; // indicate blob_name was consumed
                      } // insane
                    }
                    break;
                  }
                  prev_i = i;
                }
                if (insane) {
                  // Note: regular analysis could probably continue by resetting "insane" flag.
                  out->print_cr("Possible loop in TopSizeBlocks list detected. Analysis aborted.");
                  discard_TopSizeArray(out);
                }
              }
            }
          }
          if (blob_name != NULL) {
            os::free((void*)blob_name);
            blob_name = NULL;
          }
          //----------------------------------------------
          //---<  END register block in TopSizeArray  >---
          //----------------------------------------------
        } else {
          nBlocks_zomb++;
        }

        if (ix_beg == ix_end) {
          StatArray[ix_beg].type = cbType;
          switch (cbType) {
            case nMethod_inuse:
              highest_compilation_id = (highest_compilation_id >= compile_id) ? highest_compilation_id : compile_id;
              if (comp_lvl < CompLevel_full_optimization) {
                nBlocks_t1++;
                t1Space   += hb_bytelen;
                StatArray[ix_beg].t1_count++;
                StatArray[ix_beg].t1_space += (unsigned short)hb_len;
                StatArray[ix_beg].t1_age    = StatArray[ix_beg].t1_age < compile_id ? compile_id : StatArray[ix_beg].t1_age;
              } else {
                nBlocks_t2++;
                t2Space   += hb_bytelen;
                StatArray[ix_beg].t2_count++;
                StatArray[ix_beg].t2_space += (unsigned short)hb_len;
                StatArray[ix_beg].t2_age    = StatArray[ix_beg].t2_age < compile_id ? compile_id : StatArray[ix_beg].t2_age;
              }
              StatArray[ix_beg].level     = comp_lvl;
              StatArray[ix_beg].compiler  = cType;
              break;
            case nMethod_alive:
              StatArray[ix_beg].tx_count++;
              StatArray[ix_beg].tx_space += (unsigned short)hb_len;
              StatArray[ix_beg].tx_age    = StatArray[ix_beg].tx_age < compile_id ? compile_id : StatArray[ix_beg].tx_age;
              StatArray[ix_beg].level     = comp_lvl;
              StatArray[ix_beg].compiler  = cType;
              break;
            case nMethod_dead:
            case nMethod_unloaded:
              StatArray[ix_beg].dead_count++;
              StatArray[ix_beg].dead_space += (unsigned short)hb_len;
              break;
            default:
              // must be a stub, if it's not a dead or alive nMethod
              nBlocks_stub++;
              stubSpace   += hb_bytelen;
              StatArray[ix_beg].stub_count++;
              StatArray[ix_beg].stub_space += (unsigned short)hb_len;
              break;
          }
        } else {
          unsigned int beg_space = (unsigned int)(granule_size - ((char*)h - low_bound - ix_beg*granule_size));
          unsigned int end_space = (unsigned int)(hb_bytelen - beg_space - (ix_end-ix_beg-1)*granule_size);
          beg_space = beg_space>>log2_seg_size;  // store in units of _segment_size
          end_space = end_space>>log2_seg_size;  // store in units of _segment_size
          StatArray[ix_beg].type = cbType;
          StatArray[ix_end].type = cbType;
          switch (cbType) {
            case nMethod_inuse:
              highest_compilation_id = (highest_compilation_id >= compile_id) ? highest_compilation_id : compile_id;
              if (comp_lvl < CompLevel_full_optimization) {
                nBlocks_t1++;
                t1Space   += hb_bytelen;
                StatArray[ix_beg].t1_count++;
                StatArray[ix_beg].t1_space += (unsigned short)beg_space;
                StatArray[ix_beg].t1_age    = StatArray[ix_beg].t1_age < compile_id ? compile_id : StatArray[ix_beg].t1_age;

                StatArray[ix_end].t1_count++;
                StatArray[ix_end].t1_space += (unsigned short)end_space;
                StatArray[ix_end].t1_age    = StatArray[ix_end].t1_age < compile_id ? compile_id : StatArray[ix_end].t1_age;
              } else {
                nBlocks_t2++;
                t2Space   += hb_bytelen;
                StatArray[ix_beg].t2_count++;
                StatArray[ix_beg].t2_space += (unsigned short)beg_space;
                StatArray[ix_beg].t2_age    = StatArray[ix_beg].t2_age < compile_id ? compile_id : StatArray[ix_beg].t2_age;

                StatArray[ix_end].t2_count++;
                StatArray[ix_end].t2_space += (unsigned short)end_space;
                StatArray[ix_end].t2_age    = StatArray[ix_end].t2_age < compile_id ? compile_id : StatArray[ix_end].t2_age;
              }
              StatArray[ix_beg].level     = comp_lvl;
              StatArray[ix_beg].compiler  = cType;
              StatArray[ix_end].level     = comp_lvl;
              StatArray[ix_end].compiler  = cType;
              break;
            case nMethod_alive:
              StatArray[ix_beg].tx_count++;
              StatArray[ix_beg].tx_space += (unsigned short)beg_space;
              StatArray[ix_beg].tx_age    = StatArray[ix_beg].tx_age < compile_id ? compile_id : StatArray[ix_beg].tx_age;

              StatArray[ix_end].tx_count++;
              StatArray[ix_end].tx_space += (unsigned short)end_space;
              StatArray[ix_end].tx_age    = StatArray[ix_end].tx_age < compile_id ? compile_id : StatArray[ix_end].tx_age;

              StatArray[ix_beg].level     = comp_lvl;
              StatArray[ix_beg].compiler  = cType;
              StatArray[ix_end].level     = comp_lvl;
              StatArray[ix_end].compiler  = cType;
              break;
            case nMethod_dead:
            case nMethod_unloaded:
              StatArray[ix_beg].dead_count++;
              StatArray[ix_beg].dead_space += (unsigned short)beg_space;
              StatArray[ix_end].dead_count++;
              StatArray[ix_end].dead_space += (unsigned short)end_space;
              break;
            default:
              // must be a stub, if it's not a dead or alive nMethod
              nBlocks_stub++;
              stubSpace   += hb_bytelen;
              StatArray[ix_beg].stub_count++;
              StatArray[ix_beg].stub_space += (unsigned short)beg_space;
              StatArray[ix_end].stub_count++;
              StatArray[ix_end].stub_space += (unsigned short)end_space;
              break;
          }
          for (unsigned int ix = ix_beg+1; ix < ix_end; ix++) {
            StatArray[ix].type = cbType;
            switch (cbType) {
              case nMethod_inuse:
                if (comp_lvl < CompLevel_full_optimization) {
                  StatArray[ix].t1_count++;
                  StatArray[ix].t1_space += (unsigned short)(granule_size>>log2_seg_size);
                  StatArray[ix].t1_age    = StatArray[ix].t1_age < compile_id ? compile_id : StatArray[ix].t1_age;
                } else {
                  StatArray[ix].t2_count++;
                  StatArray[ix].t2_space += (unsigned short)(granule_size>>log2_seg_size);
                  StatArray[ix].t2_age    = StatArray[ix].t2_age < compile_id ? compile_id : StatArray[ix].t2_age;
                }
                StatArray[ix].level     = comp_lvl;
                StatArray[ix].compiler  = cType;
                break;
              case nMethod_alive:
                StatArray[ix].tx_count++;
                StatArray[ix].tx_space += (unsigned short)(granule_size>>log2_seg_size);
                StatArray[ix].tx_age    = StatArray[ix].tx_age < compile_id ? compile_id : StatArray[ix].tx_age;
                StatArray[ix].level     = comp_lvl;
                StatArray[ix].compiler  = cType;
                break;
              case nMethod_dead:
              case nMethod_unloaded:
                StatArray[ix].dead_count++;
                StatArray[ix].dead_space += (unsigned short)(granule_size>>log2_seg_size);
                break;
              default:
                // must be a stub, if it's not a dead or alive nMethod
                StatArray[ix].stub_count++;
                StatArray[ix].stub_space += (unsigned short)(granule_size>>log2_seg_size);
                break;
            }
          }
        }
      }
    }
    done = true;

    if (!insane) {
      // There is a risk for this block (because it contains many print statements) to get
      // interspersed with print data from other threads. We take this risk intentionally.
      // Getting stalled waiting for tty_lock while holding the CodeCache_lock is not desirable.
      printBox(ast, '-', "Global CodeHeap statistics for segment ", heapName);
      ast->print_cr("freeSpace        = " SIZE_FORMAT_W(8) "k, nBlocks_free     = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", freeSpace/(size_t)K,     nBlocks_free,     (100.0*freeSpace)/size,     (100.0*freeSpace)/res_size);
      ast->print_cr("usedSpace        = " SIZE_FORMAT_W(8) "k, nBlocks_used     = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", usedSpace/(size_t)K,     nBlocks_used,     (100.0*usedSpace)/size,     (100.0*usedSpace)/res_size);
      ast->print_cr("  Tier1 Space    = " SIZE_FORMAT_W(8) "k, nBlocks_t1       = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", t1Space/(size_t)K,       nBlocks_t1,       (100.0*t1Space)/size,       (100.0*t1Space)/res_size);
      ast->print_cr("  Tier2 Space    = " SIZE_FORMAT_W(8) "k, nBlocks_t2       = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", t2Space/(size_t)K,       nBlocks_t2,       (100.0*t2Space)/size,       (100.0*t2Space)/res_size);
      ast->print_cr("  Alive Space    = " SIZE_FORMAT_W(8) "k, nBlocks_alive    = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", aliveSpace/(size_t)K,    nBlocks_alive,    (100.0*aliveSpace)/size,    (100.0*aliveSpace)/res_size);
      ast->print_cr("    disconnected = " SIZE_FORMAT_W(8) "k, nBlocks_disconn  = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", disconnSpace/(size_t)K,  nBlocks_disconn,  (100.0*disconnSpace)/size,  (100.0*disconnSpace)/res_size);
      ast->print_cr("    not entrant  = " SIZE_FORMAT_W(8) "k, nBlocks_notentr  = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", notentrSpace/(size_t)K,  nBlocks_notentr,  (100.0*notentrSpace)/size,  (100.0*notentrSpace)/res_size);
      ast->print_cr("  unloadedSpace  = " SIZE_FORMAT_W(8) "k, nBlocks_unloaded = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", unloadedSpace/(size_t)K, nBlocks_unloaded, (100.0*unloadedSpace)/size, (100.0*unloadedSpace)/res_size);
      ast->print_cr("  deadSpace      = " SIZE_FORMAT_W(8) "k, nBlocks_dead     = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", deadSpace/(size_t)K,     nBlocks_dead,     (100.0*deadSpace)/size,     (100.0*deadSpace)/res_size);
      ast->print_cr("  stubSpace      = " SIZE_FORMAT_W(8) "k, nBlocks_stub     = %6d, %10.3f%% of capacity, %10.3f%% of max_capacity", stubSpace/(size_t)K,     nBlocks_stub,     (100.0*stubSpace)/size,     (100.0*stubSpace)/res_size);
      ast->print_cr("ZombieBlocks     = %8d. These are HeapBlocks which could not be identified as CodeBlobs.", nBlocks_zomb);
      ast->cr();
      ast->print_cr("Segment start          = " INTPTR_FORMAT ", used space      = " SIZE_FORMAT_W(8)"k", p2i(low_bound), size/K);
      ast->print_cr("Segment end (used)     = " INTPTR_FORMAT ", remaining space = " SIZE_FORMAT_W(8)"k", p2i(low_bound) + size, (res_size - size)/K);
      ast->print_cr("Segment end (reserved) = " INTPTR_FORMAT ", reserved space  = " SIZE_FORMAT_W(8)"k", p2i(low_bound) + res_size, res_size/K);
      ast->cr();
      ast->print_cr("latest allocated compilation id = %d", latest_compilation_id);
      ast->print_cr("highest observed compilation id = %d", highest_compilation_id);
      ast->print_cr("Building TopSizeList iterations = %ld", total_iterations);
      ast->cr();

      int             reset_val = NMethodSweeper::hotness_counter_reset_val();
      double reverse_free_ratio = (res_size > size) ? (double)res_size/(double)(res_size-size) : (double)res_size;
      printBox(ast, '-', "Method hotness information at time of this analysis", NULL);
      ast->print_cr("Highest possible method temperature:          %12d", reset_val);
      ast->print_cr("Threshold for method to be considered 'cold': %12.3f", -reset_val + reverse_free_ratio * NmethodSweepActivity);
      if (n_methods > 0) {
        avgTemp = hotnessAccumulator/n_methods;
        ast->print_cr("min. hotness = %6d", minTemp);
        ast->print_cr("avg. hotness = %6d", avgTemp);
        ast->print_cr("max. hotness = %6d", maxTemp);
      } else {
        avgTemp = 0;
        ast->print_cr("No hotness data available");
      }
      BUFFEREDSTREAM_FLUSH("\n")

      // This loop is intentionally printing directly to "out".
      // It should not print anything, anyway.
      out->print("Verifying collected data...");
      size_t granule_segs = granule_size>>log2_seg_size;
      for (unsigned int ix = 0; ix < granules; ix++) {
        if (StatArray[ix].t1_count   > granule_segs) {
          out->print_cr("t1_count[%d]   = %d", ix, StatArray[ix].t1_count);
        }
        if (StatArray[ix].t2_count   > granule_segs) {
          out->print_cr("t2_count[%d]   = %d", ix, StatArray[ix].t2_count);
        }
        if (StatArray[ix].tx_count   > granule_segs) {
          out->print_cr("tx_count[%d]   = %d", ix, StatArray[ix].tx_count);
        }
        if (StatArray[ix].stub_count > granule_segs) {
          out->print_cr("stub_count[%d] = %d", ix, StatArray[ix].stub_count);
        }
        if (StatArray[ix].dead_count > granule_segs) {
          out->print_cr("dead_count[%d] = %d", ix, StatArray[ix].dead_count);
        }
        if (StatArray[ix].t1_space   > granule_segs) {
          out->print_cr("t1_space[%d]   = %d", ix, StatArray[ix].t1_space);
        }
        if (StatArray[ix].t2_space   > granule_segs) {
          out->print_cr("t2_space[%d]   = %d", ix, StatArray[ix].t2_space);
        }
        if (StatArray[ix].tx_space   > granule_segs) {
          out->print_cr("tx_space[%d]   = %d", ix, StatArray[ix].tx_space);
        }
        if (StatArray[ix].stub_space > granule_segs) {
          out->print_cr("stub_space[%d] = %d", ix, StatArray[ix].stub_space);
        }
        if (StatArray[ix].dead_space > granule_segs) {
          out->print_cr("dead_space[%d] = %d", ix, StatArray[ix].dead_space);
        }
        //   this cast is awful! I need it because NT/Intel reports a signed/unsigned mismatch.
        if ((size_t)(StatArray[ix].t1_count+StatArray[ix].t2_count+StatArray[ix].tx_count+StatArray[ix].stub_count+StatArray[ix].dead_count) > granule_segs) {
          out->print_cr("t1_count[%d] = %d, t2_count[%d] = %d, tx_count[%d] = %d, stub_count[%d] = %d", ix, StatArray[ix].t1_count, ix, StatArray[ix].t2_count, ix, StatArray[ix].tx_count, ix, StatArray[ix].stub_count);
        }
        if ((size_t)(StatArray[ix].t1_space+StatArray[ix].t2_space+StatArray[ix].tx_space+StatArray[ix].stub_space+StatArray[ix].dead_space) > granule_segs) {
          out->print_cr("t1_space[%d] = %d, t2_space[%d] = %d, tx_space[%d] = %d, stub_space[%d] = %d", ix, StatArray[ix].t1_space, ix, StatArray[ix].t2_space, ix, StatArray[ix].tx_space, ix, StatArray[ix].stub_space);
        }
      }

      // This loop is intentionally printing directly to "out".
      // It should not print anything, anyway.
      if (used_topSizeBlocks > 0) {
        unsigned int j = 0;
        if (TopSizeArray[0].len != currMax) {
          out->print_cr("currMax(%d) differs from TopSizeArray[0].len(%d)", currMax, TopSizeArray[0].len);
        }
        for (unsigned int i = 0; (TopSizeArray[i].index != tsbStopper) && (j++ < alloc_topSizeBlocks); i = TopSizeArray[i].index) {
          if (TopSizeArray[i].len < TopSizeArray[TopSizeArray[i].index].len) {
            out->print_cr("sort error at index %d: %d !>= %d", i, TopSizeArray[i].len, TopSizeArray[TopSizeArray[i].index].len);
          }
        }
        if (j >= alloc_topSizeBlocks) {
          out->print_cr("Possible loop in TopSizeArray chaining!\n  allocBlocks = %d, usedBlocks = %d", alloc_topSizeBlocks, used_topSizeBlocks);
          for (unsigned int i = 0; i < alloc_topSizeBlocks; i++) {
            out->print_cr("  TopSizeArray[%d].index = %d, len = %d", i, TopSizeArray[i].index, TopSizeArray[i].len);
          }
        }
      }
      out->print_cr("...done\n\n");
    } else {
      // insane heap state detected. Analysis data incomplete. Just throw it away.
      discard_StatArray(out);
      discard_TopSizeArray(out);
    }
  }


  done        = false;
  while (!done && (nBlocks_free > 0)) {

    printBox(ast, '=', "C O D E   H E A P   A N A L Y S I S   (free blocks) for segment ", heapName);
    ast->print_cr("   The aggregate step collects information about all free blocks in CodeHeap.\n"
                  "   Subsequent print functions create their output based on this snapshot.\n");
    ast->print_cr("   Free space in %s is distributed over %d free blocks.", heapName, nBlocks_free);
    ast->print_cr("   Each free block takes " SIZE_FORMAT " bytes of C heap for statistics data, that is " SIZE_FORMAT "K in total.", sizeof(FreeBlk), (sizeof(FreeBlk)*nBlocks_free)/K);
    BUFFEREDSTREAM_FLUSH("\n")

    //----------------------------------------
    //--  Prepare the FreeArray of FreeBlks --
    //----------------------------------------

    //---< discard old array if size does not match  >---
    if (nBlocks_free != alloc_freeBlocks) {
      discard_FreeArray(out);
    }

    prepare_FreeArray(out, nBlocks_free, heapName);
    if (FreeArray == NULL) {
      done = true;
      continue;
    }

    //----------------------------------------
    //--  Collect all FreeBlks in FreeArray --
    //----------------------------------------

    unsigned int ix = 0;
    FreeBlock* cur  = heap->freelist();

    while (cur != NULL) {
      if (ix < alloc_freeBlocks) { // don't index out of bounds if _freelist has more blocks than anticipated
        FreeArray[ix].start = cur;
        FreeArray[ix].len   = (unsigned int)(cur->length()<<log2_seg_size);
        FreeArray[ix].index = ix;
      }
      cur  = cur->link();
      ix++;
    }
    if (ix != alloc_freeBlocks) {
      ast->print_cr("Free block count mismatch. Expected %d free blocks, but found %d.", alloc_freeBlocks, ix);
      ast->print_cr("I will update the counter and retry data collection");
      BUFFEREDSTREAM_FLUSH("\n")
      nBlocks_free = ix;
      continue;
    }
    done = true;
  }

  if (!done || (nBlocks_free == 0)) {
    if (nBlocks_free == 0) {
      printBox(ast, '-', "no free blocks found in ", heapName);
    } else if (!done) {
      ast->print_cr("Free block count mismatch could not be resolved.");
      ast->print_cr("Try to run \"aggregate\" function to update counters");
    }
    BUFFEREDSTREAM_FLUSH("")

    //---< discard old array and update global values  >---
    discard_FreeArray(out);
    set_HeapStatGlobals(out, heapName);
    return;
  }

  //---<  calculate and fill remaining fields  >---
  if (FreeArray != NULL) {
    // This loop is intentionally printing directly to "out".
    // It should not print anything, anyway.
    for (unsigned int ix = 0; ix < alloc_freeBlocks-1; ix++) {
      size_t lenSum = 0;
      FreeArray[ix].gap = (unsigned int)((address)FreeArray[ix+1].start - ((address)FreeArray[ix].start + FreeArray[ix].len));
      for (HeapBlock *h = heap->next_block(FreeArray[ix].start); (h != NULL) && (h != FreeArray[ix+1].start); h = heap->next_block(h)) {
        CodeBlob *cb  = (CodeBlob*)(heap->find_start(h));
        if ((cb != NULL) && !cb->is_nmethod()) { // checks equivalent to those in get_cbType()
          FreeArray[ix].stubs_in_gap = true;
        }
        FreeArray[ix].n_gapBlocks++;
        lenSum += h->length()<<log2_seg_size;
        if (((address)h < ((address)FreeArray[ix].start+FreeArray[ix].len)) || (h >= FreeArray[ix+1].start)) {
          out->print_cr("unsorted occupied CodeHeap block found @ %p, gap interval [%p, %p)", h, (address)FreeArray[ix].start+FreeArray[ix].len, FreeArray[ix+1].start);
        }
      }
      if (lenSum != FreeArray[ix].gap) {
        out->print_cr("Length mismatch for gap between FreeBlk[%d] and FreeBlk[%d]. Calculated: %d, accumulated: %d.", ix, ix+1, FreeArray[ix].gap, (unsigned int)lenSum);
      }
    }
  }
  set_HeapStatGlobals(out, heapName);

  printBox(ast, '=', "C O D E   H E A P   A N A L Y S I S   C O M P L E T E   for segment ", heapName);
  BUFFEREDSTREAM_FLUSH("\n")
}


void CodeHeapState::print_usedSpace(outputStream* out, CodeHeap* heap) {
  if (!initialization_complete) {
    return;
  }

  const char* heapName   = get_heapName(heap);
  get_HeapStatGlobals(out, heapName);

  if ((StatArray == NULL) || (TopSizeArray == NULL) || (used_topSizeBlocks == 0)) {
    return;
  }
  BUFFEREDSTREAM_DECL(ast, out)

  {
    printBox(ast, '=', "U S E D   S P A C E   S T A T I S T I C S   for ", heapName);
    ast->print_cr("Note: The Top%d list of the largest used blocks associates method names\n"
                  "      and other identifying information with the block size data.\n"
                  "\n"
                  "      Method names are dynamically retrieved from the code cache at print time.\n"
                  "      Due to the living nature of the code cache and because the CodeCache_lock\n"
                  "      is not continuously held, the displayed name might be wrong or no name\n"
                  "      might be found at all. The likelihood for that to happen increases\n"
                  "      over time passed between analysis and print step.\n", used_topSizeBlocks);
    BUFFEREDSTREAM_FLUSH_LOCKED("\n")
  }

  //----------------------------
  //--  Print Top Used Blocks --
  //----------------------------
  {
    char*     low_bound  = heap->low_boundary();

    printBox(ast, '-', "Largest Used Blocks in ", heapName);
    print_blobType_legend(ast);

    ast->fill_to(51);
    ast->print("%4s", "blob");
    ast->fill_to(56);
    ast->print("%9s", "compiler");
    ast->fill_to(66);
    ast->print_cr("%6s", "method");
    ast->print_cr("%18s %13s %17s %4s %9s  %5s %s",      "Addr(module)      ", "offset", "size", "type", " type lvl", " temp", "Name");
    BUFFEREDSTREAM_FLUSH_LOCKED("")

    //---<  print Top Ten Used Blocks  >---
    if (used_topSizeBlocks > 0) {
      unsigned int printed_topSizeBlocks = 0;
      for (unsigned int i = 0; i != tsbStopper; i = TopSizeArray[i].index) {
        printed_topSizeBlocks++;
        if (TopSizeArray[i].blob_name == NULL) {
          TopSizeArray[i].blob_name = os::strdup("unnamed blob or blob name unavailable");
        }
        // heap->find_start() is safe. Only works on _segmap.
        // Returns NULL or void*. Returned CodeBlob may be uninitialized.
        HeapBlock* heapBlock = TopSizeArray[i].start;
        CodeBlob*  this_blob = (CodeBlob*)(heap->find_start(heapBlock));
        if (this_blob != NULL) {
          //---<  access these fields only if we own the CodeCache_lock  >---
          //---<  blob address  >---
          ast->print(INTPTR_FORMAT, p2i(this_blob));
          ast->fill_to(19);
          //---<  blob offset from CodeHeap begin  >---
          ast->print("(+" PTR32_FORMAT ")", (unsigned int)((char*)this_blob-low_bound));
          ast->fill_to(33);
        } else {
          //---<  block address  >---
          ast->print(INTPTR_FORMAT, p2i(TopSizeArray[i].start));
          ast->fill_to(19);
          //---<  block offset from CodeHeap begin  >---
          ast->print("(+" PTR32_FORMAT ")", (unsigned int)((char*)TopSizeArray[i].start-low_bound));
          ast->fill_to(33);
        }

        //---<  print size, name, and signature (for nMethods)  >---
        bool is_nmethod = TopSizeArray[i].nm_size > 0;
        if (is_nmethod) {
          //---<  nMethod size in hex  >---
          ast->print(PTR32_FORMAT, TopSizeArray[i].nm_size);
          ast->print("(" SIZE_FORMAT_W(4) "K)", TopSizeArray[i].nm_size/K);
          ast->fill_to(51);
          ast->print("  %c", blobTypeChar[TopSizeArray[i].type]);
          //---<  compiler information  >---
          ast->fill_to(56);
          ast->print("%5s %3d", compTypeName[TopSizeArray[i].compiler], TopSizeArray[i].level);
          //---<  method temperature  >---
          ast->fill_to(67);
          ast->print("%5d", TopSizeArray[i].temperature);
          //---<  name and signature  >---
          ast->fill_to(67+6);
          if (TopSizeArray[i].type == nMethod_dead) {
            ast->print(" zombie method ");
          }
          ast->print("%s", TopSizeArray[i].blob_name);
        } else {
          //---<  block size in hex  >---
          ast->print(PTR32_FORMAT, (unsigned int)(TopSizeArray[i].len<<log2_seg_size));
          ast->print("(" SIZE_FORMAT_W(4) "K)", (TopSizeArray[i].len<<log2_seg_size)/K);
          //---<  no compiler information  >---
          ast->fill_to(56);
          //---<  name and signature  >---
          ast->fill_to(67+6);
          ast->print("%s", TopSizeArray[i].blob_name);
        }
        ast->cr();
        BUFFEREDSTREAM_FLUSH_AUTO("")
      }
      if (used_topSizeBlocks != printed_topSizeBlocks) {
        ast->print_cr("used blocks: %d, printed blocks: %d", used_topSizeBlocks, printed_topSizeBlocks);
        for (unsigned int i = 0; i < alloc_topSizeBlocks; i++) {
          ast->print_cr("  TopSizeArray[%d].index = %d, len = %d", i, TopSizeArray[i].index, TopSizeArray[i].len);
          BUFFEREDSTREAM_FLUSH_AUTO("")
        }
      }
      BUFFEREDSTREAM_FLUSH("\n\n")
    }
  }

  //-----------------------------
  //--  Print Usage Histogram  --
  //-----------------------------

  if (SizeDistributionArray != NULL) {
    unsigned long total_count = 0;
    unsigned long total_size  = 0;
    const unsigned long pctFactor = 200;

    for (unsigned int i = 0; i < nSizeDistElements; i++) {
      total_count += SizeDistributionArray[i].count;
      total_size  += SizeDistributionArray[i].lenSum;
    }

    if ((total_count > 0) && (total_size > 0)) {
      printBox(ast, '-', "Block count histogram for ", heapName);
      ast->print_cr("Note: The histogram indicates how many blocks (as a percentage\n"
                    "      of all blocks) have a size in the given range.\n"
                    "      %ld characters are printed per percentage point.\n", pctFactor/100);
      ast->print_cr("total size   of all blocks: %7ldM", (total_size<<log2_seg_size)/M);
      ast->print_cr("total number of all blocks: %7ld\n", total_count);
      BUFFEREDSTREAM_FLUSH_LOCKED("")

      ast->print_cr("[Size Range)------avg.-size-+----count-+");
      for (unsigned int i = 0; i < nSizeDistElements; i++) {
        if (SizeDistributionArray[i].rangeStart<<log2_seg_size < K) {
          ast->print("[" SIZE_FORMAT_W(5) " .." SIZE_FORMAT_W(5) " ): "
                    ,(size_t)(SizeDistributionArray[i].rangeStart<<log2_seg_size)
                    ,(size_t)(SizeDistributionArray[i].rangeEnd<<log2_seg_size)
                    );
        } else if (SizeDistributionArray[i].rangeStart<<log2_seg_size < M) {
          ast->print("[" SIZE_FORMAT_W(5) "K.." SIZE_FORMAT_W(5) "K): "
                    ,(SizeDistributionArray[i].rangeStart<<log2_seg_size)/K
                    ,(SizeDistributionArray[i].rangeEnd<<log2_seg_size)/K
                    );
        } else {
          ast->print("[" SIZE_FORMAT_W(5) "M.." SIZE_FORMAT_W(5) "M): "
                    ,(SizeDistributionArray[i].rangeStart<<log2_seg_size)/M
                    ,(SizeDistributionArray[i].rangeEnd<<log2_seg_size)/M
                    );
        }
        ast->print(" %8d | %8d |",
                   SizeDistributionArray[i].count > 0 ? (SizeDistributionArray[i].lenSum<<log2_seg_size)/SizeDistributionArray[i].count : 0,
                   SizeDistributionArray[i].count);

        unsigned int percent = pctFactor*SizeDistributionArray[i].count/total_count;
        for (unsigned int j = 1; j <= percent; j++) {
          ast->print("%c", (j%((pctFactor/100)*10) == 0) ? ('0'+j/(((unsigned int)pctFactor/100)*10)) : '*');
        }
        ast->cr();
        BUFFEREDSTREAM_FLUSH_AUTO("")
      }
      ast->print_cr("----------------------------+----------+");
      BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")

      printBox(ast, '-', "Contribution per size range to total size for ", heapName);
      ast->print_cr("Note: The histogram indicates how much space (as a percentage of all\n"
                    "      occupied space) is used by the blocks in the given size range.\n"
                    "      %ld characters are printed per percentage point.\n", pctFactor/100);
      ast->print_cr("total size   of all blocks: %7ldM", (total_size<<log2_seg_size)/M);
      ast->print_cr("total number of all blocks: %7ld\n", total_count);
      BUFFEREDSTREAM_FLUSH_LOCKED("")

      ast->print_cr("[Size Range)------avg.-size-+----count-+");
      for (unsigned int i = 0; i < nSizeDistElements; i++) {
        if (SizeDistributionArray[i].rangeStart<<log2_seg_size < K) {
          ast->print("[" SIZE_FORMAT_W(5) " .." SIZE_FORMAT_W(5) " ): "
                    ,(size_t)(SizeDistributionArray[i].rangeStart<<log2_seg_size)
                    ,(size_t)(SizeDistributionArray[i].rangeEnd<<log2_seg_size)
                    );
        } else if (SizeDistributionArray[i].rangeStart<<log2_seg_size < M) {
          ast->print("[" SIZE_FORMAT_W(5) "K.." SIZE_FORMAT_W(5) "K): "
                    ,(SizeDistributionArray[i].rangeStart<<log2_seg_size)/K
                    ,(SizeDistributionArray[i].rangeEnd<<log2_seg_size)/K
                    );
        } else {
          ast->print("[" SIZE_FORMAT_W(5) "M.." SIZE_FORMAT_W(5) "M): "
                    ,(SizeDistributionArray[i].rangeStart<<log2_seg_size)/M
                    ,(SizeDistributionArray[i].rangeEnd<<log2_seg_size)/M
                    );
        }
        ast->print(" %8d | %8d |",
                   SizeDistributionArray[i].count > 0 ? (SizeDistributionArray[i].lenSum<<log2_seg_size)/SizeDistributionArray[i].count : 0,
                   SizeDistributionArray[i].count);

        unsigned int percent = pctFactor*(unsigned long)SizeDistributionArray[i].lenSum/total_size;
        for (unsigned int j = 1; j <= percent; j++) {
          ast->print("%c", (j%((pctFactor/100)*10) == 0) ? ('0'+j/(((unsigned int)pctFactor/100)*10)) : '*');
        }
        ast->cr();
        BUFFEREDSTREAM_FLUSH_AUTO("")
      }
      ast->print_cr("----------------------------+----------+");
      BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
    }
  }
}


void CodeHeapState::print_freeSpace(outputStream* out, CodeHeap* heap) {
  if (!initialization_complete) {
    return;
  }

  const char* heapName   = get_heapName(heap);
  get_HeapStatGlobals(out, heapName);

  if ((StatArray == NULL) || (FreeArray == NULL) || (alloc_granules == 0)) {
    return;
  }
  BUFFEREDSTREAM_DECL(ast, out)

  {
    printBox(ast, '=', "F R E E   S P A C E   S T A T I S T I C S   for ", heapName);
    ast->print_cr("Note: in this context, a gap is the occupied space between two free blocks.\n"
                  "      Those gaps are of interest if there is a chance that they become\n"
                  "      unoccupied, e.g. by class unloading. Then, the two adjacent free\n"
                  "      blocks, together with the now unoccupied space, form a new, large\n"
                  "      free block.");
    BUFFEREDSTREAM_FLUSH_LOCKED("\n")
  }

  {
    printBox(ast, '-', "List of all Free Blocks in ", heapName);

    unsigned int ix = 0;
    for (ix = 0; ix < alloc_freeBlocks-1; ix++) {
      ast->print(INTPTR_FORMAT ": Len[%4d] = " HEX32_FORMAT ",", p2i(FreeArray[ix].start), ix, FreeArray[ix].len);
      ast->fill_to(38);
      ast->print("Gap[%4d..%4d]: " HEX32_FORMAT " bytes,", ix, ix+1, FreeArray[ix].gap);
      ast->fill_to(71);
      ast->print("block count: %6d", FreeArray[ix].n_gapBlocks);
      if (FreeArray[ix].stubs_in_gap) {
        ast->print(" !! permanent gap, contains stubs and/or blobs !!");
      }
      ast->cr();
      BUFFEREDSTREAM_FLUSH_AUTO("")
    }
    ast->print_cr(INTPTR_FORMAT ": Len[%4d] = " HEX32_FORMAT, p2i(FreeArray[ix].start), ix, FreeArray[ix].len);
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n")
  }


  //-----------------------------------------
  //--  Find and Print Top Ten Free Blocks --
  //-----------------------------------------

  //---<  find Top Ten Free Blocks  >---
  const unsigned int nTop = 10;
  unsigned int  currMax10 = 0;
  struct FreeBlk* FreeTopTen[nTop];
  memset(FreeTopTen, 0, sizeof(FreeTopTen));

  for (unsigned int ix = 0; ix < alloc_freeBlocks; ix++) {
    if (FreeArray[ix].len > currMax10) {  // larger than the ten largest found so far
      unsigned int currSize = FreeArray[ix].len;

      unsigned int iy;
      for (iy = 0; iy < nTop && FreeTopTen[iy] != NULL; iy++) {
        if (FreeTopTen[iy]->len < currSize) {
          for (unsigned int iz = nTop-1; iz > iy; iz--) { // make room to insert new free block
            FreeTopTen[iz] = FreeTopTen[iz-1];
          }
          FreeTopTen[iy] = &FreeArray[ix];        // insert new free block
          if (FreeTopTen[nTop-1] != NULL) {
            currMax10 = FreeTopTen[nTop-1]->len;
          }
          break; // done with this, check next free block
        }
      }
      if (iy >= nTop) {
        ast->print_cr("Internal logic error. New Max10 = %d detected, but could not be merged. Old Max10 = %d",
                      currSize, currMax10);
        continue;
      }
      if (FreeTopTen[iy] == NULL) {
        FreeTopTen[iy] = &FreeArray[ix];
        if (iy == (nTop-1)) {
          currMax10 = currSize;
        }
      }
    }
  }
  BUFFEREDSTREAM_FLUSH_AUTO("")

  {
    printBox(ast, '-', "Top Ten Free Blocks in ", heapName);

    //---<  print Top Ten Free Blocks  >---
    for (unsigned int iy = 0; (iy < nTop) && (FreeTopTen[iy] != NULL); iy++) {
      ast->print("Pos %3d: Block %4d - size " HEX32_FORMAT ",", iy+1, FreeTopTen[iy]->index, FreeTopTen[iy]->len);
      ast->fill_to(39);
      if (FreeTopTen[iy]->index == (alloc_freeBlocks-1)) {
        ast->print("last free block in list.");
      } else {
        ast->print("Gap (to next) " HEX32_FORMAT ",", FreeTopTen[iy]->gap);
        ast->fill_to(63);
        ast->print("#blocks (in gap) %d", FreeTopTen[iy]->n_gapBlocks);
      }
      ast->cr();
      BUFFEREDSTREAM_FLUSH_AUTO("")
    }
  }
  BUFFEREDSTREAM_FLUSH_LOCKED("\n\n")


  //--------------------------------------------------------
  //--  Find and Print Top Ten Free-Occupied-Free Triples --
  //--------------------------------------------------------

  //---<  find and print Top Ten Triples (Free-Occupied-Free)  >---
  currMax10 = 0;
  struct FreeBlk  *FreeTopTenTriple[nTop];
  memset(FreeTopTenTriple, 0, sizeof(FreeTopTenTriple));

  for (unsigned int ix = 0; ix < alloc_freeBlocks-1; ix++) {
    // If there are stubs in the gap, this gap will never become completely free.
    // The triple will thus never merge to one free block.
    unsigned int lenTriple  = FreeArray[ix].len + (FreeArray[ix].stubs_in_gap ? 0 : FreeArray[ix].gap + FreeArray[ix+1].len);
    FreeArray[ix].len = lenTriple;
    if (lenTriple > currMax10) {  // larger than the ten largest found so far

      unsigned int iy;
      for (iy = 0; (iy < nTop) && (FreeTopTenTriple[iy] != NULL); iy++) {
        if (FreeTopTenTriple[iy]->len < lenTriple) {
          for (unsigned int iz = nTop-1; iz > iy; iz--) {
            FreeTopTenTriple[iz] = FreeTopTenTriple[iz-1];
          }
          FreeTopTenTriple[iy] = &FreeArray[ix];
          if (FreeTopTenTriple[nTop-1] != NULL) {
            currMax10 = FreeTopTenTriple[nTop-1]->len;
          }
          break;
        }
      }
      if (iy == nTop) {
        ast->print_cr("Internal logic error. New Max10 = %d detected, but could not be merged. Old Max10 = %d",
                      lenTriple, currMax10);
        continue;
      }
      if (FreeTopTenTriple[iy] == NULL) {
        FreeTopTenTriple[iy] = &FreeArray[ix];
        if (iy == (nTop-1)) {
          currMax10 = lenTriple;
        }
      }
    }
  }
  BUFFEREDSTREAM_FLUSH_AUTO("")

  {
    printBox(ast, '-', "Top Ten Free-Occupied-Free Triples in ", heapName);
    ast->print_cr("  Use this information to judge how likely it is that a large(r) free block\n"
                  "  might get created by code cache sweeping.\n"
                  "  If all the occupied blocks can be swept, the three free blocks will be\n"
                  "  merged into one (much larger) free block. That would reduce free space\n"
                  "  fragmentation.\n");

    //---<  print Top Ten Free-Occupied-Free Triples  >---
    for (unsigned int iy = 0; (iy < nTop) && (FreeTopTenTriple[iy] != NULL); iy++) {
      ast->print("Pos %3d: Block %4d - size " HEX32_FORMAT ",", iy+1, FreeTopTenTriple[iy]->index, FreeTopTenTriple[iy]->len);
      ast->fill_to(39);
      ast->print("Gap (to next) " HEX32_FORMAT ",", FreeTopTenTriple[iy]->gap);
      ast->fill_to(63);
      ast->print("#blocks (in gap) %d", FreeTopTenTriple[iy]->n_gapBlocks);
      ast->cr();
      BUFFEREDSTREAM_FLUSH_AUTO("")
    }
  }
  BUFFEREDSTREAM_FLUSH_LOCKED("\n\n")
}


void CodeHeapState::print_count(outputStream* out, CodeHeap* heap) {
  if (!initialization_complete) {
    return;
  }

  const char* heapName   = get_heapName(heap);
  get_HeapStatGlobals(out, heapName);

  if ((StatArray == NULL) || (alloc_granules == 0)) {
    return;
  }
  BUFFEREDSTREAM_DECL(ast, out)

  unsigned int granules_per_line = 32;
  char*        low_bound         = heap->low_boundary();

  {
    printBox(ast, '=', "B L O C K   C O U N T S   for ", heapName);
    ast->print_cr("  Each granule contains an individual number of heap blocks. Large blocks\n"
                  "  may span multiple granules and are counted for each granule they touch.\n");
    if (segment_granules) {
      ast->print_cr("  You have selected granule size to be as small as segment size.\n"
                    "  As a result, each granule contains exactly one block (or a part of one block)\n"
                    "  or is displayed as empty (' ') if it's BlobType does not match the selection.\n"
                    "  Occupied granules show their BlobType character, see legend.\n");
      print_blobType_legend(ast);
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("")
  }

  {
    if (segment_granules) {
      printBox(ast, '-', "Total (all types) count for granule size == segment size", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        print_blobType_single(ast, StatArray[ix].type);
      }
    } else {
      printBox(ast, '-', "Total (all tiers) count, 0x1..0xf. '*' indicates >= 16 blocks, ' ' indicates empty", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        unsigned int count = StatArray[ix].t1_count   + StatArray[ix].t2_count   + StatArray[ix].tx_count
                           + StatArray[ix].stub_count + StatArray[ix].dead_count;
        print_count_single(ast, count);
      }
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("|\n\n\n")
  }

  {
    if (nBlocks_t1 > 0) {
      printBox(ast, '-', "Tier1 nMethod count only, 0x1..0xf. '*' indicates >= 16 blocks, ' ' indicates empty", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].t1_count > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_count_single(ast, StatArray[ix].t1_count);
        }
      }
      ast->print("|");
    } else {
      ast->print("No Tier1 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_t2 > 0) {
      printBox(ast, '-', "Tier2 nMethod count only, 0x1..0xf. '*' indicates >= 16 blocks, ' ' indicates empty", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].t2_count > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_count_single(ast, StatArray[ix].t2_count);
        }
      }
      ast->print("|");
    } else {
      ast->print("No Tier2 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_alive > 0) {
      printBox(ast, '-', "not_used/not_entrant/not_installed nMethod count only, 0x1..0xf. '*' indicates >= 16 blocks, ' ' indicates empty", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].tx_count > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_count_single(ast, StatArray[ix].tx_count);
        }
      }
      ast->print("|");
    } else {
      ast->print("No not_used/not_entrant nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_stub > 0) {
      printBox(ast, '-', "Stub & Blob count only, 0x1..0xf. '*' indicates >= 16 blocks, ' ' indicates empty", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].stub_count > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_count_single(ast, StatArray[ix].stub_count);
        }
      }
      ast->print("|");
    } else {
      ast->print("No Stubs and Blobs found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_dead > 0) {
      printBox(ast, '-', "Dead nMethod count only, 0x1..0xf. '*' indicates >= 16 blocks, ' ' indicates empty", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].dead_count > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_count_single(ast, StatArray[ix].dead_count);
        }
      }
      ast->print("|");
    } else {
      ast->print("No dead nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (!segment_granules) { // Prevent totally redundant printouts
      printBox(ast, '-', "Count by tier (combined, no dead blocks): <#t1>:<#t2>:<#s>, 0x0..0xf. '*' indicates >= 16 blocks", NULL);

      granules_per_line = 24;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);

        print_count_single(ast, StatArray[ix].t1_count);
        ast->print(":");
        print_count_single(ast, StatArray[ix].t2_count);
        ast->print(":");
        if (segment_granules && StatArray[ix].stub_count > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_count_single(ast, StatArray[ix].stub_count);
        }
        ast->print(" ");
      }
      BUFFEREDSTREAM_FLUSH_LOCKED("|\n\n\n")
    }
  }
}


void CodeHeapState::print_space(outputStream* out, CodeHeap* heap) {
  if (!initialization_complete) {
    return;
  }

  const char* heapName   = get_heapName(heap);
  get_HeapStatGlobals(out, heapName);

  if ((StatArray == NULL) || (alloc_granules == 0)) {
    return;
  }
  BUFFEREDSTREAM_DECL(ast, out)

  unsigned int granules_per_line = 32;
  char*        low_bound         = heap->low_boundary();

  {
    printBox(ast, '=', "S P A C E   U S A G E  &  F R A G M E N T A T I O N   for ", heapName);
    ast->print_cr("  The heap space covered by one granule is occupied to a various extend.\n"
                  "  The granule occupancy is displayed by one decimal digit per granule.\n");
    if (segment_granules) {
      ast->print_cr("  You have selected granule size to be as small as segment size.\n"
                    "  As a result, each granule contains exactly one block (or a part of one block)\n"
                    "  or is displayed as empty (' ') if it's BlobType does not match the selection.\n"
                    "  Occupied granules show their BlobType character, see legend.\n");
      print_blobType_legend(ast);
    } else {
      ast->print_cr("  These digits represent a fill percentage range (see legend).\n");
      print_space_legend(ast);
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("")
  }

  {
    if (segment_granules) {
      printBox(ast, '-', "Total (all types) space consumption for granule size == segment size", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        print_blobType_single(ast, StatArray[ix].type);
      }
    } else {
      printBox(ast, '-', "Total (all types) space consumption. ' ' indicates empty, '*' indicates full.", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        unsigned int space    = StatArray[ix].t1_space   + StatArray[ix].t2_space  + StatArray[ix].tx_space
                              + StatArray[ix].stub_space + StatArray[ix].dead_space;
        print_space_single(ast, space);
      }
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("|\n\n\n")
  }

  {
    if (nBlocks_t1 > 0) {
      printBox(ast, '-', "Tier1 space consumption. ' ' indicates empty, '*' indicates full", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].t1_space > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_space_single(ast, StatArray[ix].t1_space);
        }
      }
      ast->print("|");
    } else {
      ast->print("No Tier1 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_t2 > 0) {
      printBox(ast, '-', "Tier2 space consumption. ' ' indicates empty, '*' indicates full", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].t2_space > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_space_single(ast, StatArray[ix].t2_space);
        }
      }
      ast->print("|");
    } else {
      ast->print("No Tier2 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_alive > 0) {
      printBox(ast, '-', "not_used/not_entrant/not_installed space consumption. ' ' indicates empty, '*' indicates full", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].tx_space > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_space_single(ast, StatArray[ix].tx_space);
        }
      }
      ast->print("|");
    } else {
      ast->print("No Tier2 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_stub > 0) {
      printBox(ast, '-', "Stub and Blob space consumption. ' ' indicates empty, '*' indicates full", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        if (segment_granules && StatArray[ix].stub_space > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_space_single(ast, StatArray[ix].stub_space);
        }
      }
      ast->print("|");
    } else {
      ast->print("No Stubs and Blobs found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_dead > 0) {
      printBox(ast, '-', "Dead space consumption. ' ' indicates empty, '*' indicates full", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        print_space_single(ast, StatArray[ix].dead_space);
      }
      ast->print("|");
    } else {
      ast->print("No dead nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (!segment_granules) { // Prevent totally redundant printouts
      printBox(ast, '-', "Space consumption by tier (combined): <t1%>:<t2%>:<s%>. ' ' indicates empty, '*' indicates full", NULL);

      granules_per_line = 24;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);

        if (segment_granules && StatArray[ix].t1_space > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_space_single(ast, StatArray[ix].t1_space);
        }
        ast->print(":");
        if (segment_granules && StatArray[ix].t2_space > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_space_single(ast, StatArray[ix].t2_space);
        }
        ast->print(":");
        if (segment_granules && StatArray[ix].stub_space > 0) {
          print_blobType_single(ast, StatArray[ix].type);
        } else {
          print_space_single(ast, StatArray[ix].stub_space);
        }
        ast->print(" ");
      }
      ast->print("|");
      BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
    }
  }
}

void CodeHeapState::print_age(outputStream* out, CodeHeap* heap) {
  if (!initialization_complete) {
    return;
  }

  const char* heapName   = get_heapName(heap);
  get_HeapStatGlobals(out, heapName);

  if ((StatArray == NULL) || (alloc_granules == 0)) {
    return;
  }
  BUFFEREDSTREAM_DECL(ast, out)

  unsigned int granules_per_line = 32;
  char*        low_bound         = heap->low_boundary();

  {
    printBox(ast, '=', "M E T H O D   A G E   by CompileID for ", heapName);
    ast->print_cr("  The age of a compiled method in the CodeHeap is not available as a\n"
                  "  time stamp. Instead, a relative age is deducted from the method's compilation ID.\n"
                  "  Age information is available for tier1 and tier2 methods only. There is no\n"
                  "  age information for stubs and blobs, because they have no compilation ID assigned.\n"
                  "  Information for the youngest method (highest ID) in the granule is printed.\n"
                  "  Refer to the legend to learn how method age is mapped to the displayed digit.");
    print_age_legend(ast);
    BUFFEREDSTREAM_FLUSH_LOCKED("")
  }

  {
    printBox(ast, '-', "Age distribution. '0' indicates youngest 1/256, '8': oldest half, ' ': no age information", NULL);

    granules_per_line = 128;
    for (unsigned int ix = 0; ix < alloc_granules; ix++) {
      print_line_delim(out, ast, low_bound, ix, granules_per_line);
      unsigned int age1      = StatArray[ix].t1_age;
      unsigned int age2      = StatArray[ix].t2_age;
      unsigned int agex      = StatArray[ix].tx_age;
      unsigned int age       = age1 > age2 ? age1 : age2;
      age       = age > agex ? age : agex;
      print_age_single(ast, age);
    }
    ast->print("|");
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_t1 > 0) {
      printBox(ast, '-', "Tier1 age distribution. '0' indicates youngest 1/256, '8': oldest half, ' ': no age information", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        print_age_single(ast, StatArray[ix].t1_age);
      }
      ast->print("|");
    } else {
      ast->print("No Tier1 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_t2 > 0) {
      printBox(ast, '-', "Tier2 age distribution. '0' indicates youngest 1/256, '8': oldest half, ' ': no age information", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        print_age_single(ast, StatArray[ix].t2_age);
      }
      ast->print("|");
    } else {
      ast->print("No Tier2 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (nBlocks_alive > 0) {
      printBox(ast, '-', "not_used/not_entrant/not_installed age distribution. '0' indicates youngest 1/256, '8': oldest half, ' ': no age information", NULL);

      granules_per_line = 128;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        print_age_single(ast, StatArray[ix].tx_age);
      }
      ast->print("|");
    } else {
      ast->print("No Tier2 nMethods found in CodeHeap.");
    }
    BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
  }

  {
    if (!segment_granules) { // Prevent totally redundant printouts
      printBox(ast, '-', "age distribution by tier <a1>:<a2>. '0' indicates youngest 1/256, '8': oldest half, ' ': no age information", NULL);

      granules_per_line = 32;
      for (unsigned int ix = 0; ix < alloc_granules; ix++) {
        print_line_delim(out, ast, low_bound, ix, granules_per_line);
        print_age_single(ast, StatArray[ix].t1_age);
        ast->print(":");
        print_age_single(ast, StatArray[ix].t2_age);
        ast->print(" ");
      }
      ast->print("|");
      BUFFEREDSTREAM_FLUSH_LOCKED("\n\n\n")
    }
  }
}


void CodeHeapState::print_names(outputStream* out, CodeHeap* heap) {
  if (!initialization_complete) {
    return;
  }

  const char* heapName   = get_heapName(heap);
  get_HeapStatGlobals(out, heapName);

  if ((StatArray == NULL) || (alloc_granules == 0)) {
    return;
  }
  BUFFEREDSTREAM_DECL(ast, out)

  unsigned int granules_per_line   = 128;
  char*        low_bound           = heap->low_boundary();
  CodeBlob*    last_blob           = NULL;
  bool         name_in_addr_range  = true;
  bool         have_locks          = holding_required_locks();

  //---<  print at least 128K per block (i.e. between headers)  >---
  if (granules_per_line*granule_size < 128*K) {
    granules_per_line = (unsigned int)((128*K)/granule_size);
  }

  printBox(ast, '=', "M E T H O D   N A M E S   for ", heapName);
  ast->print_cr("  Method names are dynamically retrieved from the code cache at print time.\n"
                "  Due to the living nature of the code heap and because the CodeCache_lock\n"
                "  is not continuously held, the displayed name might be wrong or no name\n"
                "  might be found at all. The likelihood for that to happen increases\n"
                "  over time passed between aggregation and print steps.\n");
  BUFFEREDSTREAM_FLUSH_LOCKED("")

  for (unsigned int ix = 0; ix < alloc_granules; ix++) {
    //---<  print a new blob on a new line  >---
    if (ix%granules_per_line == 0) {
      if (!name_in_addr_range) {
        ast->print_cr("No methods, blobs, or stubs found in this address range");
      }
      name_in_addr_range = false;

      size_t end_ix = (ix+granules_per_line <= alloc_granules) ? ix+granules_per_line : alloc_granules;
      ast->cr();
      ast->print_cr("--------------------------------------------------------------------");
      ast->print_cr("Address range [" INTPTR_FORMAT "," INTPTR_FORMAT "), " SIZE_FORMAT "k", p2i(low_bound+ix*granule_size), p2i(low_bound + end_ix*granule_size), (end_ix - ix)*granule_size/(size_t)K);
      ast->print_cr("--------------------------------------------------------------------");
      BUFFEREDSTREAM_FLUSH_AUTO("")
    }
    // Only check granule if it contains at least one blob.
    unsigned int nBlobs  = StatArray[ix].t1_count   + StatArray[ix].t2_count + StatArray[ix].tx_count +
                           StatArray[ix].stub_count + StatArray[ix].dead_count;
    if (nBlobs > 0 ) {
    for (unsigned int is = 0; is < granule_size; is+=(unsigned int)seg_size) {
      // heap->find_start() is safe. Only works on _segmap.
      // Returns NULL or void*. Returned CodeBlob may be uninitialized.
      char*     this_seg  = low_bound + ix*granule_size + is;
      CodeBlob* this_blob = (CodeBlob*)(heap->find_start(this_seg));
      bool   blob_is_safe = blob_access_is_safe(this_blob);
      // blob could have been flushed, freed, and merged.
      // this_blob < last_blob is an indicator for that.
      if (blob_is_safe && (this_blob > last_blob)) {
        last_blob          = this_blob;

        //---<  get type and name  >---
        blobType       cbType = noType;
        if (segment_granules) {
          cbType = (blobType)StatArray[ix].type;
        } else {
          //---<  access these fields only if we own the CodeCache_lock  >---
          if (have_locks) {
            cbType = get_cbType(this_blob);
          }
        }

        //---<  access these fields only if we own the CodeCache_lock  >---
        const char* blob_name = "<unavailable>";
        nmethod*           nm = NULL;
        if (have_locks) {
          blob_name = this_blob->name();
          nm        = this_blob->as_nmethod_or_null();
          // this_blob->name() could return NULL if no name was given to CTOR. Inlined, maybe invisible on stack
          if (blob_name == NULL) {
            blob_name = "<unavailable>";
          }
        }

        //---<  print table header for new print range  >---
        if (!name_in_addr_range) {
          name_in_addr_range = true;
          ast->fill_to(51);
          ast->print("%9s", "compiler");
          ast->fill_to(61);
          ast->print_cr("%6s", "method");
          ast->print_cr("%18s %13s %17s %9s  %5s %18s  %s", "Addr(module)      ", "offset", "size", " type lvl", " temp", "blobType          ", "Name");
          BUFFEREDSTREAM_FLUSH_AUTO("")
        }

        //---<  print line prefix (address and offset from CodeHeap start)  >---
        ast->print(INTPTR_FORMAT, p2i(this_blob));
        ast->fill_to(19);
        ast->print("(+" PTR32_FORMAT ")", (unsigned int)((char*)this_blob-low_bound));
        ast->fill_to(33);

        // access nmethod and Method fields only if we own the CodeCache_lock.
        // This fact is implicitly transported via nm != NULL.
        if (nmethod_access_is_safe(nm)) {
          Method* method = nm->method();
          ResourceMark rm;
          //---<  collect all data to locals as quickly as possible  >---
          unsigned int total_size = nm->total_size();
          int          hotness    = nm->hotness_counter();
          bool         get_name   = (cbType == nMethod_inuse) || (cbType == nMethod_notused);
          //---<  nMethod size in hex  >---
          ast->print(PTR32_FORMAT, total_size);
          ast->print("(" SIZE_FORMAT_W(4) "K)", total_size/K);
          //---<  compiler information  >---
          ast->fill_to(51);
          ast->print("%5s %3d", compTypeName[StatArray[ix].compiler], StatArray[ix].level);
          //---<  method temperature  >---
          ast->fill_to(62);
          ast->print("%5d", hotness);
          //---<  name and signature  >---
          ast->fill_to(62+6);
          ast->print("%s", blobTypeName[cbType]);
          ast->fill_to(82+6);
          if (cbType == nMethod_dead) {
            ast->print("%14s", " zombie method");
          }

          if (get_name) {
            Symbol* methName  = method->name();
            const char*   methNameS = (methName == NULL) ? NULL : methName->as_C_string();
            methNameS = (methNameS == NULL) ? "<method name unavailable>" : methNameS;
            Symbol* methSig   = method->signature();
            const char*   methSigS  = (methSig  == NULL) ? NULL : methSig->as_C_string();
            methSigS  = (methSigS  == NULL) ? "<method signature unavailable>" : methSigS;
            ast->print("%s", methNameS);
            ast->print("%s", methSigS);
          } else {
            ast->print("%s", blob_name);
          }
        } else if (blob_is_safe) {
          ast->fill_to(62+6);
          ast->print("%s", blobTypeName[cbType]);
          ast->fill_to(82+6);
          ast->print("%s", blob_name);
        } else {
          ast->fill_to(62+6);
          ast->print("<stale blob>");
        }
        ast->cr();
        BUFFEREDSTREAM_FLUSH_AUTO("")
      } else if (!blob_is_safe && (this_blob != last_blob) && (this_blob != NULL)) {
        last_blob          = this_blob;
      }
    }
    } // nBlobs > 0
  }
  BUFFEREDSTREAM_FLUSH_LOCKED("\n\n")
}


void CodeHeapState::printBox(outputStream* ast, const char border, const char* text1, const char* text2) {
  unsigned int lineLen = 1 + 2 + 2 + 1;
  char edge, frame;

  if (text1 != NULL) {
    lineLen += (unsigned int)strlen(text1); // text1 is much shorter than MAX_INT chars.
  }
  if (text2 != NULL) {
    lineLen += (unsigned int)strlen(text2); // text2 is much shorter than MAX_INT chars.
  }
  if (border == '-') {
    edge  = '+';
    frame = '|';
  } else {
    edge  = border;
    frame = border;
  }

  ast->print("%c", edge);
  for (unsigned int i = 0; i < lineLen-2; i++) {
    ast->print("%c", border);
  }
  ast->print_cr("%c", edge);

  ast->print("%c  ", frame);
  if (text1 != NULL) {
    ast->print("%s", text1);
  }
  if (text2 != NULL) {
    ast->print("%s", text2);
  }
  ast->print_cr("  %c", frame);

  ast->print("%c", edge);
  for (unsigned int i = 0; i < lineLen-2; i++) {
    ast->print("%c", border);
  }
  ast->print_cr("%c", edge);
}

void CodeHeapState::print_blobType_legend(outputStream* out) {
  out->cr();
  printBox(out, '-', "Block types used in the following CodeHeap dump", NULL);
  for (int type = noType; type < lastType; type += 1) {
    out->print_cr("  %c - %s", blobTypeChar[type], blobTypeName[type]);
  }
  out->print_cr("  -----------------------------------------------------");
  out->cr();
}

void CodeHeapState::print_space_legend(outputStream* out) {
  unsigned int indicator = 0;
  unsigned int age_range = 256;
  unsigned int range_beg = latest_compilation_id;
  out->cr();
  printBox(out, '-', "Space ranges, based on granule occupancy", NULL);
  out->print_cr("    -   0%% == occupancy");
  for (int i=0; i<=9; i++) {
    out->print_cr("  %d - %3d%% < occupancy < %3d%%", i, 10*i, 10*(i+1));
  }
  out->print_cr("  * - 100%% == occupancy");
  out->print_cr("  ----------------------------------------------");
  out->cr();
}

void CodeHeapState::print_age_legend(outputStream* out) {
  unsigned int indicator = 0;
  unsigned int age_range = 256;
  unsigned int range_beg = latest_compilation_id;
  out->cr();
  printBox(out, '-', "Age ranges, based on compilation id", NULL);
  while (age_range > 0) {
    out->print_cr("  %d - %6d to %6d", indicator, range_beg, latest_compilation_id - latest_compilation_id/age_range);
    range_beg = latest_compilation_id - latest_compilation_id/age_range;
    age_range /= 2;
    indicator += 1;
  }
  out->print_cr("  -----------------------------------------");
  out->cr();
}

void CodeHeapState::print_blobType_single(outputStream* out, u2 /* blobType */ type) {
  out->print("%c", blobTypeChar[type]);
}

void CodeHeapState::print_count_single(outputStream* out, unsigned short count) {
  if (count >= 16)    out->print("*");
  else if (count > 0) out->print("%1.1x", count);
  else                out->print(" ");
}

void CodeHeapState::print_space_single(outputStream* out, unsigned short space) {
  size_t  space_in_bytes = ((unsigned int)space)<<log2_seg_size;
  char    fraction       = (space == 0) ? ' ' : (space_in_bytes >= granule_size-1) ? '*' : char('0'+10*space_in_bytes/granule_size);
  out->print("%c", fraction);
}

void CodeHeapState::print_age_single(outputStream* out, unsigned int age) {
  unsigned int indicator = 0;
  unsigned int age_range = 256;
  if (age > 0) {
    while ((age_range > 0) && (latest_compilation_id-age > latest_compilation_id/age_range)) {
      age_range /= 2;
      indicator += 1;
    }
    out->print("%c", char('0'+indicator));
  } else {
    out->print(" ");
  }
}

void CodeHeapState::print_line_delim(outputStream* out, outputStream* ast, char* low_bound, unsigned int ix, unsigned int gpl) {
  if (ix % gpl == 0) {
    if (ix > 0) {
      ast->print("|");
    }
    ast->cr();
    assert(out == ast, "must use the same stream!");

    ast->print(INTPTR_FORMAT, p2i(low_bound + ix*granule_size));
    ast->fill_to(19);
    ast->print("(+" PTR32_FORMAT "): |", (unsigned int)(ix*granule_size));
  }
}

void CodeHeapState::print_line_delim(outputStream* out, bufferedStream* ast, char* low_bound, unsigned int ix, unsigned int gpl) {
  assert(out != ast, "must not use the same stream!");
  if (ix % gpl == 0) {
    if (ix > 0) {
      ast->print("|");
    }
    ast->cr();

    // can't use BUFFEREDSTREAM_FLUSH_IF("", 512) here.
    // can't use this expression. bufferedStream::capacity() does not exist.
    // if ((ast->capacity() - ast->size()) < 512) {
    // Assume instead that default bufferedStream capacity (4K) was used.
    if (ast->size() > 3*K) {
      ttyLocker ttyl;
      out->print("%s", ast->as_string());
      ast->reset();
    }

    ast->print(INTPTR_FORMAT, p2i(low_bound + ix*granule_size));
    ast->fill_to(19);
    ast->print("(+" PTR32_FORMAT "): |", (unsigned int)(ix*granule_size));
  }
}

// Find out which blob type we have at hand.
// Return "noType" if anything abnormal is detected.
CodeHeapState::blobType CodeHeapState::get_cbType(CodeBlob* cb) {
  if (cb != NULL) {
    if (cb->is_runtime_stub())                return runtimeStub;
    if (cb->is_deoptimization_stub())         return deoptimizationStub;
    if (cb->is_uncommon_trap_stub())          return uncommonTrapStub;
    if (cb->is_exception_stub())              return exceptionStub;
    if (cb->is_safepoint_stub())              return safepointStub;
    if (cb->is_adapter_blob())                return adapterBlob;
    if (cb->is_method_handles_adapter_blob()) return mh_adapterBlob;
    if (cb->is_buffer_blob())                 return bufferBlob;

    //---<  access these fields only if we own CodeCache_lock and Compile_lock  >---
    // Should be ensured by caller. aggregate() and print_names() do that.
    if (holding_required_locks()) {
      nmethod*  nm = cb->as_nmethod_or_null();
      if (nm != NULL) { // no is_readable check required, nm = (nmethod*)cb.
        if (nm->is_zombie())        return nMethod_dead;
        if (nm->is_unloaded())      return nMethod_unloaded;
        if (nm->is_in_use())        return nMethod_inuse;
        if (nm->is_alive() && !(nm->is_not_entrant()))   return nMethod_notused;
        if (nm->is_alive())         return nMethod_alive;
        return nMethod_dead;
      }
    }
  }
  return noType;
}

// make sure the blob at hand is not garbage.
bool CodeHeapState::blob_access_is_safe(CodeBlob* this_blob) {
  return (this_blob != NULL) && // a blob must have been found, obviously
         (this_blob->header_size() >= 0) &&
         (this_blob->relocation_size() >= 0) &&
         ((address)this_blob + this_blob->header_size() == (address)(this_blob->relocation_begin())) &&
         ((address)this_blob + CodeBlob::align_code_offset(this_blob->header_size() + this_blob->relocation_size()) == (address)(this_blob->content_begin()));
}

// make sure the nmethod at hand (and the linked method) is not garbage.
bool CodeHeapState::nmethod_access_is_safe(nmethod* nm) {
  Method* method = (nm == NULL) ? NULL : nm->method(); // nm->method() was found to be uninitialized, i.e. != NULL, but invalid.
  return (nm != NULL) && (method != NULL) && nm->is_alive() && (method->signature() != NULL);
}

bool CodeHeapState::holding_required_locks() {
  return SafepointSynchronize::is_at_safepoint() ||
        (CodeCache_lock->owned_by_self() && Compile_lock->owned_by_self());
}
