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

#include "MmapRegion.h"
#include "SoftMMU.h"
#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace UserspaceEmulator {

class Emulator;
class SoftCPU;

struct Mallocation {
    bool contains(FlatPtr a) const
    {
        return a >= address && a < (address + size);
    }

    FlatPtr address { 0 };
    size_t size { 0 };
    bool used { false };
    bool freed { false };

    Vector<FlatPtr> malloc_backtrace;
    Vector<FlatPtr> free_backtrace;
};

class MallocRegionMetadata {
public:
    MmapRegion& region;
    FlatPtr address { 0 };
    size_t chunk_size { 0 };

    Optional<size_t> chunk_index_for_address(FlatPtr) const;
    Mallocation* mallocation_for_address(FlatPtr) const;

    Vector<Mallocation> mallocations;
};

class MallocTracer {
public:
    explicit MallocTracer(Emulator&);

    void target_did_malloc(Badge<Emulator>, FlatPtr address, size_t);
    void target_did_free(Badge<Emulator>, FlatPtr address);
    void target_did_realloc(Badge<Emulator>, FlatPtr address, size_t);

    void audit_read(const Region&, FlatPtr address, size_t);
    void audit_write(const Region&, FlatPtr address, size_t);

    void dump_leak_report();

private:
    template<typename Callback>
    void for_each_mallocation(Callback callback) const;

    Mallocation* find_mallocation(const Region&, FlatPtr);
    Mallocation* find_mallocation(FlatPtr);
    Mallocation* find_mallocation_before(FlatPtr);
    Mallocation* find_mallocation_after(FlatPtr);
    bool is_reachable(const Mallocation&) const;

    Emulator& m_emulator;

    bool m_auditing_enabled { true };
};

ALWAYS_INLINE Mallocation* MallocTracer::find_mallocation(const Region& region, FlatPtr address)
{
    if (!is<MmapRegion>(region))
        return nullptr;
    if (!static_cast<const MmapRegion&>(region).is_malloc_block())
        return nullptr;
    auto* malloc_data = static_cast<MmapRegion&>(const_cast<Region&>(region)).malloc_metadata();
    if (!malloc_data)
        return nullptr;
    auto* mallocation = malloc_data->mallocation_for_address(address);
    if (!mallocation)
        return nullptr;
    if (!mallocation->used)
        return nullptr;
    if (!mallocation->contains(address))
        return nullptr;
    return mallocation;
}

}
