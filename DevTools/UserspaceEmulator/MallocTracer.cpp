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
#include <AK/TemporaryChange.h>
#include <mallocdefs.h>
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

    if (size <= size_classes[num_size_classes - 1]) {
        FlatPtr chunked_block_address = address & ChunkedBlock::block_mask;
        // FIXME: Don't do a double hash lookup here.
        auto tracked_chunked_block = m_chunked_blocks.get(chunked_block_address);
        if (!tracked_chunked_block.has_value()) {
            m_chunked_blocks.set(chunked_block_address, make<TrackedChunkedBlock>());
            tracked_chunked_block = m_chunked_blocks.get(chunked_block_address);
            auto& block = const_cast<TrackedChunkedBlock&>(*tracked_chunked_block.value());
            block.address = chunked_block_address;
            block.chunk_size = mmap_region.read32(offsetof(CommonHeader, m_size)).value();
            auto chunk_count = (ChunkedBlock::block_size - sizeof(ChunkedBlock)) / block.chunk_size;
            block.mallocations.resize(chunk_count);
            dbgln("Tracking ChunkedBlock @ {:p} with chunk_size={}, chunk_count={}", block.address, block.chunk_size, chunk_count);
        }
        ASSERT(tracked_chunked_block.has_value());
        auto& block = const_cast<TrackedChunkedBlock&>(*tracked_chunked_block.value());
        auto chunk_offset = address - (block.address + sizeof(ChunkedBlock));
        auto chunk_index = chunk_offset / block.chunk_size;
        block.mallocations[chunk_index] = { address, size, true, false, Emulator::the().raw_backtrace(), Vector<FlatPtr>() };
    } else {
        m_big_mallocations.append({ address, size, true, false, Emulator::the().raw_backtrace(), Vector<FlatPtr>() });
    }
}

void MallocTracer::target_did_free(Badge<SoftCPU>, FlatPtr address)
{
    if (!address)
        return;

    if (auto* mallocation = find_mallocation(address)) {
        if (mallocation->freed) {
            reportln("\n=={}==  \033[31;1mDouble free()\033[0m, {:p}", getpid(), address);
            reportln("=={}==  Address {} has already been passed to free()", getpid(), address);
            Emulator::the().dump_backtrace();
        } else {
            mallocation->freed = true;
            mallocation->free_backtrace = Emulator::the().raw_backtrace();
        }
        return;
    }

    reportln("\n=={}==  \033[31;1mInvalid free()\033[0m, {:p}", getpid(), address);
    reportln("=={}==  Address {} has never been returned by malloc()", getpid(), address);
    Emulator::the().dump_backtrace();
}

void MallocTracer::target_did_realloc(Badge<SoftCPU>, FlatPtr address, size_t size)
{
    auto* region = Emulator::the().mmu().find_region({ 0x20, address });
    ASSERT(region);
    ASSERT(region->is_mmap());
    auto& mmap_region = static_cast<MmapRegion&>(*region);

    ASSERT(mmap_region.is_malloc_block());

    auto* existing_mallocation = find_mallocation(address);
    ASSERT(existing_mallocation);
    ASSERT(!existing_mallocation->freed);

    size_t old_size = existing_mallocation->size;

    auto* shadow_bits = mmap_region.shadow_data() + address - mmap_region.base();

    if (size > old_size) {
        memset(shadow_bits + old_size, 1, size - old_size);
    } else {
        memset(shadow_bits + size, 1, old_size - size);
    }

    existing_mallocation->size = size;
    // FIXME: Should we track malloc/realloc backtrace separately perhaps?
    existing_mallocation->malloc_backtrace = Emulator::the().raw_backtrace();
}

MallocTracer::Mallocation* MallocTracer::find_mallocation(FlatPtr address)
{
    FlatPtr possible_chunked_block = address & ChunkedBlock::block_mask;

    auto chunked_block = m_chunked_blocks.get(possible_chunked_block);
    if (chunked_block.has_value()) {
        auto& block = *chunked_block.value();
        auto chunk_offset = address - (block.address + sizeof(ChunkedBlock));
        auto chunk_index = chunk_offset / block.chunk_size;
        auto& mallocation = block.mallocations[chunk_index];
        if (mallocation.used) {
            ASSERT(mallocation.contains(address));
            return const_cast<Mallocation*>(&mallocation);
        }
        return nullptr;
    }

    for (auto& mallocation : m_big_mallocations) {
        if (mallocation.contains(address))
            return &mallocation;
    }

    return nullptr;
}

MallocTracer::Mallocation* MallocTracer::find_mallocation_before(FlatPtr address)
{
    Mallocation* found_mallocation = nullptr;
    for_each_mallocation([&](auto& mallocation) {
        if (mallocation.address >= address)
            return IterationDecision::Continue;
        if (!found_mallocation || (mallocation.address > found_mallocation->address))
            found_mallocation = const_cast<Mallocation*>(&mallocation);
        return IterationDecision::Continue;
    });
    return found_mallocation;
}

