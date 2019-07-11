#pragma once

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <AK/Types.h>

#ifdef __clang__
#    define CONSUMABLE(initial_state) __attribute__((consumable(initial_state)))
#    define CALLABLE_WHEN(...) __attribute__((callable_when(__VA_ARGS__)))
#    define SET_TYPESTATE(state) __attribute__((set_typestate(state)))
#    define RETURN_TYPESTATE(state) __attribute__((return_typestate(state)))
#else
#    define CONSUMABLE(initial_state)
#    define CALLABLE_WHEN(state)
#    define SET_TYPESTATE(state)
#    define RETURN_TYPESTATE(state)
#endif

namespace AK {

template<typename T>
inline void ref_if_not_null(T* ptr)
{
    if (ptr)
        ptr->ref();
}

template<typename T>
inline void deref_if_not_null(T* ptr)
{
    if (ptr)
        ptr->deref();
}

template<typename T>
class CONSUMABLE(unconsumed) NonnullRefPtr {
public:
    enum AdoptTag {
        Adopt
    };

    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->ref();
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const U& object)
        : m_ptr(&const_cast<T&>(static_cast<const T&>(object)))
    {
        m_ptr->ref();
    }
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(NonnullRefPtr&& other)
        : m_ptr(&other.leak_ref())
    {
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(NonnullRefPtr<U>&& other)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
    }
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const NonnullRefPtr& other)
        : m_ptr(const_cast<T*>(other.ptr()))
    {
        m_ptr->ref();
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const NonnullRefPtr<U>& other)
        : m_ptr(const_cast<T*>(static_cast<const T*>((other.ptr()))))
    {
        m_ptr->ref();
    }
    ~NonnullRefPtr()
    {
        deref_if_not_null(m_ptr);
        m_ptr = nullptr;
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)(0xb0b0b0b0b0b0b0b0);
        else
            m_ptr = (T*)(0xb0b0b0b0);
#endif
    }

    NonnullRefPtr& operator=(const NonnullRefPtr& other)
    {
        if (m_ptr != other.m_ptr) {
            deref_if_not_null(m_ptr);
            m_ptr = const_cast<T*>(other.ptr());
            m_ptr->ref();
        }
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(const NonnullRefPtr<U>& other)
    {
        if (m_ptr != other.m_ptr) {
            deref_if_not_null(m_ptr);
            m_ptr = const_cast<T*>(static_cast<const T*>(other.ptr()));
            m_ptr->ref();
        }
        return *this;
    }

    NonnullRefPtr& operator=(NonnullRefPtr&& other)
    {
        if (this != &other) {
            deref_if_not_null(m_ptr);
            m_ptr = &other.leak_ref();
        }
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(NonnullRefPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            deref_if_not_null(m_ptr);
            m_ptr = &other.leak_ref();
        }
        return *this;
    }

    NonnullRefPtr& operator=(T& object)
    {
        if (m_ptr != &object)
            deref_if_not_null(m_ptr);
        m_ptr = &object;
        m_ptr->ref();
        return *this;
    }

    CALLABLE_WHEN(unconsumed)
    NonnullRefPtr copy_ref() const
    {
        return NonnullRefPtr(*m_ptr);
    }

    CALLABLE_WHEN(unconsumed)
    SET_TYPESTATE(consumed)
    T& leak_ref()
    {
        ASSERT(m_ptr);
        T* leakedPtr = m_ptr;
        m_ptr = nullptr;
        return *leakedPtr;
    }

    CALLABLE_WHEN(unconsumed)
    T* ptr()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    const T* ptr() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    T* operator->()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    const T* operator->() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    T& operator*()
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    const T& operator*() const
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    operator T*()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    operator const T*() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    operator T&()
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    operator const T&() const
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

private:
    NonnullRefPtr() = delete;

    T* m_ptr { nullptr };
};

template<typename T>
inline NonnullRefPtr<T> adopt(T& object)
{
    return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, object);
}

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const NonnullRefPtr<T>& value)
{
    return stream << value.ptr();
}

}

using AK::adopt;
using AK::NonnullRefPtr;
