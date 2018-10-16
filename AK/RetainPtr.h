#pragma once

namespace std {
typedef decltype(nullptr) nullptr_t;
}

namespace AK {

template<typename T>
inline void retainIfNotNull(T* ptr)
{
    if (ptr)
        ptr->retain();
}

template<typename T>
inline void releaseIfNotNull(T* ptr)
{
    if (ptr)
        ptr->release();
}

template<typename T>
class RetainPtr {
public:
    enum AdoptTag { Adopt };

    RetainPtr() { }
    RetainPtr(const T* ptr) : m_ptr(const_cast<T*>(ptr)) { retainIfNotNull(m_ptr); }
    RetainPtr(T* ptr) : m_ptr(ptr) { retainIfNotNull(m_ptr); }
    RetainPtr(T& object) : m_ptr(&object) { m_ptr->retain(); }
    RetainPtr(AdoptTag, T& object) : m_ptr(&object) { }
    RetainPtr(RetainPtr&& other) : m_ptr(other.leakRef()) { }
    template<typename U> RetainPtr(RetainPtr<U>&& other) : m_ptr(static_cast<T*>(other.leakRef())) { }
    ~RetainPtr() { clear(); }
    RetainPtr(std::nullptr_t) { }

    RetainPtr& operator=(RetainPtr&& other)
    {
        if (this != &other) {
            releaseIfNotNull(m_ptr);
            m_ptr = other.leakRef();
        }
        return *this;
    }

    template<typename U>
    RetainPtr& operator=(RetainPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            releaseIfNotNull(m_ptr);
            m_ptr = other.leakRef();
        }
        return *this;
    }

    RetainPtr& operator=(T* ptr)
    {
        if (m_ptr != ptr)
            releaseIfNotNull(m_ptr);
        m_ptr = ptr;
        retainIfNotNull(m_ptr);
        return *this;
    }

    RetainPtr& operator=(T& object)
    {
        if (m_ptr != &object)
            releaseIfNotNull(m_ptr);
        m_ptr = &object;
        retainIfNotNull(m_ptr);
        return *this;
    }

    RetainPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    RetainPtr copyRef() const
    {
        return RetainPtr(m_ptr);
    }

    void clear()
    {
        releaseIfNotNull(m_ptr);
        m_ptr = nullptr;
    }

    bool operator!() const { return !m_ptr; }

    typedef T* RetainPtr::*UnspecifiedBoolType;
    operator UnspecifiedBoolType() const { return m_ptr ? &RetainPtr::m_ptr : nullptr; }

    T* leakRef()
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

template<typename T>
inline RetainPtr<T> adopt(T& object)
{
    return RetainPtr<T>(RetainPtr<T>::Adopt, object);
}

}

using AK::RetainPtr;
using AK::adopt;

