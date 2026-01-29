/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/OwnPtr.h>
#include <Kernel/Library/MiniStdLib.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/Region.h>

namespace Kernel::Memory {

class MappedROM {
public:
    u8 const* base() const { return region->vaddr().offset(offset).as_ptr(); }
    u8 const* end() const { return base() + size; }
    OwnPtr<Region> region;
    size_t size { 0 };
    size_t offset { 0 };
    PhysicalAddress paddr;

    Optional<PhysicalAddress> find_chunk_starting_with(StringView prefix, size_t chunk_size, CallableAs<bool, ReadonlyBytes> auto predicate) const
    {
        PhysicalAddress start_paddr = PhysicalAddress { align_up_to(paddr.get(), chunk_size) };
        size_t start_offset = start_paddr.get() - paddr.get();

        for (size_t offset = start_offset; offset <= size - prefix.length(); offset += chunk_size) {
            u8 const* candidate = base() + offset;
            if (memcmp(prefix.characters_without_null_termination(), candidate, prefix.length()) != 0)
                continue;

            if (!predicate(ReadonlyBytes { candidate, size - offset }))
                continue;

            return paddr_of(candidate);
        }

        return {};
    }

    PhysicalAddress paddr_of(u8 const* ptr) const { return paddr.offset(ptr - this->base()); }
};

}
