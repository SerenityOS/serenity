#pragma once

#include <AK/LogStream.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class RefPtr {
public:
    enum AdoptTag {
        Adopt
    };

    RefPtr() {}
    RefPtr(const T* ptr)
        : m_ptr(const_cast<T*>(ptr))
    {
        ref_if_not_null(m_ptr);
    }
    RefPtr(T* ptr)
        : m_ptr(ptr)
    {
        ref_if_not_null(m_ptr);
    }
    RefPtr(T& object)
        : m_ptr(&object)
    {
        m_ptr->ref();
    }
    RefPtr(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->ref();
    }
    RefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }
    RefPtr(RefPtr& other)
        : m_ptr(other.copy_ref().leak_ref())
    {
    }
    RefPtr(RefPtr&& other)
        : m_ptr(other.leak_ref())
    {
    }
    template<typename U>
    RefPtr(NonnullRefPtr<U>&& other)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
    }
    template<typename U>
    RefPtr(RefPtr<U>&& other)
        : m_ptr(static_cast<T*>(other.leak_ref()))
    {
    }
    RefPtr(const RefPtr& other)
        : m_ptr(const_cast<RefPtr&>(other).copy_ref().leak_ref())
    {
    }
    template<typename U>
    RefPtr(const RefPtr<U>& other)
        : m_ptr(const_cast<RefPtr<U>&>(other).copy_ref().leak_ref())
    {
    }
    ~RefPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)(0xe0e0e0e0e0e0e0e0);
        else
            m_ptr = (T*)(0xe0e0e0e0);
#endif
    }
    RefPtr(std::nullptr_t) {}

    RefPtr& operator=(RefPtr&& other)
    {
        if (this != &other) {
            deref_if_not_null(m_ptr);
            m_ptr = other.leak_ref();
        }
        return *this;
    }

    template<typename U>
    RefPtr& operator=(RefPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            deref_if_not_null(m_ptr);
            m_ptr = other.leak_ref();
        }
        return *this;
    }

    template<typename U>
    RefPtr& operator=(NonnullRefPtr<U>&& other)
    {
        deref_if_not_null(m_ptr);
        m_ptr = &other.leak_ref();
        return *this;
    }

    template<typename U>
    RefPtr& operator=(const NonnullRefPtr<U>& other)
    {
        if (m_ptr != other.ptr())
            deref_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(other.ptr());
        ASSERT(m_ptr);
        ref_if_not_null(m_ptr);
        return *this;
    }

    template<typename U>
    RefPtr& operator=(const RefPtr<U>& other)
    {
        if (m_ptr != other.ptr())
            deref_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(other.ptr());
        ref_if_not_null(m_ptr);
        return *this;
    }

    RefPtr& operator=(const T* ptr)
    {
        if (m_ptr != ptr)
            deref_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(ptr);
        ref_if_not_null(m_ptr);
        return *this;
    }

    RefPtr& operator=(const T& object)
    {
        if (m_ptr != &object)
            deref_if_not_null(m_ptr);
        m_ptr = const_cast<T*>(&object);
        ref_if_not_null(m_ptr);
        return *this;
    }

    RefPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    RefPtr copy_ref() const
    {
        return RefPtr(m_ptr);
    }

    void clear()
    {
        deref_if_not_null(m_ptr);
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

    bool operator==(const RefPtr& other) const { return m_ptr == other.m_ptr; }
    bool operator!=(const RefPtr& other) const { return m_ptr != other.m_ptr; }

    bool operator==(RefPtr& other) { return m_ptr == other.m_ptr; }
    bool operator!=(RefPtr& other) { return m_ptr != other.m_ptr; }

    bool operator==(const T* other) const { return m_ptr == other; }
    bool operator!=(const T* other) const { return m_ptr != other; }

    bool operator==(T* other) { return m_ptr == other; }
    bool operator!=(T* other) { return m_ptr != other; }

    bool is_null() const { return !m_ptr; }

private:
    T* m_ptr = nullptr;
};

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const RefPtr<T>& value)
{
    return stream << value.ptr();
}

}

using AK::RefPtr;
