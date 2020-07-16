/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "MallocTracer.h"
#include "Emulator.h"
#include "MmapRegion.h"
#include <AK/LogStream.h>

//#define REACHABLE_DEBUG

namespace UserspaceEmulator {

static pid_t s_pid = getpid();

MallocTracer::MallocTracer()
{
}

void MallocTracer::target_did_malloc(Badge<SoftCPU>, FlatPtr address, size_t size)
{
    if (auto* existing_mallocation = find_mallocation(address)) {
        ASSERT(existing_mallocation->freed);
        existing_mallocation->size = size;
        existing_mallocation->freed = false;
        return;
    }
    m_mallocations.append({ address, size, false, Emulator::the().raw_backtrace(), Vector<FlatPtr>() });
}

void MallocTracer::target_did_free(Badge<SoftCPU>, FlatPtr address)
{
    if (!address)
        return;

    for (auto& mallocation : m_mallocations) {
        if (mallocation.address == address) {
            if (mallocation.freed) {
                dbgprintf("\n");
                dbgprintf("==%d==  \033[31;1mDouble free()\033[0m, %p\n", s_pid, address);
                dbgprintf("==%d==  Address %p has already been passed to free()\n", s_pid, address);
                Emulator::the().dump_backtrace();
            } else {
                mallocation.freed = true;
                mallocation.free_backtrace = Emulator::the().raw_backtrace();
            }
            return;
        }
    }
    dbgprintf("\n");
    dbgprintf("==%d==  \033[31;1mInvalid free()\033[0m, %p\n", s_pid, address);
    dbgprintf("==%d==  Address %p has never been returned by malloc()\n", s_pid, address);
    Emulator::the().dump_backtrace();
}

MallocTracer::Mallocation* MallocTracer::find_mallocation(FlatPtr address)
{
    for (auto& mallocation : m_mallocations) {
        if (mallocation.contains(address))
            return &mallocation;
    }
    return nullptr;
}

void MallocTracer::audit_read(FlatPtr address, size_t size)
{
    if (!m_auditing_enabled)
        return;

    if (Emulator::the().is_in_malloc_or_free())
        return;

    auto* mallocation = find_mallocation(address);
    if (!mallocation)
        return;

    size_t offset_into_mallocation = address - mallocation->address;

    if (mallocation->freed) {
        dbgprintf("\n");
        dbgprintf("==%d==  \033[31;1mUse-after-free\033[0m, invalid %zu-byte read at address %p\n", s_pid, size, address);
        dbgprintf("==%d==  Address is %zu bytes into freed block of size %zu\n", s_pid, offset_into_mallocation, mallocation->size);
        Emulator::the().dump_backtrace();
        return;
    }
}

void MallocTracer::audit_write(FlatPtr address, size_t size)
{
    if (!m_auditing_enabled)
        return;

    if (Emulator::the().is_in_malloc_or_free())
        return;

    auto* mallocation = find_mallocation(address);
    if (!mallocation)
        return;

    size_t offset_into_mallocation = address - mallocation->address;

    if (mallocation->freed) {
        dbgprintf("\n");
        dbgprintf("==%d==  \033[31;1mUse-after-free\033[0m, invalid %zu-byte write at address %p\n", s_pid, size, address);
        dbgprintf("==%d==  Address is %zu bytes into freed block of size %zu\n", s_pid, offset_into_mallocation, mallocation->size);
        Emulator::the().dump_backtrace();
        return;
    }
}

bool MallocTracer::is_reachable(const Mallocation& mallocation) const
{
    ASSERT(!mallocation.freed);

    // 1. Search in active (non-freed) mallocations for pointers to this mallocation
    for (auto& other_mallocation : m_mallocations) {
        if (&mallocation == &other_mallocation)
            continue;
        size_t pointers_in_mallocation = other_mallocation.size / sizeof(u32);
        for (size_t i = 0; i < pointers_in_mallocation; ++i) {
            auto value = Emulator::the().mmu().read32({ 0x20, other_mallocation.address + i * sizeof(u32) });
            if (value == mallocation.address) {
#ifdef REACHABLE_DEBUG
                dbgprintf("mallocation %p is reachable from other mallocation %p\n", mallocation.address, other_mallocation.address);
#endif
                return true;
            }
        }
    }

    // 2. Search in other memory regions for pointers to this mallocation
    for (auto& region : Emulator::the().mmu().regions()) {
        // Skip the stack
        if (region.is_stack())
            continue;
        // Skip malloc blocks
        if (region.is_mmap() && static_cast<const MmapRegion&>(region).is_malloc_block())
            continue;

        size_t pointers_in_region = region.size() / sizeof(u32);
        for (size_t i = 0; i < pointers_in_region; ++i) {
            auto value = region.read32(i * sizeof(u32));
            if (value == mallocation.address) {
#ifdef REACHABLE_DEBUG
                dbgprintf("mallocation %p is reachable from region %p-%p\n", mallocation.address, region.base(), region.end() - 1);
#endif
                return true;
            }
        }
    }

    return false;
}

void MallocTracer::dump_leak_report()
{
    TemporaryChange change(m_auditing_enabled, false);

    size_t leaks_found = 0;
    for (auto& mallocation : m_mallocations) {
        if (mallocation.freed)
            continue;
        if (is_reachable(mallocation))
            continue;
        ++leaks_found;
        dbgprintf("\n");
        dbgprintf("==%d==  \033[31;1mLeak\033[0m, %zu-byte allocation at address %p\n", s_pid, mallocation.size, mallocation.address);
        Emulator::the().dump_backtrace(mallocation.malloc_backtrace);
    }

    dbgprintf("==%d==  \033[%d;1m%zu leak(s) found\033[0m\n", s_pid, leaks_found ? 31 : 32, leaks_found);
}

}
