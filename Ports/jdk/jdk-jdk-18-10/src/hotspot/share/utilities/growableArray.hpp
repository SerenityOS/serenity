/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_GROWABLEARRAY_HPP
#define SHARE_UTILITIES_GROWABLEARRAY_HPP

#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"
#include "utilities/powerOfTwo.hpp"

// A growable array.

/*************************************************************************/
/*                                                                       */
/*     WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING   */
/*                                                                       */
/* Should you use GrowableArrays to contain handles you must be certain  */
/* that the GrowableArray does not outlive the HandleMark that contains  */
/* the handles. Since GrowableArrays are typically resource allocated    */
/* the following is an example of INCORRECT CODE,                        */
/*                                                                       */
/* ResourceMark rm;                                                      */
/* GrowableArray<Handle>* arr = new GrowableArray<Handle>(size);         */
/* if (blah) {                                                           */
/*    while (...) {                                                      */
/*      HandleMark hm;                                                   */
/*      ...                                                              */
/*      Handle h(THREAD, some_oop);                                      */
/*      arr->append(h);                                                  */
/*    }                                                                  */
/* }                                                                     */
/* if (arr->length() != 0 ) {                                            */
/*    oop bad_oop = arr->at(0)(); // Handle is BAD HERE.                 */
/*    ...                                                                */
/* }                                                                     */
/*                                                                       */
/* If the GrowableArrays you are creating is C_Heap allocated then it    */
/* should not hold handles since the handles could trivially try and     */
/* outlive their HandleMark. In some situations you might need to do     */
/* this and it would be legal but be very careful and see if you can do  */
/* the code in some other manner.                                        */
/*                                                                       */
/*************************************************************************/

// Non-template base class responsible for handling the length and max.


class GrowableArrayBase : public ResourceObj {
  friend class VMStructs;

protected:
  // Current number of accessible elements
  int _len;
  // Current number of allocated elements
  int _max;

  GrowableArrayBase(int initial_max, int initial_len) :
      _len(initial_len),
      _max(initial_max) {
    assert(_len >= 0 && _len <= _max, "initial_len too big");
  }

  ~GrowableArrayBase() {}

public:
  int   length() const          { return _len; }
  int   max_length() const      { return _max; }

  bool  is_empty() const        { return _len == 0; }
  bool  is_nonempty() const     { return _len != 0; }
  bool  is_full() const         { return _len == _max; }

  void  clear()                 { _len = 0; }
  void  trunc_to(int length)    {
    assert(length <= _len,"cannot increase length");
    _len = length;
  }
};

template <typename E> class GrowableArrayIterator;
template <typename E, typename UnaryPredicate> class GrowableArrayFilterIterator;

// Extends GrowableArrayBase with a typed data array.
//
// E: Element type
//
// The "view" adds function that don't grow or deallocate
// the _data array, so there's no need for an allocator.
//
// The "view" can be used to type erase the allocator classes
// of GrowableArrayWithAllocator.
template <typename E>
class GrowableArrayView : public GrowableArrayBase {
protected:
  E* _data; // data array

  GrowableArrayView<E>(E* data, int initial_max, int initial_len) :
      GrowableArrayBase(initial_max, initial_len), _data(data) {}

  ~GrowableArrayView() {}

public:
  const static GrowableArrayView EMPTY;

  bool operator==(const GrowableArrayView<E>& rhs) const {
    if (_len != rhs._len)
      return false;
    for (int i = 0; i < _len; i++) {
      if (at(i) != rhs.at(i)) {
        return false;
      }
    }
    return true;
  }

  bool operator!=(const GrowableArrayView<E>& rhs) const {
    return !(*this == rhs);
  }

  E& at(int i) {
    assert(0 <= i && i < _len, "illegal index");
    return _data[i];
  }

  E const& at(int i) const {
    assert(0 <= i && i < _len, "illegal index");
    return _data[i];
  }

  E* adr_at(int i) const {
    assert(0 <= i && i < _len, "illegal index");
    return &_data[i];
  }

  E first() const {
    assert(_len > 0, "empty list");
    return _data[0];
  }

  E top() const {
    assert(_len > 0, "empty list");
    return _data[_len-1];
  }

