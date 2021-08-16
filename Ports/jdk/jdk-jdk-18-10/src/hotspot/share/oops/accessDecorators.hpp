/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_ACCESSDECORATORS_HPP
#define SHARE_OOPS_ACCESSDECORATORS_HPP

#include "gc/shared/barrierSetConfig.hpp"
#include "memory/allocation.hpp"
#include "metaprogramming/integralConstant.hpp"
#include "utilities/globalDefinitions.hpp"

// A decorator is an attribute or property that affects the way a memory access is performed in some way.
// There are different groups of decorators. Some have to do with memory ordering, others to do with,
// e.g. strength of references, strength of GC barriers, or whether compression should be applied or not.
// Some decorators are set at buildtime, such as whether primitives require GC barriers or not, others
// at callsites such as whether an access is in the heap or not, and others are resolved at runtime
// such as GC-specific barriers and encoding/decoding compressed oops.
typedef uint64_t DecoratorSet;

// The HasDecorator trait can help at compile-time determining whether a decorator set
// has an intersection with a certain other decorator set
template <DecoratorSet decorators, DecoratorSet decorator>
struct HasDecorator: public IntegralConstant<bool, (decorators & decorator) != 0> {};

// == General Decorators ==
// * DECORATORS_NONE: This is the name for the empty decorator set (in absence of other decorators).
const DecoratorSet DECORATORS_NONE                   = UCONST64(0);

// == Internal Decorators - do not use ==
// * INTERNAL_CONVERT_COMPRESSED_OOPS: This is an oop access that will require converting an oop
//   to a narrowOop or vice versa, if UseCompressedOops is known to be set.
// * INTERNAL_VALUE_IS_OOP: Remember that the involved access is on oop rather than primitive.
const DecoratorSet INTERNAL_CONVERT_COMPRESSED_OOP   = UCONST64(1) << 1;
const DecoratorSet INTERNAL_VALUE_IS_OOP             = UCONST64(1) << 2;

// == Internal run-time Decorators ==
// * INTERNAL_RT_USE_COMPRESSED_OOPS: This decorator will be set in runtime resolved
//   access backends iff UseCompressedOops is true.
const DecoratorSet INTERNAL_RT_USE_COMPRESSED_OOPS   = UCONST64(1) << 5;

const DecoratorSet INTERNAL_DECORATOR_MASK           = INTERNAL_CONVERT_COMPRESSED_OOP | INTERNAL_VALUE_IS_OOP |
                                                       INTERNAL_RT_USE_COMPRESSED_OOPS;

// == Memory Ordering Decorators ==
// The memory ordering decorators can be described in the following way:
// === Decorator Rules ===
// The different types of memory ordering guarantees have a strict order of strength.
// Explicitly specifying the stronger ordering implies that the guarantees of the weaker
// property holds too. The names come from the C++11 atomic operations, and typically
// have a JMM equivalent property.
// The equivalence may be viewed like this:
// MO_UNORDERED is equivalent to JMM plain.
// MO_RELAXED is equivalent to JMM opaque.
// MO_ACQUIRE is equivalent to JMM acquire.
// MO_RELEASE is equivalent to JMM release.
// MO_SEQ_CST is equivalent to JMM volatile.
//
// === Stores ===
//  * MO_UNORDERED (Default): No guarantees.
//    - The compiler and hardware are free to reorder aggressively. And they will.
//  * MO_RELAXED: Relaxed atomic stores.
//    - The stores are atomic.
//    - The stores are not reordered by the compiler (but possibly the HW) w.r.t
//      other ordered accesses in program order.
//    - Also used for C++ volatile stores, since actual usage of volatile
//      requires no word tearing.
//  * MO_RELEASE: Releasing stores.
//    - The releasing store will make its preceding memory accesses observable to memory accesses
//      subsequent to an acquiring load observing this releasing store.
//    - Guarantees from relaxed stores hold.
//  * MO_SEQ_CST: Sequentially consistent stores.
//    - The stores are observed in the same order by MO_SEQ_CST loads on other processors
//    - Preceding loads and stores in program order are not reordered with subsequent loads and stores in program order.
//    - Guarantees from releasing stores hold.
// === Loads ===
//  * MO_UNORDERED (Default): No guarantees
//    - The compiler and hardware are free to reorder aggressively. And they will.
//  * MO_RELAXED: Relaxed atomic loads.
//    - The loads are atomic.
//    - The loads are not reordered by the compiler (but possibly the HW) w.r.t.
//      other ordered accesses in program order.
//    - Also used for C++ volatile loads, since actual usage of volatile
//      requires no word tearing.
//  * MO_ACQUIRE: Acquiring loads.
//    - An acquiring load will make subsequent memory accesses observe the memory accesses
//      preceding the releasing store that the acquiring load observed.
//    - Guarantees from relaxed loads hold.
//  * MO_SEQ_CST: Sequentially consistent loads.
//    - These loads observe MO_SEQ_CST stores in the same order on other processors
//    - Preceding loads and stores in program order are not reordered with subsequent loads and stores in program order.
//    - Guarantees from acquiring loads hold.
// === Atomic Cmpxchg ===
//  * MO_RELAXED: Atomic but relaxed cmpxchg.
//    - Guarantees from MO_RELAXED loads and MO_RELAXED stores hold unconditionally.
//  * MO_SEQ_CST: Sequentially consistent cmpxchg.
//    - Guarantees from MO_SEQ_CST loads and MO_SEQ_CST stores hold unconditionally.
// === Atomic Xchg ===
//  * MO_RELAXED: Atomic but relaxed atomic xchg.
//    - Guarantees from MO_RELAXED loads and MO_RELAXED stores hold.
//  * MO_SEQ_CST: Sequentially consistent xchg.
//    - Guarantees from MO_SEQ_CST loads and MO_SEQ_CST stores hold.
const DecoratorSet MO_UNORDERED      = UCONST64(1) << 6;
const DecoratorSet MO_RELAXED        = UCONST64(1) << 7;
const DecoratorSet MO_ACQUIRE        = UCONST64(1) << 8;
const DecoratorSet MO_RELEASE        = UCONST64(1) << 9;
const DecoratorSet MO_SEQ_CST        = UCONST64(1) << 10;
const DecoratorSet MO_DECORATOR_MASK = MO_UNORDERED | MO_RELAXED |
                                       MO_ACQUIRE | MO_RELEASE | MO_SEQ_CST;

