#pragma once

#include <cstddef>
#include <utility>

namespace AK {

template<typename T>
class OwnPtr {
public:
    OwnPtr() { }
    explicit OwnPtr(T* ptr) : m_ptr(ptr) { }
    OwnPtr(OwnPtr&& other) : m_ptr(other.leakPtr()) { }
    template<typename U> OwnPtr(OwnPtr<U>&& other) : m_ptr(static_cast<T*>(other.leakPtr())) { }
    ~OwnPtr() { clear(); }
    OwnPtr(std::nullptr_t) { };

    OwnPtr& operator=(OwnPtr&& other)
    {
        if (this != &other) {
            delete m_ptr;
            m_ptr = other.leakPtr();
        }
        return *this;
    }

    template<typename U>
    OwnPtr& operator=(OwnPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            delete m_ptr;
            m_ptr = other.leakPtr();
        }
        return *this;
    }

    OwnPtr& operator=(T* ptr)
    {
        if (m_ptr != ptr)
            delete m_ptr;
        m_ptr = ptr;
        return *this;
    }

    OwnPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    void clear()
    {
        delete m_ptr;
        m_ptr = nullptr;
    }

    bool operator!() const { return !m_ptr; }

    typedef T* OwnPtr::*UnspecifiedBoolType;
    operator UnspecifiedBoolType() const { return m_ptr ? &OwnPtr::m_ptr : nullptr; }

    T* leakPtr()
    {
        T* leakedPtr = m_ptr;
        m_ptr = nullptr;
        return leakedPtr;
    }

    T* ptr() { return m_ptr; }
    const T* ptr() const { return m_ptr; }

    T* operator->() { return m_ptr; }
    const T* operator->() const { return m_ptr; }

    T& operator*() { return *m_ptr; }
    const T& operator*() const { return *m_ptr; }

    operator bool() { return !!m_ptr; }

private:
    T* m_ptr = nullptr;
};

template<class T, class... Args> inline OwnPtr<T>
make(Args&&... args)
{
    return OwnPtr<T>(new T(std::forward<Args>(args)...));
}

}

using AK::OwnPtr;
using AK::make;

