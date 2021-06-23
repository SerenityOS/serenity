/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

class MappedROM {
public:
    const u8* base() const { return region->vaddr().offset(offset).as_ptr(); }
    const u8* end() const { return base() + size; }
    OwnPtr<Region> region;
    size_t size { 0 };
    size_t offset { 0 };
    PhysicalAddress paddr;

    Optional<PhysicalAddress> find_chunk_starting_with(StringView prefix, size_t chunk_size) const
    {
        for (auto* candidate = base(); candidate < end(); candidate += chunk_size) {
            if (!__builtin_memcmp(prefix.characters_without_null_termination(), candidate, prefix.length()))
                return paddr_of(candidate);
        }
        return {};
    }

    PhysicalAddress paddr_of(const u8* ptr) const { return paddr.offset(ptr - this->base()); }
};

}
