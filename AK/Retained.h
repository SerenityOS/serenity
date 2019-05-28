#pragma once

#include <AK/Assertions.h>
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
inline void retain_if_not_null(T* ptr)
{
    if (ptr)
        ptr->retain();
}

template<typename T>
inline void release_if_not_null(T* ptr)
{
    if (ptr)
        ptr->release();
}

template<typename T>
class CONSUMABLE(unconsumed) Retained {
public:
    enum AdoptTag {
        Adopt
    };

    RETURN_TYPESTATE(unconsumed)
    Retained(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->retain();
    }
    RETURN_TYPESTATE(unconsumed)
    Retained(T& object)
        : m_ptr(&object)
    {
        m_ptr->retain();
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    Retained(U& object)
        : m_ptr(&static_cast<T&>(object))
    {
        m_ptr->retain();
    }
    RETURN_TYPESTATE(unconsumed)
    Retained(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }
    RETURN_TYPESTATE(unconsumed)
    Retained(Retained& other)
        : m_ptr(&other.copy_ref().leak_ref())
    {
    }
    RETURN_TYPESTATE(unconsumed)
    Retained(Retained&& other)
        : m_ptr(&other.leak_ref())
    {
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    Retained(Retained<U>&& other)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
    }
    RETURN_TYPESTATE(unconsumed)
    Retained(const Retained& other)
        : m_ptr(&const_cast<Retained&>(other).copy_ref().leak_ref())
    {
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    Retained(const Retained<U>& other)
        : m_ptr(&const_cast<Retained<U>&>(other).copy_ref().leak_ref())
    {
    }
    ~Retained()
    {
        release_if_not_null(m_ptr);
        m_ptr = nullptr;
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)(0xb0b0b0b0b0b0b0b0);
        else
            m_ptr = (T*)(0xb0b0b0b0);
#endif
    }

    CALLABLE_WHEN(unconsumed)
    Retained& operator=(Retained&& other)
    {
        if (this != &other) {
            release_if_not_null(m_ptr);
            m_ptr = &other.leak_ref();
        }
        return *this;
    }

    template<typename U>
    CALLABLE_WHEN(unconsumed)
    Retained& operator=(Retained<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            release_if_not_null(m_ptr);
            m_ptr = &other.leak_ref();
        }
        return *this;
    }

    CALLABLE_WHEN(unconsumed)
    Retained& operator=(T& object)
    {
        if (m_ptr != &object)
            release_if_not_null(m_ptr);
        m_ptr = &object;
        m_ptr->retain();
        return *this;
    }

    CALLABLE_WHEN(unconsumed)
    Retained copy_ref() const
    {
        return Retained(*m_ptr);
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

private:
    Retained() {}

    T* m_ptr { nullptr };
};

template<typename T>
inline Retained<T> adopt(T& object)
{
    return Retained<T>(Retained<T>::Adopt, object);
}

}

using AK::adopt;
using AK::Retained;
