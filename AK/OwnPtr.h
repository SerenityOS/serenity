#pragma once

#include <AK/NonnullOwnPtr.h>

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
    OwnPtr(NonnullOwnPtr<U>&& other)
        : m_ptr(static_cast<T*>(other.leak_ptr()))
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

    OwnPtr(const OwnPtr&) = delete;
    template<typename U>
    OwnPtr(const OwnPtr<U>&) = delete;
    OwnPtr& operator=(const OwnPtr&) = delete;
    template<typename U>
    OwnPtr& operator=(const OwnPtr<U>&) = delete;

    template<typename U>
    OwnPtr(const NonnullOwnPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const NonnullOwnPtr<U>&) = delete;
    template<typename U>
    OwnPtr(const RefPtr<U>&) = delete;
    template<typename U>
    OwnPtr(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    OwnPtr(const WeakPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const RefPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const WeakPtr<U>&) = delete;

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

    template<typename U>
    OwnPtr& operator=(NonnullOwnPtr<U>&& other)
    {
        ASSERT(m_ptr != other.ptr());
        delete m_ptr;
        m_ptr = other.leak_ptr();
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
        T* leaked_ptr = m_ptr;
        m_ptr = nullptr;
        return leaked_ptr;
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

private:
    T* m_ptr = nullptr;
};

template<typename T>
struct Traits<OwnPtr<T>> : public GenericTraits<OwnPtr<T>> {
    static unsigned hash(const OwnPtr<T>& p) { return (unsigned)p.ptr(); }
    static void dump(const OwnPtr<T>& p) { kprintf("%p", p.ptr()); }
    static bool equals(const OwnPtr<T>& a, const OwnPtr<T>& b) { return a.ptr() == b.ptr(); }
};

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const OwnPtr<T>& value)
{
    return stream << value.ptr();
}

}

using AK::OwnPtr;
