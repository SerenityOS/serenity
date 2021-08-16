/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_MEMORY_HEAPINSPECTION_HPP
#define SHARE_MEMORY_HEAPINSPECTION_HPP

#include "memory/allocation.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.hpp"
#include "oops/annotations.hpp"
#include "utilities/macros.hpp"
#include "gc/shared/workgroup.hpp"

class ParallelObjectIterator;

#if INCLUDE_SERVICES


// HeapInspection

// KlassInfoTable is a bucket hash table that
// maps Klass*s to extra information:
//    instance count and instance word size.
//
// A KlassInfoBucket is the head of a link list
// of KlassInfoEntry's
//
// KlassInfoHisto is a growable array of pointers
// to KlassInfoEntry's and is used to sort
// the entries.

class KlassInfoEntry: public CHeapObj<mtInternal> {
 private:
  KlassInfoEntry* _next;
  Klass*          _klass;
  uint64_t        _instance_count;
  size_t          _instance_words;
  int64_t         _index;
  bool            _do_print; // True if we should print this class when printing the class hierarchy.
  GrowableArray<KlassInfoEntry*>* _subclasses;

 public:
  KlassInfoEntry(Klass* k, KlassInfoEntry* next) :
    _next(next), _klass(k), _instance_count(0), _instance_words(0), _index(-1),
    _do_print(false), _subclasses(NULL)
  {}
  ~KlassInfoEntry();
  KlassInfoEntry* next() const   { return _next; }
  bool is_equal(const Klass* k)  { return k == _klass; }
  Klass* klass()  const          { return _klass; }
  uint64_t count()    const      { return _instance_count; }
  void set_count(uint64_t ct)    { _instance_count = ct; }
  size_t words()  const          { return _instance_words; }
  void set_words(size_t wds)     { _instance_words = wds; }
  void set_index(int64_t index)  { _index = index; }
  int64_t index()    const       { return _index; }
  GrowableArray<KlassInfoEntry*>* subclasses() const { return _subclasses; }
  void add_subclass(KlassInfoEntry* cie);
  void set_do_print(bool do_print) { _do_print = do_print; }
  bool do_print() const      { return _do_print; }
  int compare(KlassInfoEntry* e1, KlassInfoEntry* e2);
  void print_on(outputStream* st) const;
  const char* name() const;
};

class KlassInfoClosure : public StackObj {
 public:
  // Called for each KlassInfoEntry.
  virtual void do_cinfo(KlassInfoEntry* cie) = 0;
};

class KlassInfoBucket: public CHeapObj<mtInternal> {
 private:
  KlassInfoEntry* _list;
  KlassInfoEntry* list()           { return _list; }
  void set_list(KlassInfoEntry* l) { _list = l; }
 public:
  KlassInfoEntry* lookup(Klass* k);
  void initialize() { _list = NULL; }
  void empty();
  void iterate(KlassInfoClosure* cic);
};

class KlassInfoTable: public StackObj {
 private:
  static const int _num_buckets = 20011;
  size_t _size_of_instances_in_words;

  // An aligned reference address (typically the least
  // address in the perm gen) used for hashing klass
  // objects.
  HeapWord* _ref;

  KlassInfoBucket* _buckets;
  uint hash(const Klass* p);
  KlassInfoEntry* lookup(Klass* k); // allocates if not found!

  class AllClassesFinder;

 public:
  KlassInfoTable(bool add_all_classes);
  ~KlassInfoTable();
  bool record_instance(const oop obj);
  void iterate(KlassInfoClosure* cic);
  bool allocation_failed() { return _buckets == NULL; }
  size_t size_of_instances_in_words() const;
  bool merge(KlassInfoTable* table);
  bool merge_entry(const KlassInfoEntry* cie);

  friend class KlassInfoHisto;
  friend class KlassHierarchy;
};

class KlassHierarchy : AllStatic {
 public:
  static void print_class_hierarchy(outputStream* st, bool print_interfaces,  bool print_subclasses,
                                    char* classname);

