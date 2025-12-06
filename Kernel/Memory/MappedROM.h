/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
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

    u8 const* pointer_to_chunk_starting_with(StringView prefix, size_t chunk_size) const
    {
        auto prefix_length = prefix.length();
        if (size < prefix_length)
            return nullptr;
        for (auto* candidate = base(); candidate <= end() - prefix_length; candidate += chunk_size) {
            if (!__builtin_memcmp(prefix.characters_without_null_termination(), candidate, prefix.length()))
                return candidate;
        }
        return nullptr;
    }

    Optional<PhysicalAddress> find_chunk_starting_with(StringView prefix, size_t chunk_size) const
    {
        auto result = pointer_to_chunk_starting_with(prefix, chunk_size);
        if (!result)
            return {};
        return paddr_of(result);
    }

    PhysicalAddress paddr_of(u8 const* ptr) const { return paddr.offset(ptr - this->base()); }
};

}
