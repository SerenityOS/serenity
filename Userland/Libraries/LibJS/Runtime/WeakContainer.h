/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/IntrusiveList.h>

namespace JS {

class WeakContainer {
public:
    explicit WeakContainer(Heap&);
    virtual ~WeakContainer();

    virtual void remove_swept_cells(Badge<Heap>, Vector<Cell*>&) = 0;

protected:
    void deregister();

private:
    bool m_registered { true };
    Heap& m_heap;

    IntrusiveListNode<WeakContainer> m_list_node;

public:
    using List = IntrusiveList<WeakContainer, RawPtr<WeakContainer>, &WeakContainer::m_list_node>;
};

}
