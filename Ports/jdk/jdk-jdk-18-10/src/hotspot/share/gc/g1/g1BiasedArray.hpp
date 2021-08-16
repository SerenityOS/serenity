/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1BIASEDARRAY_HPP
#define SHARE_GC_G1_G1BIASEDARRAY_HPP

#include "memory/allocation.hpp"
#include "memory/memRegion.hpp"
#include "utilities/debug.hpp"
#include "utilities/powerOfTwo.hpp"

// Implements the common base functionality for arrays that contain provisions
// for accessing its elements using a biased index.
// The element type is defined by the instantiating the template.
class G1BiasedMappedArrayBase : public CHeapObj<mtGC> {
  friend class VMStructs;

  void* _alloc_base;      // the address the unpadded array has been allocated to

public:
  typedef size_t idx_t;

protected:
  address _base;          // the real base address
  size_t _length;         // the length of the array
  address _biased_base;   // base address biased by "bias" elements
  size_t _bias;           // the bias, i.e. the offset biased_base is located to the right in elements
  uint _shift_by;         // the amount of bits to shift right when mapping to an index of the array.

protected:
  G1BiasedMappedArrayBase();

  // Allocate a new array, generic version.
  address create_new_base_array(size_t length, size_t elem_size);

  // Initialize the members of this class. The biased start address of this array
  // is the bias (in elements) multiplied by the element size.
  void initialize_base(address base, size_t length, size_t bias, size_t elem_size, uint shift_by) {
    assert(base != NULL, "just checking");
    assert(length > 0, "just checking");
    assert(shift_by < sizeof(uintptr_t) * 8, "Shifting by %u, larger than word size?", shift_by);
    _base = base;
    _length = length;
    _biased_base = base - (bias * elem_size);
    _bias = bias;
    _shift_by = shift_by;
  }

  // Allocate and initialize this array to cover the heap addresses in the range
  // of [bottom, end).
  void initialize(HeapWord* bottom, HeapWord* end, size_t target_elem_size_in_bytes, size_t mapping_granularity_in_bytes) {
    assert(mapping_granularity_in_bytes > 0, "just checking");
    assert(is_power_of_2(mapping_granularity_in_bytes),
           "mapping granularity must be power of 2, is " SIZE_FORMAT, mapping_granularity_in_bytes);
    assert((uintptr_t)bottom % mapping_granularity_in_bytes == 0,
           "bottom mapping area address must be a multiple of mapping granularity " SIZE_FORMAT ", is  " PTR_FORMAT,
           mapping_granularity_in_bytes, p2i(bottom));
    assert((uintptr_t)end % mapping_granularity_in_bytes == 0,
           "end mapping area address must be a multiple of mapping granularity " SIZE_FORMAT ", is " PTR_FORMAT,
           mapping_granularity_in_bytes, p2i(end));
    size_t num_target_elems = pointer_delta(end, bottom, mapping_granularity_in_bytes);
    idx_t bias = (uintptr_t)bottom / mapping_granularity_in_bytes;
    address base = create_new_base_array(num_target_elems, target_elem_size_in_bytes);
    initialize_base(base, num_target_elems, bias, target_elem_size_in_bytes, log2i_exact(mapping_granularity_in_bytes));
  }

  size_t bias() const { return _bias; }
  uint shift_by() const { return _shift_by; }

  void verify_index(idx_t index) const PRODUCT_RETURN;
  void verify_biased_index(idx_t biased_index) const PRODUCT_RETURN;
  void verify_biased_index_inclusive_end(idx_t biased_index) const PRODUCT_RETURN;

public:
  virtual ~G1BiasedMappedArrayBase();

  // Return the length of the array in elements.
  size_t length() const { return _length; }
};

// Array that provides biased access and mapping from (valid) addresses in the
// heap into this array.
template<class T>
class G1BiasedMappedArray : public G1BiasedMappedArrayBase {
public:
  typedef G1BiasedMappedArrayBase::idx_t idx_t;

  T* base() const { return (T*)G1BiasedMappedArrayBase::_base; }
  // Return the element of the given array at the given index. Assume
  // the index is valid. This is a convenience method that does sanity
  // checking on the index.
  T get_by_index(idx_t index) const {
    verify_index(index);
    return this->base()[index];
  }

  // Set the element of the given array at the given index to the
  // given value. Assume the index is valid. This is a convenience
  // method that does sanity checking on the index.
  void set_by_index(idx_t index, T value) {
    verify_index(index);
    this->base()[index] = value;
  }

  // The raw biased base pointer.
  T* biased_base() const { return (T*)G1BiasedMappedArrayBase::_biased_base; }

  // Return the element of the given array that covers the given word in the
  // heap. Assumes the index is valid.
  T get_by_address(HeapWord* value) const {
    idx_t biased_index = ((uintptr_t)value) >> this->shift_by();
    this->verify_biased_index(biased_index);
    return biased_base()[biased_index];
  }

  T* get_ref_by_index(uintptr_t index) const {
    verify_index(index);
    return &this->base()[index];
  }

  // Return the index of the element of the given array that covers the given
  // word in the heap.
  idx_t get_index_by_address(HeapWord* value) const {
    idx_t biased_index = ((uintptr_t)value) >> this->shift_by();
    this->verify_biased_index(biased_index);
    return biased_index - _bias;
  }

  // Set the value of the array entry that corresponds to the given array.
  void set_by_address(HeapWord * address, T value) {
    idx_t biased_index = ((uintptr_t)address) >> this->shift_by();
    this->verify_biased_index(biased_index);
    biased_base()[biased_index] = value;
  }

  // Set the value of all array entries that correspond to addresses
  // in the specified MemRegion.
  void set_by_address(MemRegion range, T value) {
    idx_t biased_start = ((uintptr_t)range.start()) >> this->shift_by();
    idx_t biased_last = ((uintptr_t)range.last()) >> this->shift_by();
    this->verify_biased_index(biased_start);
    this->verify_biased_index(biased_last);
    for (idx_t i = biased_start; i <= biased_last; i++) {
      biased_base()[i] = value;
    }
  }

protected:
  // Returns the address of the element the given address maps to
  T* address_mapped_to(HeapWord* address) {
    idx_t biased_index = ((uintptr_t)address) >> this->shift_by();
    this->verify_biased_index_inclusive_end(biased_index);
    return biased_base() + biased_index;
  }

public:
  // Return the smallest address (inclusive) in the heap that this array covers.
  HeapWord* bottom_address_mapped() const {
    return (HeapWord*) ((uintptr_t)this->bias() << this->shift_by());
  }

  // Return the highest address (exclusive) in the heap that this array covers.
  HeapWord* end_address_mapped() const {
    return (HeapWord*) ((uintptr_t)(this->bias() + this->length()) << this->shift_by());
  }

protected:
  virtual T default_value() const = 0;
  // Set all elements of the given array to the given value.
  void clear() {
    T value = default_value();
    for (idx_t i = 0; i < length(); i++) {
      set_by_index(i, value);
    }
  }
public:
  G1BiasedMappedArray() {}

  // Allocate and initialize this array to cover the heap addresses in the given MemRegion.
  void initialize(MemRegion region, size_t mapping_granularity) {
    G1BiasedMappedArrayBase::initialize(region.start(), region.end(), sizeof(T), mapping_granularity);
    this->clear();
  }
};

#endif // SHARE_GC_G1_G1BIASEDARRAY_HPP
