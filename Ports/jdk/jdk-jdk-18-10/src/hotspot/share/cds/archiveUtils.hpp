/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CDS_ARCHIVEUTILS_HPP
#define SHARE_CDS_ARCHIVEUTILS_HPP

#include "logging/log.hpp"
#include "memory/iterator.hpp"
#include "memory/virtualspace.hpp"
#include "utilities/bitMap.hpp"
#include "utilities/exceptions.hpp"

class BootstrapInfo;
class ReservedSpace;
class VirtualSpace;

// ArchivePtrMarker is used to mark the location of pointers embedded in a CDS archive. E.g., when an
// InstanceKlass k is dumped, we mark the location of the k->_name pointer by effectively calling
// mark_pointer(/*ptr_loc=*/&k->_name). It's required that (_prt_base <= ptr_loc < _ptr_end). _ptr_base is
// fixed, but _ptr_end can be expanded as more objects are dumped.
class ArchivePtrMarker : AllStatic {
  static CHeapBitMap*  _ptrmap;
  static VirtualSpace* _vs;

  // Once _ptrmap is compacted, we don't allow bit marking anymore. This is to
  // avoid unintentional copy operations after the bitmap has been finalized and written.
  static bool         _compacted;

  static address* ptr_base() { return (address*)_vs->low();  } // committed lower bound (inclusive)
  static address* ptr_end()  { return (address*)_vs->high(); } // committed upper bound (exclusive)

public:
  static void initialize(CHeapBitMap* ptrmap, VirtualSpace* vs);
  static void mark_pointer(address* ptr_loc);
  static void clear_pointer(address* ptr_loc);
  static void compact(address relocatable_base, address relocatable_end);
  static void compact(size_t max_non_null_offset);

  template <typename T>
  static void mark_pointer(T* ptr_loc) {
    mark_pointer((address*)ptr_loc);
  }

  template <typename T>
  static void set_and_mark_pointer(T* ptr_loc, T ptr_value) {
    *ptr_loc = ptr_value;
    mark_pointer(ptr_loc);
  }

  static CHeapBitMap* ptrmap() {
    return _ptrmap;
  }

  static void reset_map_and_vs() {
    _ptrmap = nullptr;
    _vs = nullptr;
  }
};

// SharedDataRelocator is used to shift pointers in the CDS archive.
//
// The CDS archive is basically a contiguous block of memory (divided into several regions)
// that contains multiple objects. The objects may contain direct pointers that point to other objects
// within the archive (e.g., InstanceKlass::_name points to a Symbol in the archive). During dumping, we
// built a bitmap that marks the locations of all these pointers (using ArchivePtrMarker, see comments above).
//
// The contents of the archive assumes that it’s mapped at the default SharedBaseAddress (e.g. 0x800000000).
// If the archive ends up being mapped at a different address (e.g. 0x810000000), SharedDataRelocator
// is used to shift each marked pointer by a delta (0x10000000 in this example), so that it points to
// the actually mapped location of the target object.
class SharedDataRelocator: public BitMapClosure {
  // for all (address** p), where (is_marked(p) && _patch_base <= p && p < _patch_end) { *p += delta; }

  // Patch all pointers within this region that are marked.
  address* _patch_base;
  address* _patch_end;

  // Before patching, all pointers must point to this region.
  address _valid_old_base;
  address _valid_old_end;

  // After patching, all pointers must point to this region.
  address _valid_new_base;
  address _valid_new_end;

  // How much to relocate for each pointer.
  intx _delta;

 public:
  SharedDataRelocator(address* patch_base, address* patch_end,
                      address valid_old_base, address valid_old_end,
                      address valid_new_base, address valid_new_end, intx delta) :
    _patch_base(patch_base), _patch_end(patch_end),
    _valid_old_base(valid_old_base), _valid_old_end(valid_old_end),
    _valid_new_base(valid_new_base), _valid_new_end(valid_new_end),
    _delta(delta) {
    log_debug(cds, reloc)("SharedDataRelocator::_patch_base     = " PTR_FORMAT, p2i(_patch_base));
    log_debug(cds, reloc)("SharedDataRelocator::_patch_end      = " PTR_FORMAT, p2i(_patch_end));
    log_debug(cds, reloc)("SharedDataRelocator::_valid_old_base = " PTR_FORMAT, p2i(_valid_old_base));
    log_debug(cds, reloc)("SharedDataRelocator::_valid_old_end  = " PTR_FORMAT, p2i(_valid_old_end));
    log_debug(cds, reloc)("SharedDataRelocator::_valid_new_base = " PTR_FORMAT, p2i(_valid_new_base));
    log_debug(cds, reloc)("SharedDataRelocator::_valid_new_end  = " PTR_FORMAT, p2i(_valid_new_end));
  }

  bool do_bit(size_t offset);
};

class DumpRegion {
private:
  const char* _name;
  char* _base;
  char* _top;
  char* _end;
  uintx _max_delta;
  bool _is_packed;
  ReservedSpace* _rs;
  VirtualSpace* _vs;

  void commit_to(char* newtop);

public:
  DumpRegion(const char* name, uintx max_delta = 0)
    : _name(name), _base(NULL), _top(NULL), _end(NULL),
      _max_delta(max_delta), _is_packed(false) {}

  char* expand_top_to(char* newtop);
  char* allocate(size_t num_bytes);

  void append_intptr_t(intptr_t n, bool need_to_mark = false);

  char* base()      const { return _base;        }
  char* top()       const { return _top;         }
  char* end()       const { return _end;         }
  size_t reserved() const { return _end - _base; }
  size_t used()     const { return _top - _base; }
  bool is_packed()  const { return _is_packed;   }
  bool is_allocatable() const {
    return !is_packed() && _base != NULL;
  }

  void print(size_t total_bytes) const;
  void print_out_of_space_msg(const char* failing_region, size_t needed_bytes);

  void init(ReservedSpace* rs, VirtualSpace* vs);

  void pack(DumpRegion* next = NULL);

  bool contains(char* p) {
    return base() <= p && p < top();
  }
};

// Closure for serializing initialization data out to a data area to be
// written to the shared file.

class WriteClosure : public SerializeClosure {
private:
  DumpRegion* _dump_region;

public:
  WriteClosure(DumpRegion* r) {
    _dump_region = r;
  }

  void do_ptr(void** p) {
    _dump_region->append_intptr_t((intptr_t)*p, true);
  }

  void do_u4(u4* p) {
    _dump_region->append_intptr_t((intptr_t)(*p));
  }

  void do_bool(bool *p) {
    _dump_region->append_intptr_t((intptr_t)(*p));
  }

  void do_tag(int tag) {
    _dump_region->append_intptr_t((intptr_t)tag);
  }

  void do_oop(oop* o);
  void do_region(u_char* start, size_t size);
  bool reading() const { return false; }
};

// Closure for serializing initialization data in from a data area
// (ptr_array) read from the shared file.

class ReadClosure : public SerializeClosure {
private:
  intptr_t** _ptr_array;

  inline intptr_t nextPtr() {
    return *(*_ptr_array)++;
  }

public:
  ReadClosure(intptr_t** ptr_array) { _ptr_array = ptr_array; }

  void do_ptr(void** p);
  void do_u4(u4* p);
  void do_bool(bool *p);
  void do_tag(int tag);
  void do_oop(oop *p);
  void do_region(u_char* start, size_t size);
  bool reading() const { return true; }
};

class ArchiveUtils {
public:
  static void log_to_classlist(BootstrapInfo* bootstrap_specifier, TRAPS) NOT_CDS_RETURN;
};

#endif // SHARE_CDS_ARCHIVEUTILS_HPP
