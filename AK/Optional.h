#pragma once

#include <AK/Assertions.h>
#include <AK/Platform.h>

template<typename T>
class CONSUMABLE(unknown) alignas(T) Optional {
public:
    RETURN_TYPESTATE(unknown)
    Optional() {}

    RETURN_TYPESTATE(unknown)
    Optional(T&& value)
        : m_has_value(true)
    {
        new (&m_storage) T(move(value));
    }

    template<typename U>
    RETURN_TYPESTATE(unknown)
    Optional(U&& value)
        : m_has_value(true)
    {
        new (&m_storage) T(move(value));
    }

    RETURN_TYPESTATE(unknown)
    Optional(Optional&& other)
        : m_has_value(other.m_has_value)
    {
        if (m_has_value) {
            new (&m_storage) T(move(other.value_without_consume_state()));
            other.m_has_value = false;
        }
    }

    RETURN_TYPESTATE(unknown)
    Optional(const Optional& other)
        : m_has_value(other.m_has_value)
    {
        if (m_has_value) {
            new (&m_storage) T(other.value_without_consume_state());
        }
    }

    RETURN_TYPESTATE(unknown)
    Optional& operator=(const Optional& other)
    {
        if (this != &other) {
            clear();
            m_has_value = other.m_has_value;
            if (m_has_value) {
                new (&m_storage) T(other.value());
            }
        }
        return *this;
    }

    RETURN_TYPESTATE(unknown)
    Optional& operator=(Optional&& other)
    {
        if (this != &other) {
            clear();
            m_has_value = other.m_has_value;
            if (m_has_value) {
                new (&m_storage) T(other.release_value());
            }
        }
        return *this;
    }

    ~Optional()
    {
        clear();
    }

    void clear()
    {
        if (m_has_value) {
            value().~T();
            m_has_value = false;
        }
    }

    SET_TYPESTATE(consumed)
    bool has_value() const { return m_has_value; }

    CALLABLE_WHEN(consumed)
    T& value()
    {
        ASSERT(m_has_value);
        return *reinterpret_cast<T*>(&m_storage);
    }

    CALLABLE_WHEN(consumed)
    const T& value() const
    {
        return value_without_consume_state();
    }

    CALLABLE_WHEN(consumed)
    T release_value()
    {
        ASSERT(m_has_value);
        T released_value = move(value());
        value().~T();
        return released_value;
    }

    T value_or(const T& fallback) const
    {
        if (m_has_value)
            return value();
        return fallback;
    }

    operator bool() const { return m_has_value; }

private:
    // Call when we don't want to alter the consume state
    const T& value_without_consume_state() const
    {
        ASSERT(m_has_value);
        return *reinterpret_cast<const T*>(&m_storage);
    }
    char m_storage[sizeof(T)];
    bool m_has_value { false };
};
