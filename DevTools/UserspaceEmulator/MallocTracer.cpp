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
#include <string.h>

//#define REACHABLE_DEBUG

namespace UserspaceEmulator {

MallocTracer::MallocTracer()
{
}

void MallocTracer::target_did_malloc(Badge<SoftCPU>, FlatPtr address, size_t size)
{
    auto* region = Emulator::the().mmu().find_region({ 0x20, address });
    ASSERT(region);
    ASSERT(region->is_mmap());
    auto& mmap_region = static_cast<MmapRegion&>(*region);

    // Mark the containing mmap region as a malloc block!
    mmap_region.set_malloc(true);

    auto* shadow_bits = mmap_region.shadow_data() + address - mmap_region.base();
    memset(shadow_bits, 0, size);

    if (auto* existing_mallocation = find_mallocation(address)) {
        ASSERT(existing_mallocation->freed);
        existing_mallocation->size = size;
        existing_mallocation->freed = false;
        existing_mallocation->malloc_backtrace = Emulator::the().raw_backtrace();
        existing_mallocation->free_backtrace.clear();
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
                warnln("\n=={}==  \033[31;1mDouble free()\033[0m, {:p}", getpid(), address);
                warnln("=={}==  Address {} has already been passed to free()", getpid(), address);
                Emulator::the().dump_backtrace();
            } else {
                mallocation.freed = true;
                mallocation.free_backtrace = Emulator::the().raw_backtrace();
            }
            return;
        }
    }

    warnln("\n=={}==  \033[31;1mInvalid free()\033[0m, {:p}", getpid(), address);
    warnln("=={}==  Address {} has never been returned by malloc()", getpid(), address);
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

MallocTracer::Mallocation* MallocTracer::find_mallocation_before(FlatPtr address)
{
    Mallocation* found_mallocation = nullptr;
    for (auto& mallocation : m_mallocations) {
        if (mallocation.address >= address)
            continue;
        if (!found_mallocation || (mallocation.address > found_mallocation->address))
            found_mallocation = &mallocation;
    }
    return found_mallocation;
}

void MallocTracer::audit_read(FlatPtr address, size_t size)
{
    if (!m_auditing_enabled)
        return;

    if (Emulator::the().is_in_malloc_or_free())
        return;

    auto* mallocation = find_mallocation(address);

    if (!mallocation) {
        warnln("\n=={}==  \033[31;1mHeap buffer overflow\033[0m, invalid {}-byte read at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        if ((mallocation = find_mallocation_before(address))) {
            size_t offset_into_mallocation = address - mallocation->address;
            warnln("=={}==  Address is {} byte(s) after block of size {}, allocated at:", getpid(), offset_into_mallocation - mallocation->size, mallocation->size);
            Emulator::the().dump_backtrace(mallocation->malloc_backtrace);
        }
        return;
    }

    size_t offset_into_mallocation = address - mallocation->address;

    if (mallocation->freed) {
        warnln("\n=={}==  \033[31;1mUse-after-free\033[0m, invalid {}-byte read at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        warnln("=={}==  Address is {} byte(s) into block of size {}, allocated at:", getpid(), offset_into_mallocation, mallocation->size);
        Emulator::the().dump_backtrace(mallocation->malloc_backtrace);
        warnln("=={}==  Later freed at:", getpid());
        Emulator::the().dump_backtrace(mallocation->free_backtrace);
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
    if (!mallocation) {
        warnln("\n=={}==  \033[31;1mHeap buffer overflow\033[0m, invalid {}-byte write at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        if ((mallocation = find_mallocation_before(address))) {
            size_t offset_into_mallocation = address - mallocation->address;
            warnln("=={}==  Address is {} byte(s) into block of size {}, allocated at:", getpid(), offset_into_mallocation, mallocation->size);
            Emulator::the().dump_backtrace(mallocation->malloc_backtrace);
        }
        return;
    }

    size_t offset_into_mallocation = address - mallocation->address;

    if (mallocation->freed) {
        warnln("\n=={}==  \033[31;1mUse-after-free\033[0m, invalid {}-byte write at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        warnln("=={}==  Address is {} byte(s) into block of size {}, allocated at:", getpid(), offset_into_mallocation, mallocation->size);
        Emulator::the().dump_backtrace(mallocation->malloc_backtrace);
        warnln("=={}==  Later freed at:", getpid());
        Emulator::the().dump_backtrace(mallocation->free_backtrace);
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
        if (other_mallocation.freed)
            continue;
        size_t pointers_in_mallocation = other_mallocation.size / sizeof(u32);
        for (size_t i = 0; i < pointers_in_mallocation; ++i) {
            auto value = Emulator::the().mmu().read32({ 0x20, other_mallocation.address + i * sizeof(u32) });
            if (value.value() == mallocation.address && !value.is_uninitialized()) {
#ifdef REACHABLE_DEBUG
                warnln("mallocation {:p} is reachable from other mallocation {:p}", mallocation.address, other_mallocation.address);
#endif
                return true;
            }
        }
    }

    bool reachable = false;

    // 2. Search in other memory regions for pointers to this mallocation
    Emulator::the().mmu().for_each_region([&](auto& region) {
        // Skip the stack
        if (region.is_stack())
            return IterationDecision::Continue;
        if (region.is_text())
            return IterationDecision::Continue;
        // Skip malloc blocks
        if (region.is_mmap() && static_cast<const MmapRegion&>(region).is_malloc_block())
            return IterationDecision::Continue;

        size_t pointers_in_region = region.size() / sizeof(u32);
        for (size_t i = 0; i < pointers_in_region; ++i) {
            auto value = region.read32(i * sizeof(u32));
            if (value.value() == mallocation.address && !value.is_uninitialized()) {
#ifdef REACHABLE_DEBUG
                warnln("mallocation {:p} is reachable from region {:p}-{:p}", mallocation.address, region.base(), region.end() - 1);
#endif
                reachable = true;
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });
    return reachable;
}

void MallocTracer::dump_leak_report()
{
    TemporaryChange change(m_auditing_enabled, false);

    size_t bytes_leaked = 0;
    size_t leaks_found = 0;
    for (auto& mallocation : m_mallocations) {
        if (mallocation.freed)
            continue;
        if (is_reachable(mallocation))
            continue;
        ++leaks_found;
        bytes_leaked += mallocation.size;
        warnln("\n=={}==  \033[31;1mLeak\033[0m, {}-byte allocation at address {:p}", getpid(), mallocation.size, mallocation.address);
        Emulator::the().dump_backtrace(mallocation.malloc_backtrace);
    }

    if (!leaks_found)
        warnln("\n=={}==  \033[32;1mNo leaks found!\033[0m", getpid());
    else
        warnln("\n=={}==  \033[31;1m{} leak(s) found: {} byte(s) leaked\033[0m", getpid(), leaks_found, bytes_leaked);
}

}
