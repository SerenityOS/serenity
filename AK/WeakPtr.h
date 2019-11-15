#pragma once

#include <AK/LogStream.h>
#include <AK/Weakable.h>

namespace AK {

template<typename T>
class OwnPtr;

template<typename T>
class WeakPtr {
    friend class Weakable<T>;

public:
    WeakPtr() {}
    WeakPtr(std::nullptr_t) {}

    template<typename U>
    WeakPtr(WeakPtr<U>&& other)
        : m_link(reinterpret_cast<WeakLink<T>*>(other.take_link().ptr()))
    {
    }

    template<typename U>
    WeakPtr& operator=(WeakPtr<U>&& other)
    {
        m_link = reinterpret_cast<WeakLink<T>*>(other.take_link().ptr());
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

    RefPtr<WeakLink<T>> take_link() { return move(m_link); }

    bool operator==(const OwnPtr<T>& other) const { return ptr() == other.ptr(); }

private:
    WeakPtr(RefPtr<WeakLink<T>> link)
        : m_link(move(link))
    {
    }

    RefPtr<WeakLink<T>> m_link;
};

template<typename T>
inline WeakPtr<T> Weakable<T>::make_weak_ptr()
{
    if (!m_link)
        m_link = adopt(*new WeakLink<T>(static_cast<T&>(*this)));
    return WeakPtr<T>(m_link);
}

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const WeakPtr<T>& value)
{
    return stream << value.ptr();
}

}

using AK::WeakPtr;