// === Barrier Strength Decorators ===
// * AS_RAW: The access will translate into a raw memory access, hence ignoring all semantic concerns
//   except memory ordering and compressed oops. This will bypass runtime function pointer dispatching
//   in the pipeline and hardwire to raw accesses without going trough the GC access barriers.
//  - Accesses on oop* translate to raw memory accesses without runtime checks
//  - Accesses on narrowOop* translate to encoded/decoded memory accesses without runtime checks
//  - Accesses on HeapWord* translate to a runtime check choosing one of the above
//  - Accesses on other types translate to raw memory accesses without runtime checks
// * AS_NO_KEEPALIVE: The barrier is used only on oop references and will not keep any involved objects
//   alive, regardless of the type of reference being accessed. It will however perform the memory access
//   in a consistent way w.r.t. e.g. concurrent compaction, so that the right field is being accessed,
//   or maintain, e.g. intergenerational or interregional pointers if applicable. This should be used with
//   extreme caution in isolated scopes.
// * AS_NORMAL: The accesses will be resolved to an accessor on the BarrierSet class, giving the
//   responsibility of performing the access and what barriers to be performed to the GC. This is the default.
//   Note that primitive accesses will only be resolved on the barrier set if the appropriate build-time
//   decorator for enabling primitive barriers is enabled for the build.
const DecoratorSet AS_RAW                  = UCONST64(1) << 11;
const DecoratorSet AS_NO_KEEPALIVE         = UCONST64(1) << 12;
const DecoratorSet AS_NORMAL               = UCONST64(1) << 13;
const DecoratorSet AS_DECORATOR_MASK       = AS_RAW | AS_NO_KEEPALIVE | AS_NORMAL;

// === Reference Strength Decorators ===
// These decorators only apply to accesses on oop-like types (oop/narrowOop).
// * ON_STRONG_OOP_REF: Memory access is performed on a strongly reachable reference.
// * ON_WEAK_OOP_REF: The memory access is performed on a weakly reachable reference.
// * ON_PHANTOM_OOP_REF: The memory access is performed on a phantomly reachable reference.
//   This is the same ring of strength as jweak and weak oops in the VM.
// * ON_UNKNOWN_OOP_REF: The memory access is performed on a reference of unknown strength.
//   This could for example come from the unsafe API.
// * Default (no explicit reference strength specified): ON_STRONG_OOP_REF
const DecoratorSet ON_STRONG_OOP_REF  = UCONST64(1) << 14;
const DecoratorSet ON_WEAK_OOP_REF    = UCONST64(1) << 15;
const DecoratorSet ON_PHANTOM_OOP_REF = UCONST64(1) << 16;
const DecoratorSet ON_UNKNOWN_OOP_REF = UCONST64(1) << 17;
const DecoratorSet ON_DECORATOR_MASK  = ON_STRONG_OOP_REF | ON_WEAK_OOP_REF |
                                        ON_PHANTOM_OOP_REF | ON_UNKNOWN_OOP_REF;

// === Access Location ===
// Accesses can take place in, e.g. the heap, old or young generation, different native roots, or native memory off the heap.
// The location is important to the GC as it may imply different actions. The following decorators are used:
// * IN_HEAP: The access is performed in the heap. Many barriers such as card marking will
//   be omitted if this decorator is not set.
// * IN_NATIVE: The access is performed in an off-heap data structure.
const DecoratorSet IN_HEAP            = UCONST64(1) << 18;
const DecoratorSet IN_NATIVE          = UCONST64(1) << 19;
const DecoratorSet IN_DECORATOR_MASK  = IN_HEAP | IN_NATIVE;

