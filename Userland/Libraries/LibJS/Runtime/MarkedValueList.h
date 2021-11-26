/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class MarkedValueList : public Vector<Value, 32> {
    AK_MAKE_NONCOPYABLE(MarkedValueList);

public:
    explicit MarkedValueList(Heap&);
    MarkedValueList(MarkedValueList&&);
    ~MarkedValueList();

    MarkedValueList& operator=(MarkedValueList&&) = delete;

    Vector<Value, 32>& values() { return *this; }

    MarkedValueList copy() const
    {
        MarkedValueList copy { m_heap };
        copy.extend(*this);
        return copy;
    }

private:
    Heap& m_heap;

    IntrusiveListNode<MarkedValueList> m_list_node;

public:
    using List = IntrusiveList<&MarkedValueList::m_list_node>;
};

}
