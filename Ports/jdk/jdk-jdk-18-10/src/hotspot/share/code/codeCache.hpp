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

#ifndef SHARE_CODE_CODECACHE_HPP
#define SHARE_CODE_CODECACHE_HPP

#include "code/codeBlob.hpp"
#include "code/nmethod.hpp"
#include "gc/shared/gcBehaviours.hpp"
#include "memory/allocation.hpp"
#include "memory/heap.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/mutexLocker.hpp"

// The CodeCache implements the code cache for various pieces of generated
// code, e.g., compiled java methods, runtime stubs, transition frames, etc.
// The entries in the CodeCache are all CodeBlob's.

// -- Implementation --
// The CodeCache consists of one or more CodeHeaps, each of which contains
// CodeBlobs of a specific CodeBlobType. Currently heaps for the following
// types are available:
//  - Non-nmethods: Non-nmethods like Buffers, Adapters and Runtime Stubs
//  - Profiled nmethods: nmethods that are profiled, i.e., those
//    executed at level 2 or 3
//  - Non-Profiled nmethods: nmethods that are not profiled, i.e., those
//    executed at level 1 or 4 and native methods
//  - All: Used for code of all types if code cache segmentation is disabled.
//
// In the rare case of the non-nmethod code heap getting full, non-nmethod code
// will be stored in the non-profiled code heap as a fallback solution.
//
// Depending on the availability of compilers and compilation mode there
// may be fewer heaps. The size of the code heaps depends on the values of
// ReservedCodeCacheSize, NonProfiledCodeHeapSize and ProfiledCodeHeapSize
// (see CodeCache::heap_available(..) and CodeCache::initialize_heaps(..)
// for details).
//
// Code cache segmentation is controlled by the flag SegmentedCodeCache.
// If turned off, all code types are stored in a single code heap. By default
// code cache segmentation is turned on if tiered mode is enabled and
// ReservedCodeCacheSize >= 240 MB.
//
// All methods of the CodeCache accepting a CodeBlobType only apply to
// CodeBlobs of the given type. For example, iteration over the
// CodeBlobs of a specific type can be done by using CodeCache::first_blob(..)
// and CodeCache::next_blob(..) and providing the corresponding CodeBlobType.
//
// IMPORTANT: If you add new CodeHeaps to the code cache or change the
// existing ones, make sure to adapt the dtrace scripts (jhelper.d) for
// Solaris and BSD.

class ExceptionCache;
class KlassDepChange;
class OopClosure;
class ShenandoahParallelCodeHeapIterator;

class CodeCache : AllStatic {
  friend class VMStructs;
  friend class JVMCIVMStructs;
  template <class T, class Filter> friend class CodeBlobIterator;
  friend class WhiteBox;
  friend class CodeCacheLoader;
  friend class ShenandoahParallelCodeHeapIterator;
 private:
  // CodeHeaps of the cache
  static GrowableArray<CodeHeap*>* _heaps;
  static GrowableArray<CodeHeap*>* _compiled_heaps;
  static GrowableArray<CodeHeap*>* _nmethod_heaps;
  static GrowableArray<CodeHeap*>* _allocable_heaps;

  static address _low_bound;                            // Lower bound of CodeHeap addresses
  static address _high_bound;                           // Upper bound of CodeHeap addresses
  static int _number_of_nmethods_with_dependencies;     // Total number of nmethods with dependencies
  static uint8_t _unloading_cycle;                      // Global state for recognizing old nmethods that need to be unloaded

  static ExceptionCache* volatile _exception_cache_purge_list;

  // CodeHeap management
  static void initialize_heaps();                             // Initializes the CodeHeaps
  // Check the code heap sizes set by the user via command line
  static void check_heap_sizes(size_t non_nmethod_size, size_t profiled_size, size_t non_profiled_size, size_t cache_size, bool all_set);
  // Creates a new heap with the given name and size, containing CodeBlobs of the given type
  static void add_heap(ReservedSpace rs, const char* name, int code_blob_type);
  static CodeHeap* get_code_heap_containing(void* p);         // Returns the CodeHeap containing the given pointer, or NULL
  static CodeHeap* get_code_heap(const CodeBlob* cb);         // Returns the CodeHeap for the given CodeBlob
  static CodeHeap* get_code_heap(int code_blob_type);         // Returns the CodeHeap for the given CodeBlobType
  // Returns the name of the VM option to set the size of the corresponding CodeHeap
  static const char* get_code_heap_flag_name(int code_blob_type);
  static ReservedCodeSpace reserve_heap_memory(size_t size);  // Reserves one continuous chunk of memory for the CodeHeaps