// == Boolean Flag Decorators ==
// * IS_ARRAY: The access is performed on a heap allocated array. This is sometimes a special case
//   for some GCs.
// * IS_DEST_UNINITIALIZED: This property can be important to e.g. SATB barriers by
//   marking that the previous value is uninitialized nonsense rather than a real value.
// * IS_NOT_NULL: This property can make certain barriers faster such as compressing oops.
const DecoratorSet IS_ARRAY              = UCONST64(1) << 20;
const DecoratorSet IS_DEST_UNINITIALIZED = UCONST64(1) << 21;
const DecoratorSet IS_NOT_NULL           = UCONST64(1) << 22;

// == Arraycopy Decorators ==
// * ARRAYCOPY_CHECKCAST: This property means that the class of the objects in source
//   are not guaranteed to be subclasses of the class of the destination array. This requires
//   a check-cast barrier during the copying operation. If this is not set, it is assumed
//   that the array is covariant: (the source array type is-a destination array type)
// * ARRAYCOPY_DISJOINT: This property means that it is known that the two array ranges
//   are disjoint.
// * ARRAYCOPY_ARRAYOF: The copy is in the arrayof form.
// * ARRAYCOPY_ATOMIC: The accesses have to be atomic over the size of its elements.
// * ARRAYCOPY_ALIGNED: The accesses have to be aligned on a HeapWord.
const DecoratorSet ARRAYCOPY_CHECKCAST            = UCONST64(1) << 23;
const DecoratorSet ARRAYCOPY_DISJOINT             = UCONST64(1) << 24;
const DecoratorSet ARRAYCOPY_ARRAYOF              = UCONST64(1) << 25;
const DecoratorSet ARRAYCOPY_ATOMIC               = UCONST64(1) << 26;
const DecoratorSet ARRAYCOPY_ALIGNED              = UCONST64(1) << 27;
const DecoratorSet ARRAYCOPY_DECORATOR_MASK       = ARRAYCOPY_CHECKCAST | ARRAYCOPY_DISJOINT |
                                                    ARRAYCOPY_DISJOINT | ARRAYCOPY_ARRAYOF |
                                                    ARRAYCOPY_ATOMIC | ARRAYCOPY_ALIGNED;

// == Resolve barrier decorators ==
// * ACCESS_READ: Indicate that the resolved object is accessed read-only. This allows the GC
//   backend to use weaker and more efficient barriers.
// * ACCESS_WRITE: Indicate that the resolved object is used for write access.
const DecoratorSet ACCESS_READ                    = UCONST64(1) << 28;
const DecoratorSet ACCESS_WRITE                   = UCONST64(1) << 29;

// Keep track of the last decorator.
const DecoratorSet DECORATOR_LAST = UCONST64(1) << 29;

namespace AccessInternal {
  // This class adds implied decorators that follow according to decorator rules.
  // For example adding default reference strength and default memory ordering
  // semantics.
  template <DecoratorSet input_decorators>
  struct DecoratorFixup: AllStatic {
    // If no reference strength has been picked, then strong will be picked
    static const DecoratorSet ref_strength_default = input_decorators |
      (((ON_DECORATOR_MASK & input_decorators) == 0 && (INTERNAL_VALUE_IS_OOP & input_decorators) != 0) ?
       ON_STRONG_OOP_REF : DECORATORS_NONE);
    // If no memory ordering has been picked, unordered will be picked
    static const DecoratorSet memory_ordering_default = ref_strength_default |
      ((MO_DECORATOR_MASK & ref_strength_default) == 0 ? MO_UNORDERED : DECORATORS_NONE);
    // If no barrier strength has been picked, normal will be used
    static const DecoratorSet barrier_strength_default = memory_ordering_default |
      ((AS_DECORATOR_MASK & memory_ordering_default) == 0 ? AS_NORMAL : DECORATORS_NONE);
    static const DecoratorSet value = barrier_strength_default;
  };

  // This function implements the above DecoratorFixup rules, but without meta
  // programming for code generation that does not use templates.
  inline DecoratorSet decorator_fixup(DecoratorSet input_decorators) {
    // If no reference strength has been picked, then strong will be picked
    DecoratorSet ref_strength_default = input_decorators |
      (((ON_DECORATOR_MASK & input_decorators) == 0 && (INTERNAL_VALUE_IS_OOP & input_decorators) != 0) ?
       ON_STRONG_OOP_REF : DECORATORS_NONE);
    // If no memory ordering has been picked, unordered will be picked
    DecoratorSet memory_ordering_default = ref_strength_default |
      ((MO_DECORATOR_MASK & ref_strength_default) == 0 ? MO_UNORDERED : DECORATORS_NONE);
    // If no barrier strength has been picked, normal will be used
    DecoratorSet barrier_strength_default = memory_ordering_default |
      ((AS_DECORATOR_MASK & memory_ordering_default) == 0 ? AS_NORMAL : DECORATORS_NONE);
    return barrier_strength_default;
  }
}

#endif // SHARE_OOPS_ACCESSDECORATORS_HPP