 private:
  static void set_do_print_for_class_hierarchy(KlassInfoEntry* cie, KlassInfoTable* cit,
                                               bool print_subclasse);
  static void print_class(outputStream* st, KlassInfoEntry* cie, bool print_subclasses);
};

class KlassInfoHisto : public StackObj {
 private:
  static const int _histo_initial_size = 1000;
  KlassInfoTable *_cit;
  GrowableArray<KlassInfoEntry*>* _elements;
  GrowableArray<KlassInfoEntry*>* elements() const { return _elements; }
  static int sort_helper(KlassInfoEntry** e1, KlassInfoEntry** e2);
  void print_elements(outputStream* st) const;
  bool is_selected(const char *col_name);

  template <class T> static int count_bytes(T* x) {
    return (HeapWordSize * ((x) ? (x)->size() : 0));
  }

  template <class T> static int count_bytes_array(T* x) {
    if (x == NULL) {
      return 0;
    }
    if (x->length() == 0) {
      // This is a shared array, e.g., Universe::the_empty_int_array(). Don't
      // count it to avoid double-counting.
      return 0;
    }
    return HeapWordSize * x->size();
  }

  static void print_julong(outputStream* st, int width, julong n) {
    int num_spaces = width - julong_width(n);
    if (num_spaces > 0) {
      st->print("%*s", num_spaces, "");
    }
    st->print(JULONG_FORMAT, n);
  }

  static int julong_width(julong n) {
    if (n == 0) {
      return 1;
    }
    int w = 0;
    while (n > 0) {
      n /= 10;
      w += 1;
    }
    return w;
  }

  static int col_width(julong n, const char *name) {
    int w = julong_width(n);
    int min = (int)(strlen(name));
    if (w < min) {
        w = min;
    }
    // add a leading space for separation.
    return w + 1;
  }

 public:
  KlassInfoHisto(KlassInfoTable* cit);
  ~KlassInfoHisto();
  void add(KlassInfoEntry* cie);
  void print_histo_on(outputStream* st);
  void sort();
};

#endif // INCLUDE_SERVICES

// These declarations are needed since the declaration of KlassInfoTable and
// KlassInfoClosure are guarded by #if INLCUDE_SERVICES
class KlassInfoTable;
class KlassInfoClosure;

class HeapInspection : public StackObj {
 public:
  void heap_inspection(outputStream* st, uint parallel_thread_num = 1) NOT_SERVICES_RETURN;
  uintx populate_table(KlassInfoTable* cit, BoolObjectClosure* filter = NULL, uint parallel_thread_num = 1) NOT_SERVICES_RETURN_(0);
  static void find_instances_at_safepoint(Klass* k, GrowableArray<oop>* result) NOT_SERVICES_RETURN;
 private:
  void iterate_over_heap(KlassInfoTable* cit, BoolObjectClosure* filter = NULL);
};

// Parallel heap inspection task. Parallel inspection can fail due to
// a native OOM when allocating memory for TL-KlassInfoTable.
// _success will be set false on an OOM, and serial inspection tried.
class ParHeapInspectTask : public AbstractGangTask {
 private:
  ParallelObjectIterator* _poi;
  KlassInfoTable* _shared_cit;
  BoolObjectClosure* _filter;
  uintx _missed_count;
  bool _success;
  Mutex _mutex;

 public:
  ParHeapInspectTask(ParallelObjectIterator* poi,
                     KlassInfoTable* shared_cit,
                     BoolObjectClosure* filter) :
      AbstractGangTask("Iterating heap"),
      _poi(poi),
      _shared_cit(shared_cit),
      _filter(filter),
      _missed_count(0),
      _success(true),
      _mutex(Mutex::leaf, "Parallel heap iteration data merge lock") {}

  uintx missed_count() const {
    return _missed_count;
  }

  bool success() {
    return _success;
  }

  virtual void work(uint worker_id);
};

#endif // SHARE_MEMORY_HEAPINSPECTION_HPP
