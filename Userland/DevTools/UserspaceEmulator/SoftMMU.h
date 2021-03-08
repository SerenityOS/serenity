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

#pragma once

#include "Region.h"
#include "ValueWithShadow.h"
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <LibX86/Instruction.h>

namespace UserspaceEmulator {

class Emulator;

class SoftMMU {
public:
    explicit SoftMMU(Emulator&);

    ValueWithShadow<u8> read8(X86::LogicalAddress);
    ValueWithShadow<u16> read16(X86::LogicalAddress);
    ValueWithShadow<u32> read32(X86::LogicalAddress);
    ValueWithShadow<u64> read64(X86::LogicalAddress);

    void write8(X86::LogicalAddress, ValueWithShadow<u8>);
    void write16(X86::LogicalAddress, ValueWithShadow<u16>);
    void write32(X86::LogicalAddress, ValueWithShadow<u32>);
    void write64(X86::LogicalAddress, ValueWithShadow<u64>);

    ALWAYS_INLINE Region* find_region(X86::LogicalAddress address)
    {
        if (address.selector() == 0x2b)
            return m_tls_region.ptr();

        size_t page_index = address.offset() / PAGE_SIZE;
        return m_page_to_region_map[page_index];
    }

    void add_region(NonnullOwnPtr<Region>);
    void remove_region(Region&);
    void ensure_split_at(X86::LogicalAddress);

    void set_tls_region(NonnullOwnPtr<Region>);

    bool fast_fill_memory8(X86::LogicalAddress, size_t size, ValueWithShadow<u8>);
    bool fast_fill_memory32(X86::LogicalAddress, size_t size, ValueWithShadow<u32>);

    void copy_to_vm(FlatPtr destination, const void* source, size_t);
    void copy_from_vm(void* destination, const FlatPtr source, size_t);
    ByteBuffer copy_buffer_from_vm(const FlatPtr source, size_t);

    template<typename Callback>
    void for_each_region(Callback callback)
    {
        if (m_tls_region) {
            if (callback(*m_tls_region) == IterationDecision::Break)
                return;
        }
        for (auto& region : m_regions) {
            if (callback(region) == IterationDecision::Break)
                return;
        }
    }

    template<typename Callback>
    void for_regions_in(X86::LogicalAddress address, size_t size, Callback callback)
    {
        VERIFY(size > 0);
        X86::LogicalAddress address_end = address;
        address_end.set_offset(address_end.offset() + size);
        ensure_split_at(address);
        ensure_split_at(address_end);

        size_t first_page = address.offset() / PAGE_SIZE;
        size_t last_page = (address_end.offset() - 1) / PAGE_SIZE;
        Region* last_reported = nullptr;
        for (size_t page = first_page; page <= last_page; ++page) {
            Region* current_region = m_page_to_region_map[page];
            if (page != first_page && current_region == last_reported)
                continue;
            if (callback(current_region) == IterationDecision::Break)
                return;
            last_reported = current_region;
        }
    }

private:
    Emulator& m_emulator;

    Region* m_page_to_region_map[786432];

    OwnPtr<Region> m_tls_region;
    NonnullOwnPtrVector<Region> m_regions;
};

}
