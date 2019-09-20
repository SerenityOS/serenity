#pragma once

#include <AK/StdLibExtras.h>

// This is a stopgap pointer. It's not meant to stick around forever.

template<typename T>
class ObjectPtr {
public:
    ObjectPtr() {}
    ObjectPtr(T* ptr) : m_ptr(ptr) {}
    ~ObjectPtr()
    {
        if (m_ptr && !m_ptr->parent())
            delete m_ptr;
    }

    ObjectPtr(const ObjectPtr& other)
        : m_ptr(other.m_ptr)
    {
    }

    ObjectPtr(ObjectPtr&& other)
    {
        m_ptr = exchange(other.m_ptr, nullptr);
    }

    ObjectPtr& operator=(const ObjectPtr& other)
    {
        m_ptr = other.m_ptr;
        return *this;
    }

    ObjectPtr& operator=(ObjectPtr&& other)
    {
        if (this != &other) {
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

private:
    T* m_ptr { nullptr };
};