  E last() const {
    return top();
  }

  GrowableArrayIterator<E> begin() const {
    return GrowableArrayIterator<E>(this, 0);
  }

  GrowableArrayIterator<E> end() const {
    return GrowableArrayIterator<E>(this, length());
  }

  E pop() {
    assert(_len > 0, "empty list");
    return _data[--_len];
  }

  void at_put(int i, const E& elem) {
    assert(0 <= i && i < _len, "illegal index");
    _data[i] = elem;
  }

  bool contains(const E& elem) const {
    for (int i = 0; i < _len; i++) {
      if (_data[i] == elem) return true;
    }
    return false;
  }

  int  find(const E& elem) const {
    for (int i = 0; i < _len; i++) {
      if (_data[i] == elem) return i;
    }
    return -1;
  }

  int  find_from_end(const E& elem) const {
    for (int i = _len-1; i >= 0; i--) {
      if (_data[i] == elem) return i;
    }
    return -1;
  }

  int  find(void* token, bool f(void*, E)) const {
    for (int i = 0; i < _len; i++) {
      if (f(token, _data[i])) return i;
    }
    return -1;
  }

  int  find_from_end(void* token, bool f(void*, E)) const {
    // start at the end of the array
    for (int i = _len-1; i >= 0; i--) {
      if (f(token, _data[i])) return i;
    }
    return -1;
  }

  // Order preserving remove operations.

  void remove(const E& elem) {
    // Assuming that element does exist.
    bool removed = remove_if_existing(elem);
    if (removed) return;
    ShouldNotReachHere();
  }

  bool remove_if_existing(const E& elem) {
    // Returns TRUE if elem is removed.
    for (int i = 0; i < _len; i++) {
      if (_data[i] == elem) {
        remove_at(i);
        return true;
      }
    }
    return false;
  }

  void remove_at(int index) {
    assert(0 <= index && index < _len, "illegal index");
    for (int j = index + 1; j < _len; j++) {
      _data[j-1] = _data[j];
    }
    _len--;
  }

  // Remove all elements up to the index (exclusive). The order is preserved.
  void remove_till(int idx) {
    for (int i = 0, j = idx; j < length(); i++, j++) {
      at_put(i, at(j));
    }
    trunc_to(length() - idx);
  }

  // The order is changed.
  void delete_at(int index) {
    assert(0 <= index && index < _len, "illegal index");
    if (index < --_len) {
      // Replace removed element with last one.
      _data[index] = _data[_len];
    }
  }

  void sort(int f(E*, E*)) {
    qsort(_data, length(), sizeof(E), (_sort_Fn)f);
  }
  // sort by fixed-stride sub arrays:
  void sort(int f(E*, E*), int stride) {
    qsort(_data, length() / stride, sizeof(E) * stride, (_sort_Fn)f);
  }

  template <typename K, int compare(const K&, const E&)> int find_sorted(const K& key, bool& found) {
    found = false;
    int min = 0;
    int max = length() - 1;

    while (max >= min) {
      int mid = (int)(((uint)max + min) / 2);
      E value = at(mid);
      int diff = compare(key, value);
      if (diff > 0) {
        min = mid + 1;
      } else if (diff < 0) {
        max = mid - 1;
      } else {
        found = true;
        return mid;
      }
    }
    return min;
  }

  template <typename K>
  int find_sorted(CompareClosure<E>* cc, const K& key, bool& found) {
    found = false;
    int min = 0;
    int max = length() - 1;

    while (max >= min) {
      int mid = (int)(((uint)max + min) / 2);
      E value = at(mid);
      int diff = cc->do_compare(key, value);
      if (diff > 0) {
        min = mid + 1;
      } else if (diff < 0) {
        max = mid - 1;
      } else {
        found = true;
        return mid;
      }
    }
    return min;
  }

  size_t data_size_in_bytes() const {
    return _len * sizeof(E);
  }

  void print() const {
    tty->print("Growable Array " INTPTR_FORMAT, p2i(this));
    tty->print(": length %d (_max %d) { ", _len, _max);
    for (int i = 0; i < _len; i++) {
      tty->print(INTPTR_FORMAT " ", *(intptr_t*)&(_data[i]));
    }
    tty->print("}\n");
  }
};