  // Iteration
  static CodeBlob* first_blob(CodeHeap* heap);                // Returns the first CodeBlob on the given CodeHeap
  static CodeBlob* first_blob(int code_blob_type);            // Returns the first CodeBlob of the given type
  static CodeBlob* next_blob(CodeHeap* heap, CodeBlob* cb);   // Returns the next CodeBlob on the given CodeHeap

  static size_t bytes_allocated_in_freelists();
  static int    allocated_segments();
  static size_t freelists_length();

  // Make private to prevent unsafe calls.  Not all CodeBlob*'s are embedded in a CodeHeap.
  static bool contains(CodeBlob *p) { fatal("don't call me!"); return false; }

 public:
  // Initialization
  static void initialize();
  static size_t page_size(bool aligned = true, size_t min_pages = 1); // Returns the page size used by the CodeCache

  static int code_heap_compare(CodeHeap* const &lhs, CodeHeap* const &rhs);

  static void add_heap(CodeHeap* heap);
  static const GrowableArray<CodeHeap*>* heaps() { return _heaps; }
  static const GrowableArray<CodeHeap*>* compiled_heaps() { return _compiled_heaps; }
  static const GrowableArray<CodeHeap*>* nmethod_heaps() { return _nmethod_heaps; }

  // Allocation/administration
  static CodeBlob* allocate(int size, int code_blob_type, bool handle_alloc_failure = true, int orig_code_blob_type = CodeBlobType::All); // allocates a new CodeBlob
  static void commit(CodeBlob* cb);                        // called when the allocated CodeBlob has been filled
  static int  alignment_unit();                            // guaranteed alignment of all CodeBlobs
  static int  alignment_offset();                          // guaranteed offset of first CodeBlob byte within alignment unit (i.e., allocation header)
  static void free(CodeBlob* cb);                          // frees a CodeBlob
  static void free_unused_tail(CodeBlob* cb, size_t used); // frees the unused tail of a CodeBlob (only used by TemplateInterpreter::initialize())
  static bool contains(void *p);                           // returns whether p is included
  static bool contains(nmethod* nm);                       // returns whether nm is included
  static void blobs_do(void f(CodeBlob* cb));              // iterates over all CodeBlobs
  static void blobs_do(CodeBlobClosure* f);                // iterates over all CodeBlobs
  static void nmethods_do(void f(nmethod* nm));            // iterates over all nmethods
  static void metadata_do(MetadataClosure* f);             // iterates over metadata in alive nmethods

  // Lookup
  static CodeBlob* find_blob(void* start);              // Returns the CodeBlob containing the given address
  static CodeBlob* find_blob_unsafe(void* start);       // Same as find_blob but does not fail if looking up a zombie method
  static nmethod*  find_nmethod(void* start);           // Returns the nmethod containing the given address
  static CompiledMethod* find_compiled(void* start);

  static int       blob_count();                        // Returns the total number of CodeBlobs in the cache
  static int       blob_count(int code_blob_type);
  static int       adapter_count();                     // Returns the total number of Adapters in the cache
  static int       adapter_count(int code_blob_type);
  static int       nmethod_count();                     // Returns the total number of nmethods in the cache
  static int       nmethod_count(int code_blob_type);

  // GC support
  static void verify_oops();
  // If any oops are not marked this method unloads (i.e., breaks root links
  // to) any unmarked codeBlobs in the cache.  Sets "marked_for_unloading"
  // to "true" iff some code got unloaded.
  // "unloading_occurred" controls whether metadata should be cleaned because of class unloading.
  class UnloadingScope: StackObj {
    ClosureIsUnloadingBehaviour _is_unloading_behaviour;
    IsUnloadingBehaviour*       _saved_behaviour;

  public:
    UnloadingScope(BoolObjectClosure* is_alive);
    ~UnloadingScope();
  };

