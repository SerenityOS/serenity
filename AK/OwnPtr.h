#pragma once

#include "StdLibExtras.h"
#include "Traits.h"
#include "Types.h"

namespace AK {

template<typename T>
class OwnPtr {
public:
    OwnPtr() {}
    explicit OwnPtr(T* ptr)
        : m_ptr(ptr)
    {
    }
    OwnPtr(OwnPtr&& other)
        : m_ptr(other.leak_ptr())
    {
    }
    template<typename U>
    OwnPtr(OwnPtr<U>&& other)
        : m_ptr(static_cast<T*>(other.leak_ptr()))
    {
    }
    OwnPtr(std::nullptr_t) {};
    ~OwnPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)(0xe1e1e1e1e1e1e1e1);
        else
            m_ptr = (T*)(0xe1e1e1e1);
#endif
    }

    OwnPtr& operator=(OwnPtr&& other)
    {
        if (this != &other) {
            delete m_ptr;
            m_ptr = other.leak_ptr();
        }
        return *this;
    }

    template<typename U>
    OwnPtr& operator=(OwnPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            delete m_ptr;
            m_ptr = other.leak_ptr();
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

    T* leak_ptr()
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

    operator const T*() const { return m_ptr; }
    operator T*() { return m_ptr; }

    operator bool() { return !!m_ptr; }

private:
    T* m_ptr = nullptr;
};

template<class T, class... Args>
inline OwnPtr<T>
make(Args&&... args)
{
    return OwnPtr<T>(new T(AK::forward<Args>(args)...));
}

template<typename T>
struct Traits<OwnPtr<T>> {
    static unsigned hash(const OwnPtr<T>& p) { return (unsigned)p.ptr(); }
    static void dump(const OwnPtr<T>& p) { kprintf("%p", p.ptr()); }
};

}

using AK::make;
using AK::OwnPtr;