template<typename E>
const GrowableArrayView<E> GrowableArrayView<E>::EMPTY(nullptr, 0, 0);

// GrowableArrayWithAllocator extends the "view" with
// the capability to grow and deallocate the data array.
//
// The allocator responsibility is delegated to the sub-class.
//
// Derived: The sub-class responsible for allocation / deallocation
//  - E* Derived::allocate()       - member function responsible for allocation
//  - void Derived::deallocate(E*) - member function responsible for deallocation
template <typename E, typename Derived>
class GrowableArrayWithAllocator : public GrowableArrayView<E> {
  friend class VMStructs;

  void grow(int j);

protected:
  GrowableArrayWithAllocator(E* data, int initial_max) :
      GrowableArrayView<E>(data, initial_max, 0) {
    for (int i = 0; i < initial_max; i++) {
      ::new ((void*)&data[i]) E();
    }
  }

  GrowableArrayWithAllocator(E* data, int initial_max, int initial_len, const E& filler) :
      GrowableArrayView<E>(data, initial_max, initial_len) {
    int i = 0;
    for (; i < initial_len; i++) {
      ::new ((void*)&data[i]) E(filler);
    }
    for (; i < initial_max; i++) {
      ::new ((void*)&data[i]) E();
    }
  }

  ~GrowableArrayWithAllocator() {}

public:
  int append(const E& elem) {
    if (this->_len == this->_max) grow(this->_len);
    int idx = this->_len++;
    this->_data[idx] = elem;
    return idx;
  }

  bool append_if_missing(const E& elem) {
    // Returns TRUE if elem is added.
    bool missed = !this->contains(elem);
    if (missed) append(elem);
    return missed;
  }

  void push(const E& elem) { append(elem); }

  E at_grow(int i, const E& fill = E()) {
    assert(0 <= i, "negative index");
    if (i >= this->_len) {
      if (i >= this->_max) grow(i);
      for (int j = this->_len; j <= i; j++)
        this->_data[j] = fill;
      this->_len = i+1;
    }
    return this->_data[i];
  }

  void at_put_grow(int i, const E& elem, const E& fill = E()) {
    assert(0 <= i, "negative index");
    if (i >= this->_len) {
      if (i >= this->_max) grow(i);
      for (int j = this->_len; j < i; j++)
        this->_data[j] = fill;
      this->_len = i+1;
    }
    this->_data[i] = elem;
  }

  // inserts the given element before the element at index i
  void insert_before(const int idx, const E& elem) {
    assert(0 <= idx && idx <= this->_len, "illegal index");
    if (this->_len == this->_max) grow(this->_len);
    for (int j = this->_len - 1; j >= idx; j--) {
      this->_data[j + 1] = this->_data[j];
    }
    this->_len++;
    this->_data[idx] = elem;
  }

  void insert_before(const int idx, const GrowableArrayView<E>* array) {
    assert(0 <= idx && idx <= this->_len, "illegal index");
    int array_len = array->length();
    int new_len = this->_len + array_len;
    if (new_len >= this->_max) grow(new_len);

    for (int j = this->_len - 1; j >= idx; j--) {
      this->_data[j + array_len] = this->_data[j];
    }

    for (int j = 0; j < array_len; j++) {
      this->_data[idx + j] = array->at(j);
    }

    this->_len += array_len;
  }

  void appendAll(const GrowableArrayView<E>* l) {
    for (int i = 0; i < l->length(); i++) {
      this->at_put_grow(this->_len, l->at(i), E());
    }
  }

  // Binary search and insertion utility.  Search array for element
  // matching key according to the static compare function.  Insert
  // that element is not already in the list.  Assumes the list is
  // already sorted according to compare function.
  template <int compare(const E&, const E&)> E insert_sorted(const E& key) {
    bool found;
    int location = GrowableArrayView<E>::template find_sorted<E, compare>(key, found);
    if (!found) {
      insert_before(location, key);
    }
    return this->at(location);
  }

  E insert_sorted(CompareClosure<E>* cc, const E& key) {
    bool found;
    int location = find_sorted(cc, key, found);
    if (!found) {
      insert_before(location, key);
    }
    return this->at(location);
  }

