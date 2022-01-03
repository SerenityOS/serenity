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
public:
    explicit MarkedValueList(Heap&);
    MarkedValueList(MarkedValueList const&);
    MarkedValueList(MarkedValueList&&);
    ~MarkedValueList();

    Vector<Value, 32>& values() { return *this; }

    MarkedValueList& operator=(JS::MarkedValueList const& other);

private:
    Heap* m_heap;

    IntrusiveListNode<MarkedValueList> m_list_node;

public:
    using List = IntrusiveList<&MarkedValueList::m_list_node>;
};

}
