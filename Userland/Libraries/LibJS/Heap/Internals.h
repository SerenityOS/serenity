/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/Noncopyable.h>
#include <AK/Types.h>
#include <LibJS/Forward.h>

namespace JS {

class HeapBase {
    AK_MAKE_NONCOPYABLE(HeapBase);
    AK_MAKE_NONMOVABLE(HeapBase);

public:
    VM& vm() { return m_vm; }

protected:
    HeapBase(VM& vm)
        : m_vm(vm)
    {
    }

    VM& m_vm;
};

class HeapBlockBase {
    AK_MAKE_NONMOVABLE(HeapBlockBase);
    AK_MAKE_NONCOPYABLE(HeapBlockBase);

public:
    static constexpr auto block_size = 4 * KiB;
    static HeapBlockBase* from_cell(Cell const* cell)
    {
        return reinterpret_cast<HeapBlockBase*>(bit_cast<FlatPtr>(cell) & ~(HeapBlockBase::block_size - 1));
    }

    Heap& heap() { return m_heap; }

protected:
    HeapBlockBase(Heap& heap)
        : m_heap(heap)
    {
    }

    Heap& m_heap;
};

}