MallocTracer::Mallocation* MallocTracer::find_mallocation_after(FlatPtr address)
{
    Mallocation* found_mallocation = nullptr;
    for_each_mallocation([&](auto& mallocation) {
        if (mallocation.address <= address)
            return IterationDecision::Continue;
        if (!found_mallocation || (mallocation.address < found_mallocation->address))
            found_mallocation = const_cast<Mallocation*>(&mallocation);
        return IterationDecision::Continue;
    });
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
        reportln("\n=={}==  \033[31;1mHeap buffer overflow\033[0m, invalid {}-byte read at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        auto* mallocation_before = find_mallocation_before(address);
        auto* mallocation_after = find_mallocation_after(address);
        size_t distance_to_mallocation_before = mallocation_before ? (address - mallocation_before->address - mallocation_before->size) : 0;
        size_t distance_to_mallocation_after = mallocation_after ? (mallocation_after->address - address) : 0;
        if (mallocation_before && (!mallocation_after || distance_to_mallocation_before < distance_to_mallocation_after)) {
            reportln("=={}==  Address is {} byte(s) after block of size {}, identity {:p}, allocated at:", getpid(), distance_to_mallocation_before, mallocation_before->size, mallocation_before->address);
            Emulator::the().dump_backtrace(mallocation_before->malloc_backtrace);
            return;
        }
        if (mallocation_after && (!mallocation_before || distance_to_mallocation_after < distance_to_mallocation_before)) {
            reportln("=={}==  Address is {} byte(s) before block of size {}, identity {:p}, allocated at:", getpid(), distance_to_mallocation_after, mallocation_after->size, mallocation_after->address);
            Emulator::the().dump_backtrace(mallocation_after->malloc_backtrace);
        }
        return;
    }

    size_t offset_into_mallocation = address - mallocation->address;

    if (mallocation->freed) {
        reportln("\n=={}==  \033[31;1mUse-after-free\033[0m, invalid {}-byte read at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        reportln("=={}==  Address is {} byte(s) into block of size {}, allocated at:", getpid(), offset_into_mallocation, mallocation->size);
        Emulator::the().dump_backtrace(mallocation->malloc_backtrace);
        reportln("=={}==  Later freed at:", getpid());
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
        reportln("\n=={}==  \033[31;1mHeap buffer overflow\033[0m, invalid {}-byte write at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        auto* mallocation_before = find_mallocation_before(address);
        auto* mallocation_after = find_mallocation_after(address);
        size_t distance_to_mallocation_before = mallocation_before ? (address - mallocation_before->address - mallocation_before->size) : 0;
        size_t distance_to_mallocation_after = mallocation_after ? (mallocation_after->address - address) : 0;
        if (mallocation_before && (!mallocation_after || distance_to_mallocation_before < distance_to_mallocation_after)) {
            reportln("=={}==  Address is {} byte(s) after block of size {}, identity {:p}, allocated at:", getpid(), distance_to_mallocation_before, mallocation_before->size, mallocation_before->address);
            Emulator::the().dump_backtrace(mallocation_before->malloc_backtrace);
            return;
        }
        if (mallocation_after && (!mallocation_before || distance_to_mallocation_after < distance_to_mallocation_before)) {
            reportln("=={}==  Address is {} byte(s) before block of size {}, identity {:p}, allocated at:", getpid(), distance_to_mallocation_after, mallocation_after->size, mallocation_after->address);
            Emulator::the().dump_backtrace(mallocation_after->malloc_backtrace);
        }
        return;
    }

    size_t offset_into_mallocation = address - mallocation->address;

    if (mallocation->freed) {
        reportln("\n=={}==  \033[31;1mUse-after-free\033[0m, invalid {}-byte write at address {:p}", getpid(), size, address);
        Emulator::the().dump_backtrace();
        reportln("=={}==  Address is {} byte(s) into block of size {}, allocated at:", getpid(), offset_into_mallocation, mallocation->size);
        Emulator::the().dump_backtrace(mallocation->malloc_backtrace);
        reportln("=={}==  Later freed at:", getpid());
        Emulator::the().dump_backtrace(mallocation->free_backtrace);
        return;
    }
}

bool MallocTracer::is_reachable(const Mallocation& mallocation) const
{
    ASSERT(!mallocation.freed);

    bool reachable = false;

    // 1. Search in active (non-freed) mallocations for pointers to this mallocation
    for_each_mallocation([&](auto& other_mallocation) {
        if (&mallocation == &other_mallocation)
            return IterationDecision::Continue;
        if (other_mallocation.freed)
            return IterationDecision::Continue;
        size_t pointers_in_mallocation = other_mallocation.size / sizeof(u32);
        for (size_t i = 0; i < pointers_in_mallocation; ++i) {
            auto value = Emulator::the().mmu().read32({ 0x20, other_mallocation.address + i * sizeof(u32) });
            if (value.value() == mallocation.address && !value.is_uninitialized()) {
#ifdef REACHABLE_DEBUG
                reportln("mallocation {:p} is reachable from other mallocation {:p}", mallocation.address, other_mallocation.address);
#endif
                reachable = true;
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });

    if (reachable)
        return true;

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
                reportln("mallocation {:p} is reachable from region {:p}-{:p}", mallocation.address, region.base(), region.end() - 1);
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
    for_each_mallocation([&](auto& mallocation) {
        if (mallocation.freed)
            return IterationDecision::Continue;
        if (is_reachable(mallocation))
            return IterationDecision::Continue;
        ++leaks_found;
        bytes_leaked += mallocation.size;
        reportln("\n=={}==  \033[31;1mLeak\033[0m, {}-byte allocation at address {:p}", getpid(), mallocation.size, mallocation.address);
        Emulator::the().dump_backtrace(mallocation.malloc_backtrace);
        return IterationDecision::Continue;
    });

    if (!leaks_found)
        reportln("\n=={}==  \033[32;1mNo leaks found!\033[0m", getpid());
    else
        reportln("\n=={}==  \033[31;1m{} leak(s) found: {} byte(s) leaked\033[0m", getpid(), leaks_found, bytes_leaked);
}

}