  static void do_unloading(BoolObjectClosure* is_alive, bool unloading_occurred);
  static uint8_t unloading_cycle() { return _unloading_cycle; }
  static void increment_unloading_cycle();
  static void release_exception_cache(ExceptionCache* entry);
  static void purge_exception_caches();

  // Printing/debugging
  static void print();                           // prints summary
  static void print_internals();
  static void print_memory_overhead();
  static void verify();                          // verifies the code cache
  static void print_trace(const char* event, CodeBlob* cb, int size = 0) PRODUCT_RETURN;
  static void print_summary(outputStream* st, bool detailed = true); // Prints a summary of the code cache usage
  static void log_state(outputStream* st);
  LINUX_ONLY(static void write_perf_map();)
  static const char* get_code_heap_name(int code_blob_type)  { return (heap_available(code_blob_type) ? get_code_heap(code_blob_type)->name() : "Unused"); }
  static void report_codemem_full(int code_blob_type, bool print);

  // Dcmd (Diagnostic commands)
  static void print_codelist(outputStream* st);
  static void print_layout(outputStream* st);

  // The full limits of the codeCache
  static address low_bound()                          { return _low_bound; }
  static address low_bound(int code_blob_type);
  static address high_bound()                         { return _high_bound; }
  static address high_bound(int code_blob_type);

  // Profiling
  static size_t capacity();
  static size_t unallocated_capacity(int code_blob_type);
  static size_t unallocated_capacity();
  static size_t max_capacity();

  static double reverse_free_ratio(int code_blob_type);

  static void clear_inline_caches();                  // clear all inline caches
  static void cleanup_inline_caches();                // clean unloaded/zombie nmethods from inline caches

  // Returns true if an own CodeHeap for the given CodeBlobType is available
  static bool heap_available(int code_blob_type);

  // Returns the CodeBlobType for the given CompiledMethod
  static int get_code_blob_type(CompiledMethod* cm) {
    return get_code_heap(cm)->code_blob_type();
  }

  static bool code_blob_type_accepts_compiled(int type) {
    bool result = type == CodeBlobType::All || type <= CodeBlobType::MethodProfiled;
    return result;
  }

  static bool code_blob_type_accepts_nmethod(int type) {
    return type == CodeBlobType::All || type <= CodeBlobType::MethodProfiled;
  }

  static bool code_blob_type_accepts_allocable(int type) {
    return type <= CodeBlobType::All;
  }


  // Returns the CodeBlobType for the given compilation level
  static int get_code_blob_type(int comp_level) {
    if (comp_level == CompLevel_none ||
        comp_level == CompLevel_simple ||
        comp_level == CompLevel_full_optimization) {
      // Non profiled methods
      return CodeBlobType::MethodNonProfiled;
    } else if (comp_level == CompLevel_limited_profile ||
               comp_level == CompLevel_full_profile) {
      // Profiled methods
      return CodeBlobType::MethodProfiled;
    }
    ShouldNotReachHere();
    return 0;
  }

  static void verify_clean_inline_caches();
  static void verify_icholder_relocations();

  // Deoptimization
 private:
  static int  mark_for_deoptimization(KlassDepChange& changes);

 public:
  static void mark_all_nmethods_for_deoptimization();
  static int  mark_for_deoptimization(Method* dependee);
  static void make_marked_nmethods_not_entrant();

  // Flushing and deoptimization
  static void flush_dependents_on(InstanceKlass* dependee);

  // RedefineClasses support
  // Flushing and deoptimization in case of evolution
  static int  mark_dependents_for_evol_deoptimization();
  static void mark_all_nmethods_for_evol_deoptimization();
  static void flush_evol_dependents();
  static void old_nmethods_do(MetadataClosure* f) NOT_JVMTI_RETURN;
  static void unregister_old_nmethod(CompiledMethod* c) NOT_JVMTI_RETURN;

  // Support for fullspeed debugging
  static void flush_dependents_on_method(const methodHandle& dependee);

  // tells how many nmethods have dependencies
  static int number_of_nmethods_with_dependencies();

  static int get_codemem_full_count(int code_blob_type) {
    CodeHeap* heap = get_code_heap(code_blob_type);
    return (heap != NULL) ? heap->full_count() : 0;
  }

