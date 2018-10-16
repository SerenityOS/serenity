#pragma once

#include "types.h"

#define SANITIZE_REFPTR

template<typename T> class RefPtr;
template<typename T> RefPtr<T> adoptRef(T*);

template<typename T>
class RefPtr {
public:
    RefPtr() { }
    RefPtr(T* ptr) : m_ptr(ptr) { refIfNotNull(m_ptr); }

    ~RefPtr()
    {
        derefIfNotNull(m_ptr);
#ifdef SANITIZE_REFPTR
        m_ptr = (T*)(0xeeeeeeee);
#endif
    }

    RefPtr(RefPtr&& other)
        : m_ptr(other.leakPtr())
    {
    }

    RefPtr& operator=(RefPtr&& other)
    {
        if (this == &other)
            return *this;
        m_ptr = other.leakPtr();
        return *this;
    }

    template<typename U>
    RefPtr(RefPtr<U>&& other)
        : m_ptr(static_cast<T*>(other.leakPtr()))
    {
    }

    template<typename U>
    RefPtr& operator=(RefPtr<U>&& other)
    {
        if (this == &other)
            return *this;
        m_ptr = static_cast<T*>(other.leakPtr());
        return *this;
    }

    RefPtr(const RefPtr& other)
        : m_ptr(other.m_ptr)
    {
        refIfNotNull(m_ptr);
    }

    RefPtr& operator=(const RefPtr& other)
    {
        if (this == &other)
            return *this;
        m_ptr = other.m_ptr;
        refIfNotNull(m_ptr);
        return *this;
    }

    T* ptr() { return m_ptr; }
    const T* ptr() const { return m_ptr; }
    T* operator->() { return m_ptr; }
    const T* operator->() const { return m_ptr; }
    T& operator*() { return *m_ptr; }
    const T& operator*() const { return *m_ptr; }
    operator bool() const { return m_ptr; }

    T* leakPtr()
    {
        T* ptr = m_ptr;
        m_ptr = nullptr;
        return ptr;
    }

private:
    template<typename U> friend class RefPtr;
    friend RefPtr adoptRef<T>(T*);

    enum AdoptTag { Adopt };
    RefPtr(AdoptTag, T* ptr) : m_ptr(ptr) { }

    inline void refIfNotNull(T* ptr) { if (ptr) ptr->ref(); }
    inline void derefIfNotNull(T* ptr) { if (ptr) ptr->deref(); }

    T* m_ptr { nullptr };
};

template<typename T>
inline RefPtr<T> adoptRef(T* ptr)
{
    ASSERT(ptr->refCount() == 1);
    return RefPtr<T>(RefPtr<T>::Adopt, ptr);
}
