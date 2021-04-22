/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

class ScatterGatherList {
    struct ScatterGatherEntry {
        FlatPtr page_base;
        size_t offset;
        size_t length;
    };

public:
    static ScatterGatherList create_from_buffer(const u8* buffer, size_t);
    static ScatterGatherList create_from_physical(PhysicalAddress, size_t);

    void add_entry(FlatPtr, size_t offset, size_t size);
    [[nodiscard]] size_t length() const { return m_entries.size(); }

    void for_each_entry(Function<void(const FlatPtr, const size_t)> callback) const;

private:
    Vector<ScatterGatherEntry> m_entries;
};

}