  void swap(GrowableArrayWithAllocator<E, Derived>* other) {
    ::swap(this->_data, other->_data);
    ::swap(this->_len, other->_len);
    ::swap(this->_max, other->_max);
  }

  void clear_and_deallocate();
};

template <typename E, typename Derived>
void GrowableArrayWithAllocator<E, Derived>::grow(int j) {
  int old_max = this->_max;
  // grow the array by increasing _max to the first power of two larger than the size we need
  this->_max = next_power_of_2((uint32_t)j);
  // j < _max
  E* newData = static_cast<Derived*>(this)->allocate();
  int i = 0;
  for (     ; i < this->_len; i++) ::new ((void*)&newData[i]) E(this->_data[i]);
  for (     ; i < this->_max; i++) ::new ((void*)&newData[i]) E();
  for (i = 0; i < old_max; i++) this->_data[i].~E();
  if (this->_data != NULL) {
    static_cast<Derived*>(this)->deallocate(this->_data);
  }
  this->_data = newData;
}

template <typename E, typename Derived>
void GrowableArrayWithAllocator<E, Derived>::clear_and_deallocate() {
  if (this->_data != NULL) {
    for (int i = 0; i < this->_max; i++) {
      this->_data[i].~E();
    }
    static_cast<Derived*>(this)->deallocate(this->_data);
    this->_data = NULL;
  }
  this->_len = 0;
  this->_max = 0;
}

class GrowableArrayResourceAllocator {
public:
  static void* allocate(int max, int element_size);
};

// Arena allocator
class GrowableArrayArenaAllocator {
public:
  static void* allocate(int max, int element_size, Arena* arena);
};

// CHeap allocator
class GrowableArrayCHeapAllocator {
public:
  static void* allocate(int max, int element_size, MEMFLAGS memflags);
  static void deallocate(void* mem);
};

#ifdef ASSERT

// Checks resource allocation nesting
class GrowableArrayNestingCheck {
  // resource area nesting at creation
  int _nesting;

public:
  GrowableArrayNestingCheck(bool on_stack);

  void on_stack_alloc() const;
};

#endif // ASSERT

// Encodes where the backing array is allocated
// and performs necessary checks.
class GrowableArrayMetadata {
  uintptr_t _bits;

  // resource area nesting at creation
  debug_only(GrowableArrayNestingCheck _nesting_check;)

  uintptr_t bits(MEMFLAGS memflags) const {
    if (memflags == mtNone) {
      // Stack allocation
      return 0;
    }

    // CHeap allocation
    return (uintptr_t(memflags) << 1) | 1;
  }

  uintptr_t bits(Arena* arena) const {
    return uintptr_t(arena);
  }

public:
  GrowableArrayMetadata(Arena* arena) :
      _bits(bits(arena))
      debug_only(COMMA _nesting_check(on_stack())) {
  }

  GrowableArrayMetadata(MEMFLAGS memflags) :
      _bits(bits(memflags))
      debug_only(COMMA _nesting_check(on_stack())) {
  }

#ifdef ASSERT
  GrowableArrayMetadata(const GrowableArrayMetadata& other) :
      _bits(other._bits),
      _nesting_check(other._nesting_check) {
    assert(!on_C_heap(), "Copying of CHeap arrays not supported");
    assert(!other.on_C_heap(), "Copying of CHeap arrays not supported");
  }

  GrowableArrayMetadata& operator=(const GrowableArrayMetadata& other) {
    _bits = other._bits;
    _nesting_check = other._nesting_check;
    assert(!on_C_heap(), "Assignment of CHeap arrays not supported");
    assert(!other.on_C_heap(), "Assignment of CHeap arrays not supported");
    return *this;
  }

  void init_checks(const GrowableArrayBase* array) const;
  void on_stack_alloc_check() const;
#endif // ASSERT

  bool on_C_heap() const { return (_bits & 1) == 1; }
  bool on_stack () const { return _bits == 0;      }
  bool on_arena () const { return (_bits & 1) == 0 && _bits != 0; }

  Arena* arena() const      { return (Arena*)_bits; }
  MEMFLAGS memflags() const { return MEMFLAGS(_bits >> 1); }
};

