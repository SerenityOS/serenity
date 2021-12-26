#pragma once

#include <AK/LogStream.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class OwnPtr;

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
    RefPtr(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->ref();
    }
    RefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }
    RefPtr(RefPtr&& other)
        : m_ptr(other.leak_ref())
    {
    }
    RefPtr(const NonnullRefPtr<T>& other)
        : m_ptr(const_cast<T*>(other.ptr()))
    {
        ASSERT(m_ptr);
        m_ptr->ref();
    }
    template<typename U>
    RefPtr(const NonnullRefPtr<U>& other)
        : m_ptr(static_cast<T*>(const_cast<U*>(other.ptr())))
    {
        ASSERT(m_ptr);
        m_ptr->ref();
    }
    template<typename U>
    RefPtr(NonnullRefPtr<U>&& other)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
        ASSERT(m_ptr);
    }
    template<typename U>
    RefPtr(RefPtr<U>&& other)
        : m_ptr(static_cast<T*>(other.leak_ref()))
    {
    }
    RefPtr(const RefPtr& other)
        : m_ptr(const_cast<T*>(other.ptr()))
    {
        ref_if_not_null(m_ptr);
    }
    template<typename U>
    RefPtr(const RefPtr<U>& other)
        : m_ptr(static_cast<T*>(const_cast<U*>(other.ptr())))
    {
        ref_if_not_null(m_ptr);
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

    template<typename U>
    RefPtr(const OwnPtr<U>&) = delete;
    template<typename U>
    RefPtr& operator=(const OwnPtr<U>&) = delete;

    template<typename U>
    void swap(RefPtr<U>& other)
    {
        ::swap(m_ptr, other.m_ptr);
    }

    RefPtr& operator=(RefPtr&& other)
    {
        RefPtr tmp = move(other);
        swap(tmp);
        return *this;
    }

    template<typename U>
    RefPtr& operator=(RefPtr<U>&& other)
    {
        RefPtr tmp = move(other);
        swap(tmp);
        return *this;
    }

    template<typename U>
    RefPtr& operator=(NonnullRefPtr<U>&& other)
    {
        RefPtr tmp = move(other);
        swap(tmp);
        ASSERT(m_ptr);
        return *this;
    }

    RefPtr& operator=(const NonnullRefPtr<T>& other)
    {
        RefPtr tmp = other;
        swap(tmp);
        ASSERT(m_ptr);
        return *this;
    }

    template<typename U>
    RefPtr& operator=(const NonnullRefPtr<U>& other)
    {
        RefPtr tmp = other;
        swap(tmp);
        ASSERT(m_ptr);
        return *this;
    }

    RefPtr& operator=(const RefPtr& other)
    {
        RefPtr tmp = other;
        swap(tmp);
        return *this;
    }

    template<typename U>
    RefPtr& operator=(const RefPtr<U>& other)
    {
        RefPtr tmp = other;
        swap(tmp);
        return *this;
    }

    RefPtr& operator=(const T* ptr)
    {
        RefPtr tmp = ptr;
        swap(tmp);
        return *this;
    }

    RefPtr& operator=(const T& object)
    {
        RefPtr tmp = object;
        swap(tmp);
        return *this;
    }

    RefPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    void clear()
    {
        deref_if_not_null(m_ptr);
        m_ptr = nullptr;
    }

    bool operator!() const { return !m_ptr; }

    [[nodiscard]] T* leak_ref()
    {
        return exchange(m_ptr, nullptr);
    }

    T* ptr() { return m_ptr; }
    const T* ptr() const { return m_ptr; }

    T* operator->()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    const T* operator->() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    T& operator*()
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

    const T& operator*() const
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

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
