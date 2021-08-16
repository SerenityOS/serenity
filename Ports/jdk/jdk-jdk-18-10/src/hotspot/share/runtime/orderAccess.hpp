/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_ORDERACCESS_HPP
#define SHARE_RUNTIME_ORDERACCESS_HPP

#include "memory/allocation.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/macros.hpp"

//                Memory Access Ordering Model
//
// This interface is based on the JSR-133 Cookbook for Compiler Writers.
//
// In the following, the terms 'previous', 'subsequent', 'before',
// 'after', 'preceding' and 'succeeding' refer to program order.  The
// terms 'down' and 'below' refer to forward load or store motion
// relative to program order, while 'up' and 'above' refer to backward
// motion.
//
// We define four primitive memory barrier operations.
//
// LoadLoad:   Load1(s); LoadLoad; Load2
//
// Ensures that Load1 completes (obtains the value it loads from memory)
// before Load2 and any subsequent load operations.  Loads before Load1
// may *not* float below Load2 and any subsequent load operations.
//
// StoreStore: Store1(s); StoreStore; Store2
//
// Ensures that Store1 completes (the effect on memory of Store1 is made
// visible to other processors) before Store2 and any subsequent store
// operations.  Stores before Store1 may *not* float below Store2 and any
// subsequent store operations.
//
// LoadStore:  Load1(s); LoadStore; Store2
//
// Ensures that Load1 completes before Store2 and any subsequent store
// operations.  Loads before Load1 may *not* float below Store2 and any
// subsequent store operations.
//
// StoreLoad:  Store1(s); StoreLoad; Load2
//
// Ensures that Store1 completes before Load2 and any subsequent load
// operations.  Stores before Store1 may *not* float below Load2 and any
// subsequent load operations.
//
// We define two further barriers: acquire and release.
//
// Conceptually, acquire/release semantics form unidirectional and
// asynchronous barriers w.r.t. a synchronizing load(X) and store(X) pair.
// They should always be used in pairs to publish (release store) and
// access (load acquire) some implicitly understood shared data between
// threads in a relatively cheap fashion not requiring storeload. If not
// used in such a pair, it is advised to use a membar instead:
// acquire/release only make sense as pairs.
//
// T1: access_shared_data
// T1: ]release
// T1: (...)
// T1: store(X)
//
// T2: load(X)
// T2: (...)
// T2: acquire[
// T2: access_shared_data
//
// It is guaranteed that if T2: load(X) synchronizes with (observes the
// value written by) T1: store(X), then the memory accesses before the T1:
// ]release happen before the memory accesses after the T2: acquire[.
//
// Total Store Order (TSO) machines can be seen as machines issuing a
// release store for each store and a load acquire for each load. Therefore
// there is an inherent resemblence between TSO and acquire/release
// semantics. TSO can be seen as an abstract machine where loads are
// executed immediately when encountered (hence loadload reordering not
// happening) but enqueues stores in a FIFO queue
// for asynchronous serialization (neither storestore or loadstore
// reordering happening). The only reordering happening is storeload due to
// the queue asynchronously serializing stores (yet in order).
//
// Acquire/release semantics essentially exploits this asynchronicity: when
// the load(X) acquire[ observes the store of ]release store(X), the
// accesses before the release must have happened before the accesses after
// acquire.
//
// The API offers both stand-alone acquire() and release() as well as bound
// load_acquire() and release_store(). It is guaranteed that these are
// semantically equivalent w.r.t. the defined model. However, since
// stand-alone acquire()/release() does not know which previous
// load/subsequent store is considered the synchronizing load/store, they
// may be more conservative in implementations. We advise using the bound
// variants whenever possible.
//
// We define a "fence" operation, as a bidirectional barrier.
// It guarantees that any memory access preceding the fence is not
// reordered w.r.t. any memory accesses subsequent to the fence in program
// order. This may be used to prevent sequences of loads from floating up
// above sequences of stores.
//
// The following table shows the implementations on some architectures:
//
//                       Constraint     x86          sparc TSO          ppc
// ---------------------------------------------------------------------------
// fence                 LoadStore  |   lock         membar #StoreLoad  sync
//                       StoreStore |   addl 0,(sp)
//                       LoadLoad   |
//                       StoreLoad
//
// release               LoadStore  |                                   lwsync
//                       StoreStore
//
// acquire               LoadLoad   |                                   lwsync
//                       LoadStore
//
// release_store                        <store>      <store>            lwsync
//                                                                      <store>
//
// release_store_fence                  xchg         <store>            lwsync
//                                                   membar #StoreLoad  <store>
//                                                                      sync
//
//
// load_acquire                         <load>       <load>             <load>
//                                                                      lwsync
//
// Ordering a load relative to preceding stores requires a StoreLoad,
// which implies a membar #StoreLoad between the store and load under
// sparc-TSO. On x86, we use explicitly locked add.
//
// Conventional usage is to issue a load_acquire for ordered loads.  Use
// release_store for ordered stores when you care only that prior stores
// are visible before the release_store, but don't care exactly when the
// store associated with the release_store becomes visible.  Use
// release_store_fence to update values like the thread state, where we
// don't want the current thread to continue until all our prior memory
// accesses (including the new thread state) are visible to other threads.
// This is equivalent to the volatile semantics of the Java Memory Model.
//
//                    C++ Volatile Semantics
//
// C++ volatile semantics prevent compiler re-ordering between
// volatile memory accesses. However, reordering between non-volatile
// and volatile memory accesses is in general undefined. For compiler
// reordering constraints taking non-volatile memory accesses into
// consideration, a compiler barrier has to be used instead.  Some
// compiler implementations may choose to enforce additional
// constraints beyond those required by the language. Note also that
// both volatile semantics and compiler barrier do not prevent
// hardware reordering.
//
//                os::is_MP Considered Redundant
//
// Callers of this interface do not need to test os::is_MP() before
// issuing an operation. The test is taken care of by the implementation
// of the interface (depending on the vm version and platform, the test
// may or may not be actually done by the implementation).
//
//
//                A Note on Memory Ordering and Cache Coherency
//
// Cache coherency and memory ordering are orthogonal concepts, though they
// interact.  E.g., all existing itanium machines are cache-coherent, but
// the hardware can freely reorder loads wrt other loads unless it sees a
// load-acquire instruction.  All existing sparc machines are cache-coherent
// and, unlike itanium, TSO guarantees that the hardware orders loads wrt
// loads and stores, and stores wrt to each other.
//
// Consider the implementation of loadload.  *If* your platform *isn't*
// cache-coherent, then loadload must not only prevent hardware load
// instruction reordering, but it must *also* ensure that subsequent
// loads from addresses that could be written by other processors (i.e.,
// that are broadcast by other processors) go all the way to the first
// level of memory shared by those processors and the one issuing
// the loadload.
//
// So if we have a MP that has, say, a per-processor D$ that doesn't see
// writes by other processors, and has a shared E$ that does, the loadload
// barrier would have to make sure that either
//
// 1. cache lines in the issuing processor's D$ that contained data from
// addresses that could be written by other processors are invalidated, so
// subsequent loads from those addresses go to the E$, (it could do this
// by tagging such cache lines as 'shared', though how to tell the hardware
// to do the tagging is an interesting problem), or
//
// 2. there never are such cache lines in the issuing processor's D$, which
// means all references to shared data (however identified: see above)
// bypass the D$ (i.e., are satisfied from the E$).
//
// If your machine doesn't have an E$, substitute 'main memory' for 'E$'.
//
// Either of these alternatives is a pain, so no current machine we know of
// has incoherent caches.
//
// If loadload didn't have these properties, the store-release sequence for
// publishing a shared data structure wouldn't work, because a processor
// trying to read data newly published by another processor might go to
// its own incoherent caches to satisfy the read instead of to the newly
// written shared memory.
//
//
//                NOTE WELL!!
//
//                A Note on MutexLocker and Friends
//
// See mutexLocker.hpp.  We assume throughout the VM that MutexLocker's
// and friends' constructors do a fence, a lock and an acquire *in that
// order*.  And that their destructors do a release and unlock, in *that*
// order.  If their implementations change such that these assumptions
// are violated, a whole lot of code will break.
//
// Finally, we define an "instruction_fence" operation, which ensures that all
// instructions that come after the fence in program order are fetched
// from the cache or memory after the fence has completed.

class OrderAccess : public AllStatic {
 public:
  // barriers
  static void     loadload();
  static void     storestore();
  static void     loadstore();
  static void     storeload();

  static void     acquire();
  static void     release();
  static void     fence();

  static void     cross_modify_fence() {
    cross_modify_fence_impl();
    cross_modify_fence_verify();
  }

  // Processors which are not multi-copy-atomic require a full fence
  // to enforce a globally consistent order of Independent Reads of
  // Independent Writes. Please use only for such patterns!
  static void     loadload_for_IRIW() {
#ifndef CPU_MULTI_COPY_ATOMIC
    fence();
#else
    loadload();
#endif
  }
private:
  // This is a helper that invokes the StubRoutines::fence_entry()
  // routine if it exists, It should only be used by platforms that
  // don't have another way to do the inline assembly.
  static void StubRoutines_fence();

  static void cross_modify_fence_impl();

  static void cross_modify_fence_verify() PRODUCT_RETURN;
};

#include OS_CPU_HEADER(orderAccess)

#endif // SHARE_RUNTIME_ORDERACCESS_HPP
