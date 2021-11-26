/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    ValueWithShadow<u128> read128(X86::LogicalAddress);
    ValueWithShadow<u256> read256(X86::LogicalAddress);

    void write8(X86::LogicalAddress, ValueWithShadow<u8>);
    void write16(X86::LogicalAddress, ValueWithShadow<u16>);
    void write32(X86::LogicalAddress, ValueWithShadow<u32>);
    void write64(X86::LogicalAddress, ValueWithShadow<u64>);
    void write128(X86::LogicalAddress, ValueWithShadow<u128>);
    void write256(X86::LogicalAddress, ValueWithShadow<u256>);

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

    template<typename Type, typename Callback>
    void for_each_region_of_type(Callback callback)
    {
        return for_each_region([callback](auto& region) {
            if (!is<Type>(region))
                return IterationDecision::Continue;
            return callback(static_cast<Type&>(region));
        });
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

    Region* m_page_to_region_map[786432] = { nullptr };

    OwnPtr<Region> m_tls_region;
    NonnullOwnPtrVector<Region> m_regions;
};

}
