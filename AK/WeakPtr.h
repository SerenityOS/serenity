#pragma once

#include "Weakable.h"

namespace AK {

template<typename T>
class WeakPtr {
    friend class Weakable<T>;
public:
    WeakPtr() { }
    WeakPtr(std::nullptr_t) { }

    operator bool() const { return ptr(); }

    T* ptr() { return m_link ? m_link->ptr() : nullptr; }
    const T* ptr() const { return m_link ? m_link->ptr() : nullptr; }
    bool isNull() const { return !m_link || !m_link->ptr(); }

private:
    WeakPtr(RetainPtr<WeakLink<T>>&& link) : m_link(std::move(link)) { }

    RetainPtr<WeakLink<T>> m_link;
};

template<typename T>
inline WeakPtr<T> Weakable<T>::makeWeakPtr()
{
    if (!m_link)
        m_link = adopt(*new WeakLink<T>(*this));
    return WeakPtr<T>(m_link.copyRef());
}

}

using AK::WeakPtr;

