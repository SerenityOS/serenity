/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_MEMORY_GUARDEDMEMORY_HPP
#define SHARE_MEMORY_GUARDEDMEMORY_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

/**
 * Guarded memory for detecting buffer overrun.
 *
 * Allows allocations to be wrapped with padded bytes of a known byte pattern,
 * that is a "guard". Guard patterns may be verified to detect buffer overruns.
 *
 * Primarily used by "debug malloc" and "checked JNI".
 *
 * Memory layout:
 *
 * |Offset             | Content              | Description    |
 * |------------------------------------------------------------
 * |base_addr          | 0xABABABABABABABAB   | Head guard     |
 * |+16                | <size_t:user_size>   | User data size |
 * |+sizeof(uintptr_t) | <tag>                | Tag word       |
 * |+sizeof(void*)     | 0xF1 <user_data> (   | User data      |
 * |+user_size         | 0xABABABABABABABAB   | Tail guard     |
 * -------------------------------------------------------------
 *
 * Where:
 *  - guard padding uses "badResourceValue" (0xAB)
 *  - tag word is general purpose
 *  - user data
 *    -- initially padded with "uninitBlockPad" (0xF1),
 *    -- to "freeBlockPad" (0xBA), when freed
 *
 * Usage:
 *
 * * Allocations: one may wrap allocations with guard memory:
 * <code>
 *   Thing* alloc_thing() {
 *     void* mem = user_alloc_fn(GuardedMemory::get_total_size(sizeof(thing)));
 *     GuardedMemory guarded(mem, sizeof(thing));
 *     return (Thing*) guarded.get_user_ptr();
 *   }
 * </code>
 * * Verify: memory guards are still in tact
 * <code>
 *   bool verify_thing(Thing* thing) {
 *     GuardedMemory guarded((void*)thing);
 *     return guarded.verify_guards();
 *   }
 * </code>
 * * Free: one may mark bytes as freed (further debugging support)
 * <code>
 *   void free_thing(Thing* thing) {
 *    GuardedMemory guarded((void*)thing);
 *    assert(guarded.verify_guards(), "Corrupt thing");
 *    user_free_fn(guards.release_for_freeing();
 *   }
 * </code>
 */
class GuardedMemory : StackObj { // Wrapper on stack

  friend class GuardedMemoryTest;
  // Private inner classes for memory layout...

protected:

  /**
   * Guard class for header and trailer known pattern to test for overwrites.
   */
  class Guard { // Class for raw memory (no vtbl allowed)
    friend class GuardedMemory;
   protected:
    enum {
      GUARD_SIZE = 16
    };

    u_char _guard[GUARD_SIZE];

   public:

    void build() {
      u_char* c = _guard; // Possibly unaligned if tail guard
      u_char* end = c + GUARD_SIZE;
      while (c < end) {
        *c = badResourceValue;
        c++;
      }
    }

    bool verify() const {
      u_char* c = (u_char*) _guard;
      u_char* end = c + GUARD_SIZE;
      while (c < end) {
        if (*c != badResourceValue) {
          return false;
        }
        c++;
      }
      return true;
    }

  }; // GuardedMemory::Guard

  /**
   * Header guard and size
   */
  class GuardHeader : Guard {
    friend class GuardedMemory;
   protected:
    // Take care in modifying fields here, will effect alignment
    // e.g. x86 ABI 16 byte stack alignment
    union {
      uintptr_t __unused_full_word1;
      size_t _user_size;
    };
    void* _tag;
   public:
    void set_user_size(const size_t usz) { _user_size = usz; }
    size_t get_user_size() const { return _user_size; }

    void set_tag(const void* tag) { _tag = (void*) tag; }
    void* get_tag() const { return _tag; }

  }; // GuardedMemory::GuardHeader

  // Guarded Memory...

 protected:
  u_char* _base_addr;

 public:

  /**
   * Create new guarded memory.
   *
   * Wraps, starting at the given "base_ptr" with guards. Use "get_user_ptr()"
   * to return a pointer suitable for user data.
   *
   * @param base_ptr  allocation wishing to be wrapped, must be at least "GuardedMemory::get_total_size()" bytes.
   * @param user_size the size of the user data to be wrapped.
   * @param tag       optional general purpose tag.
   */
  GuardedMemory(void* base_ptr, const size_t user_size, const void* tag = NULL) {
    wrap_with_guards(base_ptr, user_size, tag);
  }

