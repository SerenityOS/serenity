/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

// This header provides architecture-agnostic abstractions for memory fences.
// These fences should only be used to enforce memory ordering constraints when interacting with device memory.
// When no device memory is involved, use atomic_thread_fence() from <AK/Atomic.h>.
// atomic_thread_fence() is generally not strong enough when interacting with device memory.

namespace Kernel {

// These functions can be used to prevent reording of memory loads and/or stores.
// Without any explicit fences (or usage of atomics, MemoryType::IO, ...), the CPU might reorder memory accesses,
// so that an outside observer might not see these memory accesses in program order.
// This is problematic for use cases like DMA, where we want the device to see all of our memory writes before
// we perform an MMIO write that notifies it about the new data.

// Note: Memory ordering is often quite complicated to wrap you head around,
//       so please add a comment that explains why a fence is necessary whenever you use a memory fence.

// This is the strongest fence. It ensures that all *loads and stores* that appear in program order before
// this fence are visible before any memory *load or store* after the fence.
void full_memory_fence();

// This function ensures that all *loads* that appear in program order before this fence
// are visible before any memory *load* after the fence.
void load_memory_fence();

// This function ensures that all *stores* that appear in program order before this fence
// are visible before any memory *store* after the fence.
void store_memory_fence();

}

#if ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/MemoryFences.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/MemoryFences.h>
#elif ARCH(X86_64)
#    include <Kernel/Arch/x86_64/MemoryFences.h>
#else
#    error Unknown architecture
#endif