// THE GrowableArray.
//
// Supports multiple allocation strategies:
//  - Resource stack allocation: if memflags == mtNone
//  - CHeap allocation: if memflags != mtNone
//  - Arena allocation: if an arena is provided
//
// There are some drawbacks of using GrowableArray, that are removed in some
// of the other implementations of GrowableArrayWithAllocator sub-classes:
//
// Memory overhead: The multiple allocation strategies uses extra metadata
//  embedded in the instance.
//
// Strict allocation locations: There are rules about where the GrowableArray
//  instance is allocated, that depends on where the data array is allocated.
//  See: init_checks.

template <typename E>
class GrowableArray : public GrowableArrayWithAllocator<E, GrowableArray<E> > {
  friend class GrowableArrayWithAllocator<E, GrowableArray<E> >;
  friend class GrowableArrayTest;

  static E* allocate(int max) {
    return (E*)GrowableArrayResourceAllocator::allocate(max, sizeof(E));
  }

  static E* allocate(int max, MEMFLAGS memflags) {
    if (memflags != mtNone) {
      return (E*)GrowableArrayCHeapAllocator::allocate(max, sizeof(E), memflags);
    }

    return (E*)GrowableArrayResourceAllocator::allocate(max, sizeof(E));
  }

  static E* allocate(int max, Arena* arena) {
    return (E*)GrowableArrayArenaAllocator::allocate(max, sizeof(E), arena);
  }

  GrowableArrayMetadata _metadata;

  void init_checks() const { debug_only(_metadata.init_checks(this);) }

  // Where are we going to allocate memory?
  bool on_C_heap() const { return _metadata.on_C_heap(); }
  bool on_stack () const { return _metadata.on_stack(); }
  bool on_arena () const { return _metadata.on_arena(); }

  E* allocate() {
    if (on_stack()) {
      debug_only(_metadata.on_stack_alloc_check());
      return allocate(this->_max);
    }

    if (on_C_heap()) {
      return allocate(this->_max, _metadata.memflags());
    }

    assert(on_arena(), "Sanity");
    return allocate(this->_max, _metadata.arena());
  }

  void deallocate(E* mem) {
    if (on_C_heap()) {
      GrowableArrayCHeapAllocator::deallocate(mem);
    }
  }

public:
  GrowableArray(int initial_max = 2, MEMFLAGS memflags = mtNone) :
      GrowableArrayWithAllocator<E, GrowableArray<E> >(
          allocate(initial_max, memflags),
          initial_max),
      _metadata(memflags) {
    init_checks();
  }

  GrowableArray(int initial_max, int initial_len, const E& filler, MEMFLAGS memflags = mtNone) :
      GrowableArrayWithAllocator<E, GrowableArray<E> >(
          allocate(initial_max, memflags),
          initial_max, initial_len, filler),
      _metadata(memflags) {
    init_checks();
  }

  GrowableArray(Arena* arena, int initial_max, int initial_len, const E& filler) :
      GrowableArrayWithAllocator<E, GrowableArray<E> >(
          allocate(initial_max, arena),
          initial_max, initial_len, filler),
      _metadata(arena) {
    init_checks();
  }

  ~GrowableArray() {
    if (on_C_heap()) {
      this->clear_and_deallocate();
    }
  }
};

// Leaner GrowableArray for CHeap backed data arrays, with compile-time decided MEMFLAGS.
template <typename E, MEMFLAGS F>
class GrowableArrayCHeap : public GrowableArrayWithAllocator<E, GrowableArrayCHeap<E, F> > {
  friend class GrowableArrayWithAllocator<E, GrowableArrayCHeap<E, F> >;

  STATIC_ASSERT(F != mtNone);

  static E* allocate(int max, MEMFLAGS flags) {
    if (max == 0) {
      return NULL;
    }

    return (E*)GrowableArrayCHeapAllocator::allocate(max, sizeof(E), flags);
  }

  NONCOPYABLE(GrowableArrayCHeap);

  E* allocate() {
    return allocate(this->_max, F);
  }

  void deallocate(E* mem) {
    GrowableArrayCHeapAllocator::deallocate(mem);
  }

public:
  GrowableArrayCHeap(int initial_max = 0) :
      GrowableArrayWithAllocator<E, GrowableArrayCHeap<E, F> >(
          allocate(initial_max, F),
          initial_max) {}