  // CodeHeap State Analytics.
  // interface methods for CodeHeap printing, called by CompileBroker
  static void aggregate(outputStream *out, size_t granularity);
  static void discard(outputStream *out);
  static void print_usedSpace(outputStream *out);
  static void print_freeSpace(outputStream *out);
  static void print_count(outputStream *out);
  static void print_space(outputStream *out);
  static void print_age(outputStream *out);
  static void print_names(outputStream *out);
};


// Iterator to iterate over nmethods in the CodeCache.
template <class T, class Filter> class CodeBlobIterator : public StackObj {
 public:
  enum LivenessFilter { all_blobs, only_alive, only_alive_and_not_unloading };

 private:
  CodeBlob* _code_blob;   // Current CodeBlob
  GrowableArrayIterator<CodeHeap*> _heap;
  GrowableArrayIterator<CodeHeap*> _end;
  bool _only_alive;
  bool _only_not_unloading;

 public:
  CodeBlobIterator(LivenessFilter filter, T* nm = NULL)
    : _only_alive(filter == only_alive || filter == only_alive_and_not_unloading),
      _only_not_unloading(filter == only_alive_and_not_unloading)
  {
    if (Filter::heaps() == NULL) {
      return;
    }
    _heap = Filter::heaps()->begin();
    _end = Filter::heaps()->end();
    // If set to NULL, initialized by first call to next()
    _code_blob = (CodeBlob*)nm;
    if (nm != NULL) {
      while(!(*_heap)->contains_blob(_code_blob)) {
        ++_heap;
      }
      assert((*_heap)->contains_blob(_code_blob), "match not found");
    }
  }

  // Advance iterator to next blob
  bool next() {
    assert_locked_or_safepoint(CodeCache_lock);

    for (;;) {
      // Walk through heaps as required
      if (!next_blob()) {
        if (_heap == _end) {
          return false;
        }
        ++_heap;
        continue;
      }

      // Filter is_alive as required
      if (_only_alive && !_code_blob->is_alive()) {
        continue;
      }

      // Filter is_unloading as required
      if (_only_not_unloading) {
        CompiledMethod* cm = _code_blob->as_compiled_method_or_null();
        if (cm != NULL && cm->is_unloading()) {
          continue;
        }
      }

      return true;
    }
  }

  bool end()  const { return _code_blob == NULL; }
  T* method() const { return (T*)_code_blob; }

private:

  // Advance iterator to the next blob in the current code heap
  bool next_blob() {
    if (_heap == _end) {
      return false;
    }
    CodeHeap *heap = *_heap;
    // Get first method CodeBlob
    if (_code_blob == NULL) {
      _code_blob = CodeCache::first_blob(heap);
      if (_code_blob == NULL) {
        return false;
      } else if (Filter::apply(_code_blob)) {
        return true;
      }
    }
    // Search for next method CodeBlob
    _code_blob = CodeCache::next_blob(heap, _code_blob);
    while (_code_blob != NULL && !Filter::apply(_code_blob)) {
      _code_blob = CodeCache::next_blob(heap, _code_blob);
    }
    return _code_blob != NULL;
  }
};


struct CompiledMethodFilter {
  static bool apply(CodeBlob* cb) { return cb->is_compiled(); }
  static const GrowableArray<CodeHeap*>* heaps() { return CodeCache::compiled_heaps(); }
};


struct NMethodFilter {
  static bool apply(CodeBlob* cb) { return cb->is_nmethod(); }
  static const GrowableArray<CodeHeap*>* heaps() { return CodeCache::nmethod_heaps(); }
};

struct AllCodeBlobsFilter {
  static bool apply(CodeBlob* cb) { return true; }
  static const GrowableArray<CodeHeap*>* heaps() { return CodeCache::heaps(); }
};

typedef CodeBlobIterator<CompiledMethod, CompiledMethodFilter> CompiledMethodIterator;
typedef CodeBlobIterator<nmethod, NMethodFilter> NMethodIterator;
typedef CodeBlobIterator<CodeBlob, AllCodeBlobsFilter> AllCodeBlobsIterator;

#endif // SHARE_CODE_CODECACHE_HPP
