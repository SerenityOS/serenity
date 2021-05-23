/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

struct GraphNode {
    Vector<FlatPtr> edges_from_node {};

    bool is_reachable { false };
};

using MemoryGraph = HashMap<FlatPtr, GraphNode>;

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
    void target_did_change_chunk_size(Badge<Emulator>, FlatPtr, size_t);

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

    void dump_memory_graph();
    void populate_memory_graph();

    void update_metadata(MmapRegion& mmap_region, size_t chunk_size);

    Emulator& m_emulator;

    MemoryGraph m_memory_graph {};

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