  GrowableArrayCHeap(int initial_max, int initial_len, const E& filler) :
      GrowableArrayWithAllocator<E, GrowableArrayCHeap<E, F> >(
          allocate(initial_max, F),
          initial_max, initial_len, filler) {}

  ~GrowableArrayCHeap() {
    this->clear_and_deallocate();
  }

  void* operator new(size_t size) throw() {
    return ResourceObj::operator new(size, ResourceObj::C_HEAP, F);
  }

  void* operator new(size_t size, const std::nothrow_t&  nothrow_constant) throw() {
    return ResourceObj::operator new(size, nothrow_constant, ResourceObj::C_HEAP, F);
  }
};

// Custom STL-style iterator to iterate over GrowableArrays
// It is constructed by invoking GrowableArray::begin() and GrowableArray::end()
template <typename E>
class GrowableArrayIterator : public StackObj {
  friend class GrowableArrayView<E>;
  template <typename F, typename UnaryPredicate> friend class GrowableArrayFilterIterator;

 private:
  const GrowableArrayView<E>* _array; // GrowableArray we iterate over
  int _position;                      // The current position in the GrowableArray

  // Private constructor used in GrowableArray::begin() and GrowableArray::end()
  GrowableArrayIterator(const GrowableArrayView<E>* array, int position) : _array(array), _position(position) {
    assert(0 <= position && position <= _array->length(), "illegal position");
  }

 public:
  GrowableArrayIterator() : _array(NULL), _position(0) { }
  GrowableArrayIterator<E>& operator++() { ++_position; return *this; }
  E operator*()                          { return _array->at(_position); }

  bool operator==(const GrowableArrayIterator<E>& rhs)  {
    assert(_array == rhs._array, "iterator belongs to different array");
    return _position == rhs._position;
  }

  bool operator!=(const GrowableArrayIterator<E>& rhs)  {
    assert(_array == rhs._array, "iterator belongs to different array");
    return _position != rhs._position;
  }
};

// Custom STL-style iterator to iterate over elements of a GrowableArray that satisfy a given predicate
template <typename E, class UnaryPredicate>
class GrowableArrayFilterIterator : public StackObj {
  friend class GrowableArrayView<E>;

 private:
  const GrowableArrayView<E>* _array; // GrowableArray we iterate over
  int _position;                      // Current position in the GrowableArray
  UnaryPredicate _predicate;          // Unary predicate the elements of the GrowableArray should satisfy

 public:
  GrowableArrayFilterIterator(const GrowableArrayIterator<E>& begin, UnaryPredicate filter_predicate) :
      _array(begin._array), _position(begin._position), _predicate(filter_predicate) {
    // Advance to first element satisfying the predicate
    while(_position != _array->length() && !_predicate(_array->at(_position))) {
      ++_position;
    }
  }

  GrowableArrayFilterIterator<E, UnaryPredicate>& operator++() {
    do {
      // Advance to next element satisfying the predicate
      ++_position;
    } while(_position != _array->length() && !_predicate(_array->at(_position)));
    return *this;
  }

  E operator*() { return _array->at(_position); }

  bool operator==(const GrowableArrayIterator<E>& rhs)  {
    assert(_array == rhs._array, "iterator belongs to different array");
    return _position == rhs._position;
  }

  bool operator!=(const GrowableArrayIterator<E>& rhs)  {
    assert(_array == rhs._array, "iterator belongs to different array");
    return _position != rhs._position;
  }

  bool operator==(const GrowableArrayFilterIterator<E, UnaryPredicate>& rhs)  {
    assert(_array == rhs._array, "iterator belongs to different array");
    return _position == rhs._position;
  }

  bool operator!=(const GrowableArrayFilterIterator<E, UnaryPredicate>& rhs)  {
    assert(_array == rhs._array, "iterator belongs to different array");
    return _position != rhs._position;
  }
};

// Arrays for basic types
typedef GrowableArray<int> intArray;
typedef GrowableArray<int> intStack;
typedef GrowableArray<bool> boolArray;

#endif // SHARE_UTILITIES_GROWABLEARRAY_HPP
