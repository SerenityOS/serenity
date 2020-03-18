#pragma once

#include <AK/Badge.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibJS/Forward.h>

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
};

template<class T>
class Handle {
public:
    Handle() {}

    static Handle create(T* cell)
    {
        return Handle(adopt(*new HandleImpl(cell)));
    }

    T* cell() { return static_cast<T*>(m_impl->cell()); }
    const T* cell() const { return static_cast<const T*>(m_impl->cell()); }

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