  /**
   * Wrap existing guarded memory.
   *
   * To use this constructor, one must have created guarded memory with
   * "GuardedMemory(void*, size_t, void*)" (or indirectly via helper, e.g. "wrap_copy()").
   *
   * @param user_p  existing wrapped memory.
   */
  GuardedMemory(void* userp) {
    u_char* user_ptr = (u_char*) userp;
    assert((uintptr_t)user_ptr > (sizeof(GuardHeader) + 0x1000), "Invalid pointer");
    _base_addr = (user_ptr - sizeof(GuardHeader));
  }

  /**
   * Create new guarded memory.
   *
   * Wraps, starting at the given "base_ptr" with guards. Allows reuse of stack allocated helper.
   *
   * @param base_ptr  allocation wishing to be wrapped, must be at least "GuardedMemory::get_total_size()" bytes.
   * @param user_size the size of the user data to be wrapped.
   * @param tag       optional general purpose tag.
   *
   * @return user data pointer (inner pointer to supplied "base_ptr").
   */
  void* wrap_with_guards(void* base_ptr, size_t user_size, const void* tag = NULL) {
    assert(base_ptr != NULL, "Attempt to wrap NULL with memory guard");
    _base_addr = (u_char*)base_ptr;
    get_head_guard()->build();
    get_head_guard()->set_user_size(user_size);
    get_tail_guard()->build();
    set_tag(tag);
    set_user_bytes(uninitBlockPad);
    assert(verify_guards(), "Expected valid memory guards");
    return get_user_ptr();
  }

  /**
   * Verify head and tail guards.
   *
   * @return true if guards are intact, false would indicate a buffer overrun.
   */
  bool verify_guards() const {
    if (_base_addr != NULL) {
      return (get_head_guard()->verify() && get_tail_guard()->verify());
    }
    return false;
  }

  /**
   * Set the general purpose tag.
   *
   * @param tag general purpose tag.
   */
  void set_tag(const void* tag) { get_head_guard()->set_tag(tag); }

  /**
   * Return the general purpose tag.
   *
   * @return the general purpose tag, defaults to NULL.
   */
  void* get_tag() const { return get_head_guard()->get_tag(); }

  /**
   * Return the size of the user data.
   *
   * @return the size of the user data.
   */
  size_t get_user_size() const {
    assert(_base_addr != NULL, "Not wrapping any memory");
    return get_head_guard()->get_user_size();
  }

  /**
   * Return the user data pointer.
   *
   * @return the user data pointer.
   */
  u_char* get_user_ptr() const {
    assert(_base_addr != NULL, "Not wrapping any memory");
    return _base_addr + sizeof(GuardHeader);
  }

  /**
   * Release the wrapped pointer for resource freeing.
   *
   * Pads the user data with "freeBlockPad", and dis-associates the helper.
   *
   * @return the original base pointer used to wrap the data.
   */
  void* release_for_freeing() {
    set_user_bytes(freeBlockPad);
    return release();
  }

  /**
   * Dis-associate the help from the original base address.
   *
   * @return the original base pointer used to wrap the data.
   */
  void* release() {
    void* p = (void*) _base_addr;
    _base_addr = NULL;
    return p;
  }

  virtual void print_on(outputStream* st) const;

 protected:
  GuardHeader*  get_head_guard() const { return (GuardHeader*) _base_addr; }
  Guard*        get_tail_guard() const { return (Guard*) (get_user_ptr() + get_user_size()); };
  void set_user_bytes(u_char ch) {
    memset(get_user_ptr(), ch, get_user_size());
  }

 public:
  /**
   * Return the total size required for wrapping the given user size.
   *
   * @return the total size required for wrapping the given user size.
   */
  static size_t get_total_size(size_t user_size) {
    size_t total_size = sizeof(GuardHeader) + user_size + sizeof(Guard);
    assert(total_size > user_size, "Unexpected wrap-around");
    return total_size;
  }

  // Helper functions...

  /**
   * Wrap a copy of size "len" of "ptr".
   *
   * @param ptr the memory to be copied
   * @param len the length of the copy
   * @param tag optional general purpose tag (see GuardedMemory::get_tag())
   *
   * @return guarded wrapped memory pointer to the user area, or NULL if OOM.
   */
  static void* wrap_copy(const void* p, const size_t len, const void* tag = NULL);

  /**
   * Free wrapped copy.
   *
   * Frees memory copied with "wrap_copy()".
   *
   * @param p memory returned by "wrap_copy()".
   *
   * @return true if guards were verified as intact. false indicates a buffer overrun.
   */
  static bool free_copy(void* p);

}; // GuardedMemory

#endif // SHARE_MEMORY_GUARDEDMEMORY_HPP
