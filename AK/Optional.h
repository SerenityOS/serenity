#pragma once

#include <AK/Assertions.h>

template<typename T>
class alignas(T) Optional {
public:
    Optional() {}

    Optional(T&& value)
        : m_has_value(true)
    {
        new (&m_storage) T(move(value));
    }

    template<typename U>
    Optional(U&& value)
        : m_has_value(true)
    {
        new (&m_storage) T(move(value));
    }

    Optional(Optional&& other)
        : m_has_value(other.m_has_value)
    {
        if (m_has_value) {
            new (&m_storage) T(move(other.value()));
            other.m_has_value = false;
        }
    }

    Optional(const Optional& other)
        : m_has_value(other.m_has_value)
    {
        if (m_has_value) {
            new (&m_storage) T(other.value());
        }
    }

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

    bool has_value() const { return m_has_value; }

    T& value()
    {
        ASSERT(m_has_value);
        return *reinterpret_cast<T*>(&m_storage);
    }

    const T& value() const
    {
        ASSERT(m_has_value);
        return *reinterpret_cast<const T*>(&m_storage);
    }

    T release_value()
    {
        ASSERT(m_has_value);
        T released_value = move(value());
        value().~T();
        return released_value;
    }

private:
    char m_storage[sizeof(T)] __attribute__((aligned(sizeof(T))));
    bool m_has_value { false };
};
