#pragma once

#include <AK/StdLibExtras.h>

// This is a stopgap pointer. It's not meant to stick around forever.

template<typename T>
class ObjectPtr {
public:
    ObjectPtr() {}
    ObjectPtr(T* ptr)
        : m_ptr(ptr)
    {
    }
    ObjectPtr(T& ptr)
        : m_ptr(&ptr)
    {
    }
    ~ObjectPtr()
    {
        clear();
    }

    void clear()
    {
        if (m_ptr && !m_ptr->parent())
            delete m_ptr;
        m_ptr = nullptr;
    }

    ObjectPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    template<typename U>
    ObjectPtr(U* ptr)
        : m_ptr(static_cast<T*>(ptr))
    {
    }

    ObjectPtr(const ObjectPtr& other)
        : m_ptr(other.m_ptr)
    {
    }

    template<typename U>
    ObjectPtr(const ObjectPtr<U>& other)
        : m_ptr(static_cast<T*>(const_cast<ObjectPtr<U>&>(other).ptr()))
    {
    }

    ObjectPtr(ObjectPtr&& other)
    {
        m_ptr = other.leak_ptr();
    }

    template<typename U>
    ObjectPtr(const ObjectPtr<U>&& other)
    {
        m_ptr = static_cast<T*>(const_cast<ObjectPtr<U>&>(other).leak_ptr());
    }

    ObjectPtr& operator=(const ObjectPtr& other)
    {
        if (this != &other) {
            clear();
            m_ptr = other.m_ptr;
        }
        return *this;
    }

    ObjectPtr& operator=(ObjectPtr&& other)
    {
        if (this != &other) {
            clear();
            m_ptr = exchange(other.m_ptr, nullptr);
        }
        return *this;
    }

    T* operator->() { return m_ptr; }
    const T* operator->() const { return m_ptr; }

    operator T*() { return m_ptr; }
    operator const T*() const { return m_ptr; }

    T& operator*() { return *m_ptr; }
    const T& operator*() const { return *m_ptr; }

    T* ptr() const { return m_ptr; }
    T* leak_ptr() { return exchange(m_ptr, nullptr); }

    operator bool() const { return !!m_ptr; }

private:
    T* m_ptr { nullptr };
};
