#pragma once

#include <AK/Retained.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class RetainPtr {
public:
    enum AdoptTag {
        Adopt
    };

    RetainPtr() {}
    RetainPtr(const T* ptr)
        : m_ptr(const_cast<T*>(ptr))
    {
        retain_if_not_null(m_ptr);
    }
    RetainPtr(T* ptr)
        : m_ptr(ptr)
    {
        retain_if_not_null(m_ptr);
    }
    RetainPtr(T& object)
        : m_ptr(&object)
    {
        m_ptr->retain();
    }
    RetainPtr(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->retain();
    }
    RetainPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }
    RetainPtr(RetainPtr& other)
        : m_ptr(other.copy_ref().leak_ref())
    {
    }
    RetainPtr(RetainPtr&& other)
        : m_ptr(other.leak_ref())
    {
    }
    template<typename U>
    RetainPtr(Retained<U>&& other)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
    }
    template<typename U>
    RetainPtr(RetainPtr<U>&& other)
        : m_ptr(static_cast<T*>(other.leak_ref()))
    {
    }
    RetainPtr(const RetainPtr& other)
        : m_ptr(const_cast<RetainPtr&>(other).copy_ref().leak_ref())
    {
    }
    template<typename U>
    RetainPtr(const RetainPtr<U>& other)
        : m_ptr(const_cast<RetainPtr<U>&>(other).copy_ref().leak_ref())
    {
    }
    ~RetainPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)(0xe0e0e0e0e0e0e0e0);
        else
            m_ptr = (T*)(0xe0e0e0e0);
#endif
    }
    RetainPtr(std::nullptr_t) {}

    RetainPtr& operator=(RetainPtr&& other)
    {
        if (this != &other) {
            release_if_not_null(m_ptr);
            m_ptr = other.leak_ref();
        }
        return *this;
    }

    template<typename U>
    RetainPtr& operator=(RetainPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            release_if_not_null(m_ptr);
            m_ptr = other.leak_ref();
        }
        return *this;
    }

    template<typename U>
    RetainPtr& operator=(Retained<U>&& other)
    {
        release_if_not_null(m_ptr);
        m_ptr = &other.leak_ref();
        return *this;
    }

    template<typename U>
    RetainPtr& operator=(const Retained<U>& other)
    {
        if (m_ptr != other.ptr())
            release_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(other.ptr());
        ASSERT(m_ptr);
        retain_if_not_null(m_ptr);
        return *this;
    }

    template<typename U>
    RetainPtr& operator=(const RetainPtr<U>& other)
    {
        if (m_ptr != other.ptr())
            release_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(other.ptr());
        retain_if_not_null(m_ptr);
        return *this;
    }

    RetainPtr& operator=(const T* ptr)
    {
        if (m_ptr != ptr)
            release_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(ptr);
        retain_if_not_null(m_ptr);
        return *this;
    }

    RetainPtr& operator=(const T& object)
    {
        if (m_ptr != &object)
            release_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(&object);
        retain_if_not_null(m_ptr);
        return *this;
    }

    RetainPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    RetainPtr copy_ref() const
    {
        return RetainPtr(m_ptr);
    }

    void clear()
    {
        release_if_not_null(m_ptr);
        m_ptr = nullptr;
    }

    bool operator!() const { return !m_ptr; }

    T* leak_ref()
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

    bool operator==(std::nullptr_t) const { return !m_ptr; }
    bool operator!=(std::nullptr_t) const { return m_ptr; }

    bool operator==(const RetainPtr& other) const { return m_ptr == other.m_ptr; }
    bool operator!=(const RetainPtr& other) const { return m_ptr != other.m_ptr; }

    bool operator==(RetainPtr& other) { return m_ptr == other.m_ptr; }
    bool operator!=(RetainPtr& other) { return m_ptr != other.m_ptr; }

    bool operator==(const T* other) const { return m_ptr == other; }
    bool operator!=(const T* other) const { return m_ptr != other; }

    bool operator==(T* other) { return m_ptr == other; }
    bool operator!=(T* other) { return m_ptr != other; }

    bool is_null() const { return !m_ptr; }

private:
    T* m_ptr = nullptr;
};

}

using AK::RetainPtr;
