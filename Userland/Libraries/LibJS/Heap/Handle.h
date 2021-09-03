/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Badge.h>
#include <YAK/IntrusiveList.h>
#include <YAK/Noncopyable.h>
#include <YAK/RefCounted.h>
#include <YAK/RefPtr.h>
#include <LibJS/Forward.h>

namespace JS {

class HandleImpl : public RefCounted<HandleImpl> {
    YAK_MAKE_NONCOPYABLE(HandleImpl);
    YAK_MAKE_NONMOVABLE(HandleImpl);

public:
    ~HandleImpl();

    Cell* cell() { return m_cell; }
    const Cell* cell() const { return m_cell; }

private:
    template<class T>
    friend class Handle;

    explicit HandleImpl(Cell*);
    Cell* m_cell { nullptr };

    IntrusiveListNode<HandleImpl> m_list_node;

public:
    using List = IntrusiveList<HandleImpl, RawPtr<HandleImpl>, &HandleImpl::m_list_node>;
};

template<class T>
class Handle {
public:
    Handle() { }

    static Handle create(T* cell)
    {
        return Handle(adopt_ref(*new HandleImpl(cell)));
    }

    T* cell() { return static_cast<T*>(m_impl->cell()); }
    const T* cell() const { return static_cast<const T*>(m_impl->cell()); }

    bool is_null() const { return m_impl.is_null(); }

private:
    explicit Handle(NonnullRefPtr<HandleImpl> impl)
        : m_impl(move(impl))
    {
    }

    RefPtr<HandleImpl> m_impl;
};

template<class T>
inline Handle<T> make_handle(T* cell)
{
    return Handle<T>::create(cell);
}

}
