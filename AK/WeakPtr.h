#pragma once

#include "Weakable.h"

namespace AK {

template<typename T> class OwnPtr;

template<typename T>
class WeakPtr {
    friend class Weakable<T>;
public:
    WeakPtr() { }
    WeakPtr(std::nullptr_t) { }

    template<typename U>
    WeakPtr(WeakPtr<U>&& other)
        : m_link(reinterpret_cast<WeakLink<T>*>(other.leak_link()))
    {
    }

    template<typename U>
    WeakPtr& operator=(WeakPtr<U>&& other)
    {
        m_link = reinterpret_cast<WeakLink<T>*>(other.leak_link());
        return *this;
    }

    operator bool() const { return ptr(); }

    T* ptr() { return m_link ? m_link->ptr() : nullptr; }
    const T* ptr() const { return m_link ? m_link->ptr() : nullptr; }

    T* operator->() { return ptr(); }
    const T* operator->() const { return ptr(); }

    T& operator*() { return *ptr(); }
    const T& operator*() const { return *ptr(); }

    operator const T*() const { return ptr(); }
    operator T*() { return ptr(); }

    bool is_null() const { return !m_link || !m_link->ptr(); }
    void clear() { m_link = nullptr; }

    WeakLink<T>* leak_link() { return m_link.leak_ref(); }

    bool operator==(const OwnPtr<T>& other) const { return ptr() == other.ptr(); }

private:
    WeakPtr(RetainPtr<WeakLink<T>>&& link) : m_link(move(link)) { }

    RetainPtr<WeakLink<T>> m_link;
};

template<typename T>
inline WeakPtr<T> Weakable<T>::make_weak_ptr()
{
    if (!m_link)
        m_link = adopt(*new WeakLink<T>(static_cast<T&>(*this)));
    return WeakPtr<T>(m_link.copy_ref());
}

}

using AK::WeakPtr;

