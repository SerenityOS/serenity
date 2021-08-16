/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_MUTABLENUMASPACE_HPP
#define SHARE_GC_PARALLEL_MUTABLENUMASPACE_HPP

#include "gc/parallel/mutableSpace.hpp"
#include "gc/shared/gcUtil.hpp"
#include "runtime/globals.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"

/*
 *    The NUMA-aware allocator (MutableNUMASpace) is basically a modification
 * of MutableSpace which preserves interfaces but implements different
 * functionality. The space is split into chunks for each locality group
 * (resizing for adaptive size policy is also supported). For each thread
 * allocations are performed in the chunk corresponding to the home locality
 * group of the thread. Whenever any chunk fills-in the young generation
 * collection occurs.
 *   The chunks can be also be adaptively resized. The idea behind the adaptive
 * sizing is to reduce the loss of the space in the eden due to fragmentation.
 * The main cause of fragmentation is uneven allocation rates of threads.
 * The allocation rate difference between locality groups may be caused either by
 * application specifics or by uneven LWP distribution by the OS. Besides,
 * application can have less threads then the number of locality groups.
 * In order to resize the chunk we measure the allocation rate of the
 * application between collections. After that we reshape the chunks to reflect
 * the allocation rate pattern. The AdaptiveWeightedAverage exponentially
 * decaying average is used to smooth the measurements. The NUMASpaceResizeRate
 * parameter is used to control the adaptation speed by restricting the number of
 * bytes that can be moved during the adaptation phase.
 *   Chunks may contain pages from a wrong locality group. The page-scanner has
 * been introduced to address the problem. Remote pages typically appear due to
 * the memory shortage in the target locality group. Besides Solaris would
 * allocate a large page from the remote locality group even if there are small
 * local pages available. The page-scanner scans the pages right after the
 * collection and frees remote pages in hope that subsequent reallocation would
 * be more successful. This approach proved to be useful on systems with high
 * load where multiple processes are competing for the memory.
 */

class MutableNUMASpace : public MutableSpace {
  friend class VMStructs;

  class LGRPSpace : public CHeapObj<mtGC> {
    int _lgrp_id;
    MutableSpace* _space;
    MemRegion _invalid_region;
    AdaptiveWeightedAverage *_alloc_rate;
    bool _allocation_failed;

    struct SpaceStats {
      size_t _local_space, _remote_space, _unbiased_space, _uncommited_space;
      size_t _large_pages, _small_pages;

      SpaceStats() {
        _local_space = 0;
        _remote_space = 0;
        _unbiased_space = 0;
        _uncommited_space = 0;
        _large_pages = 0;
        _small_pages = 0;
      }
    };

    SpaceStats _space_stats;

    char* _last_page_scanned;
    char* last_page_scanned()            { return _last_page_scanned; }
    void set_last_page_scanned(char* p)  { _last_page_scanned = p;    }
   public:
    LGRPSpace(int l, size_t alignment) : _lgrp_id(l), _allocation_failed(false), _last_page_scanned(NULL) {
      _space = new MutableSpace(alignment);
      _alloc_rate = new AdaptiveWeightedAverage(NUMAChunkResizeWeight);
    }
    ~LGRPSpace() {
      delete _space;
      delete _alloc_rate;
    }

    void add_invalid_region(MemRegion r) {
      if (!_invalid_region.is_empty()) {
      _invalid_region.set_start(MIN2(_invalid_region.start(), r.start()));
      _invalid_region.set_end(MAX2(_invalid_region.end(), r.end()));
      } else {
      _invalid_region = r;
      }
    }

    static bool equals(void* lgrp_id_value, LGRPSpace* p) {
      return *(int*)lgrp_id_value == p->lgrp_id();
    }

    // Report a failed allocation.
    void set_allocation_failed() { _allocation_failed = true;  }

    void sample() {
      // If there was a failed allocation make allocation rate equal
      // to the size of the whole chunk. This ensures the progress of
      // the adaptation process.
      size_t alloc_rate_sample;
      if (_allocation_failed) {
        alloc_rate_sample = space()->capacity_in_bytes();
        _allocation_failed = false;
      } else {
        alloc_rate_sample = space()->used_in_bytes();
      }
      alloc_rate()->sample(alloc_rate_sample);
    }

    MemRegion invalid_region() const                { return _invalid_region;      }
    void set_invalid_region(MemRegion r)            { _invalid_region = r;         }
    int lgrp_id() const                             { return _lgrp_id;             }
    MutableSpace* space() const                     { return _space;               }
    AdaptiveWeightedAverage* alloc_rate() const     { return _alloc_rate;          }
    void clear_alloc_rate()                         { _alloc_rate->clear();        }
    SpaceStats* space_stats()                       { return &_space_stats;        }
    void clear_space_stats()                        { _space_stats = SpaceStats(); }

