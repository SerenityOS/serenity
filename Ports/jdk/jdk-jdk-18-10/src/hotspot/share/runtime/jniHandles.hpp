/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_JNIHANDLES_HPP
#define SHARE_RUNTIME_JNIHANDLES_HPP

#include "memory/allocation.hpp"
#include "runtime/handles.hpp"

class JavaThread;
class OopStorage;
class Thread;

// Interface for creating and resolving local/global JNI handles

class JNIHandles : AllStatic {
  friend class VMStructs;
 private:
  // These are used by the serviceability agent.
  static OopStorage* _global_handles;
  static OopStorage* _weak_global_handles;
  friend void jni_handles_init();

  static OopStorage* global_handles();
  static OopStorage* weak_global_handles();

  inline static bool is_jweak(jobject handle);
  inline static oop* jobject_ptr(jobject handle); // NOT jweak!
  inline static oop* jweak_ptr(jobject handle);

  template <DecoratorSet decorators, bool external_guard> inline static oop resolve_impl(jobject handle);

  // Resolve handle into oop, without keeping the object alive
  inline static oop resolve_no_keepalive(jobject handle);

  // This method is not inlined in order to avoid circular includes between
  // this header file and thread.hpp.
  static bool current_thread_in_native();

 public:
  // Low tag bit in jobject used to distinguish a jweak.  jweak is
  // type equivalent to jobject, but there are places where we need to
  // be able to distinguish jweak values from other jobjects, and
  // is_weak_global_handle is unsuitable for performance reasons.  To
  // provide such a test we add weak_tag_value to the (aligned) byte
  // address designated by the jobject to produce the corresponding
  // jweak.  Accessing the value of a jobject must account for it
  // being a possibly offset jweak.
  static const uintptr_t weak_tag_size = 1;
  static const uintptr_t weak_tag_alignment = (1u << weak_tag_size);
  static const uintptr_t weak_tag_mask = weak_tag_alignment - 1;
  static const int weak_tag_value = 1;

  // Resolve handle into oop
  inline static oop resolve(jobject handle);
  // Resolve handle into oop, result guaranteed not to be null
  inline static oop resolve_non_null(jobject handle);
  // Resolve externally provided handle into oop with some guards
  static oop resolve_external_guard(jobject handle);

  // Check for equality without keeping objects alive
  static bool is_same_object(jobject handle1, jobject handle2);

  // Local handles
  static jobject make_local(oop obj);
  static jobject make_local(Thread* thread, oop obj,  // Faster version when current thread is known
                            AllocFailType alloc_failmode = AllocFailStrategy::EXIT_OOM);
  inline static void destroy_local(jobject handle);

  // Global handles
  static jobject make_global(Handle  obj,
                             AllocFailType alloc_failmode = AllocFailStrategy::EXIT_OOM);
  static void destroy_global(jobject handle);

  // Weak global handles
  static jobject make_weak_global(Handle obj,
                                  AllocFailType alloc_failmode = AllocFailStrategy::EXIT_OOM);
  static void destroy_weak_global(jobject handle);
  static bool is_global_weak_cleared(jweak handle); // Test jweak without resolution

  // Debugging
  static void print_on(outputStream* st);
  static void print();
  static void verify();
  // The category predicates all require handle != NULL.
  static bool is_local_handle(Thread* thread, jobject handle);
  static bool is_frame_handle(JavaThread* thread, jobject handle);
  static bool is_global_handle(jobject handle);
  static bool is_weak_global_handle(jobject handle);
  static size_t global_handle_memory_usage();
  static size_t weak_global_handle_memory_usage();

#ifndef PRODUCT
  // Is handle from any local block of any thread?
  static bool is_local_handle(jobject handle);
#endif

  // precondition: handle != NULL.
  static jobjectRefType handle_type(Thread* thread, jobject handle);

  // Garbage collection support(global handles only, local handles are traversed from thread)
  // Traversal of regular global handles
  static void oops_do(OopClosure* f);
  // Traversal of weak global handles. Unreachable oops are cleared.
  static void weak_oops_do(BoolObjectClosure* is_alive, OopClosure* f);
  // Traversal of weak global handles.
  static void weak_oops_do(OopClosure* f);

  static bool is_global_storage(const OopStorage* storage);
};



// JNI handle blocks holding local/global JNI handles

class JNIHandleBlock : public CHeapObj<mtInternal> {
  friend class VMStructs;
  friend class ZeroInterpreter;

 private:
  enum SomeConstants {
    block_size_in_oops  = 32                    // Number of handles per handle block
  };

  uintptr_t       _handles[block_size_in_oops]; // The handles
  int             _top;                         // Index of next unused handle
  JNIHandleBlock* _next;                        // Link to next block

  // The following instance variables are only used by the first block in a chain.
  // Having two types of blocks complicates the code and the space overhead in negligible.
  JNIHandleBlock* _last;                        // Last block in use
  JNIHandleBlock* _pop_frame_link;              // Block to restore on PopLocalFrame call
  uintptr_t*      _free_list;                   // Handle free list
  int             _allocate_before_rebuild;     // Number of blocks to allocate before rebuilding free list

  // Check JNI, "planned capacity" for current frame (or push/ensure)
  size_t          _planned_capacity;

  #ifndef PRODUCT
  JNIHandleBlock* _block_list_link;             // Link for list below
  static JNIHandleBlock* _block_list;           // List of all allocated blocks (for debugging only)
  #endif

  static JNIHandleBlock* _block_free_list;      // Free list of currently unused blocks
  static int      _blocks_allocated;            // For debugging/printing

  // Fill block with bad_handle values
  void zap() NOT_DEBUG_RETURN;

  // Free list computation
  void rebuild_free_list();

  // No more handles in the both the current and following blocks
  void clear() { _top = 0; }

 public:
  // Handle allocation
  jobject allocate_handle(oop obj, AllocFailType alloc_failmode = AllocFailStrategy::EXIT_OOM);

  // Block allocation and block free list management
  static JNIHandleBlock* allocate_block(Thread* thread = NULL, AllocFailType alloc_failmode = AllocFailStrategy::EXIT_OOM);
  static void release_block(JNIHandleBlock* block, Thread* thread = NULL);

  // JNI PushLocalFrame/PopLocalFrame support
  JNIHandleBlock* pop_frame_link() const          { return _pop_frame_link; }
  void set_pop_frame_link(JNIHandleBlock* block)  { _pop_frame_link = block; }

  // Stub generator support
  static int top_offset_in_bytes()                { return offset_of(JNIHandleBlock, _top); }

  // Garbage collection support
  // Traversal of handles
  void oops_do(OopClosure* f);

  // Checked JNI support
  void set_planned_capacity(size_t planned_capacity) { _planned_capacity = planned_capacity; }
  const size_t get_planned_capacity() { return _planned_capacity; }
  const size_t get_number_of_live_handles();

  // Debugging
  bool chain_contains(jobject handle) const;    // Does this block or following blocks contain handle
  bool contains(jobject handle) const;          // Does this block contain handle
  size_t length() const;                        // Length of chain starting with this block
  size_t memory_usage() const;
  #ifndef PRODUCT
  static bool any_contains(jobject handle);     // Does any block currently in use contain handle
  static void print_statistics();
  #endif
};

#endif // SHARE_RUNTIME_JNIHANDLES_HPP
