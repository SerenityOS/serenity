/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class HandleImpl : public RefCounted<HandleImpl> {
    AK_MAKE_NONCOPYABLE(HandleImpl);
    AK_MAKE_NONMOVABLE(HandleImpl);

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
    using List = IntrusiveList<&HandleImpl::m_list_node>;
};

template<class T>
class Handle {
public:
    Handle() = default;

    static Handle create(T* cell)
    {
        return Handle(adopt_ref(*new HandleImpl(cell)));
    }

    T* cell() { return static_cast<T*>(m_impl->cell()); }
    const T* cell() const { return static_cast<const T*>(m_impl->cell()); }

    bool is_null() const { return m_impl.is_null(); }

    T* operator->() { return cell(); }
    T const* operator->() const { return cell(); }

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

template<class T>
inline Handle<T> make_handle(T& cell)
{
    return Handle<T>::create(&cell);
}

template<>
class Handle<Value> {
public:
    Handle() = default;

    static Handle create(Value value)
    {
        if (value.is_cell())
            return Handle(value, &value.as_cell());
        return Handle(value);
    }

    auto cell() { return m_handle.cell(); }
    auto cell() const { return m_handle.cell(); }
    auto value() const { return m_value; }
    bool is_null() const { return m_handle.is_null(); }

private:
    explicit Handle(Value value)
        : m_value(value)
    {
    }

    explicit Handle(Value value, Cell* cell)
        : m_value(value)
        , m_handle(Handle<Cell>::create(cell))
    {
    }

    Value m_value;
    Handle<Cell> m_handle;
};

inline Handle<Value> make_handle(Value value)
{
    return Handle<Value>::create(value);
}

}