    void accumulate_statistics(size_t page_size);
    void scan_pages(size_t page_size, size_t page_count);
  };

  GrowableArray<LGRPSpace*>* _lgrp_spaces;
  size_t _page_size;
  unsigned _adaptation_cycles, _samples_count;

  bool _must_use_large_pages;

  void set_page_size(size_t psz)                     { _page_size = psz;          }
  size_t page_size() const                           { return _page_size;         }

  unsigned adaptation_cycles()                       { return _adaptation_cycles; }
  void set_adaptation_cycles(int v)                  { _adaptation_cycles = v;    }

  unsigned samples_count()                           { return _samples_count;     }
  void increment_samples_count()                     { ++_samples_count;          }

  size_t _base_space_size;
  void set_base_space_size(size_t v)                 { _base_space_size = v;      }
  size_t base_space_size() const                     { return _base_space_size;   }

  // Check if the NUMA topology has changed. Add and remove spaces if needed.
  // The update can be forced by setting the force parameter equal to true.
  bool update_layout(bool force);
  // Bias region towards the lgrp.
  void bias_region(MemRegion mr, int lgrp_id);
  // Free pages in a given region.
  void free_region(MemRegion mr);
  // Get current chunk size.
  size_t current_chunk_size(int i);
  // Get default chunk size (equally divide the space).
  size_t default_chunk_size();
  // Adapt the chunk size to follow the allocation rate.
  size_t adaptive_chunk_size(int i, size_t limit);
  // Scan and free invalid pages.
  void scan_pages(size_t page_count);
  // Return the bottom_region and the top_region. Align them to page_size() boundary.
  // |------------------new_region---------------------------------|
  // |----bottom_region--|---intersection---|------top_region------|
  void select_tails(MemRegion new_region, MemRegion intersection,
                    MemRegion* bottom_region, MemRegion *top_region);
  // Try to merge the invalid region with the bottom or top region by decreasing
  // the intersection area. Return the invalid_region aligned to the page_size()
  // boundary if it's inside the intersection. Return non-empty invalid_region
  // if it lies inside the intersection (also page-aligned).
  // |------------------new_region---------------------------------|
  // |----------------|-------invalid---|--------------------------|
  // |----bottom_region--|---intersection---|------top_region------|
  void merge_regions(MemRegion new_region, MemRegion* intersection,
                     MemRegion *invalid_region);

 public:
  GrowableArray<LGRPSpace*>* lgrp_spaces() const     { return _lgrp_spaces;       }
  MutableNUMASpace(size_t alignment);
  virtual ~MutableNUMASpace();
  // Space initialization.
  virtual void initialize(MemRegion mr,
                          bool clear_space,
                          bool mangle_space,
                          bool setup_pages = SetupPages,
                          WorkGang* pretouch_gang = NULL);
  // Update space layout if necessary. Do all adaptive resizing job.
  virtual void update();
  // Update allocation rate averages.
  virtual void accumulate_statistics();

  virtual void clear(bool mangle_space);
  virtual void mangle_unused_area() PRODUCT_RETURN;
  virtual void mangle_unused_area_complete() PRODUCT_RETURN;
  virtual void mangle_region(MemRegion mr) PRODUCT_RETURN;
  virtual void check_mangled_unused_area(HeapWord* limit) PRODUCT_RETURN;
  virtual void check_mangled_unused_area_complete() PRODUCT_RETURN;
  virtual void set_top_for_allocations(HeapWord* v) PRODUCT_RETURN;
  virtual void set_top_for_allocations() PRODUCT_RETURN;

  virtual void ensure_parsability();
  virtual size_t used_in_words() const;
  virtual size_t free_in_words() const;

  using MutableSpace::capacity_in_words;
  virtual size_t capacity_in_words(Thread* thr) const;
  virtual size_t tlab_capacity(Thread* thr) const;
  virtual size_t tlab_used(Thread* thr) const;
  virtual size_t unsafe_max_tlab_alloc(Thread* thr) const;

  // Allocation (return NULL if full)
  virtual HeapWord* cas_allocate(size_t word_size);

  // Debugging
  virtual void print_on(outputStream* st) const;
  virtual void print_short_on(outputStream* st) const;
  virtual void verify();

  virtual void set_top(HeapWord* value);
};

#endif // SHARE_GC_PARALLEL_MUTABLENUMASPACE_HPP
