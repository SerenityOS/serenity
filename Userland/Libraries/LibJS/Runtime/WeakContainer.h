/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Heap.h>

namespace JS {

class WeakContainer {
public:
    explicit WeakContainer(Heap& heap)
        : m_heap(heap)
    {
        m_heap.did_create_weak_container({}, *this);
    }
    virtual ~WeakContainer()
    {
        m_heap.did_destroy_weak_container({}, *this);
    }

    virtual void remove_sweeped_cells(Badge<Heap>, Vector<Cell*>&) = 0;

private:
    Heap& m_heap;
};

}
