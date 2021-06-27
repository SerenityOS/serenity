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
        deregister();
    }

    virtual void remove_swept_cells(Badge<Heap>, Vector<Cell*>&) = 0;

protected:
    void deregister()
    {
        if (!m_registered)
            return;
        m_heap.did_destroy_weak_container({}, *this);
        m_registered = false;
    }

private:
    bool m_registered { true };
    Heap& m_heap;
};

}
