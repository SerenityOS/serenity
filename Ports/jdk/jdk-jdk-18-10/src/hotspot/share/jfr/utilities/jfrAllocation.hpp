/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRALLOCATION_HPP
#define SHARE_JFR_UTILITIES_JFRALLOCATION_HPP

#include "memory/allocation.hpp"
#include "utilities/exceptions.hpp"

/*
 * A subclass to the CHeapObj<mtTracing> allocator, useful for critical
 * Jfr subsystems. Critical in this context means subsystems for which
 * allocations are crucial to the bootstrap and initialization of Jfr.
 * The default behaviour by a CHeapObj is to call vm_exit_out_of_memory()
 * on allocation failure and this is problematic in combination with the
 * Jfr on-demand, dynamic start at runtime, capability.
 * We would not like a user dynamically starting Jfr to
 * tear down the VM she is about to inspect as a side effect.
 *
 * This allocator uses the RETURN_NULL capabilities
 * instead of calling vm_exit_out_of_memory() until Jfr is properly started.
 * This allows for controlled behaviour on allocation failures during startup,
 * which means we can take actions on failure, such as transactional rollback
 * (deallocations and restorations).
 * In addition, this allocator allows for easy hooking of memory
 * allocations / deallocations for debugging purposes.
 */

class JfrCHeapObj : public CHeapObj<mtTracing> {
 private:
  static void on_memory_allocation(const void* allocation, size_t size);
  static char* allocate_array_noinline(size_t elements, size_t element_size);

 public:
  NOINLINE void* operator new(size_t size) throw();
  NOINLINE void* operator new (size_t size, const std::nothrow_t&  nothrow_constant) throw();
  NOINLINE void* operator new [](size_t size) throw();
  NOINLINE void* operator new [](size_t size, const std::nothrow_t&  nothrow_constant) throw();
  void  operator delete(void* p, size_t size);
  void  operator delete [] (void* p, size_t size);
  static char* realloc_array(char* old, size_t size);
  static void free(void* p, size_t size = 0);

  template <class T>
  static T* new_array(size_t size) {
    T* const memory = (T*)allocate_array_noinline(size, sizeof(T));
    on_memory_allocation(memory, sizeof(T) * size);
    return memory;
  }
};

#endif // SHARE_JFR_UTILITIES_JFRALLOCATION_HPP
