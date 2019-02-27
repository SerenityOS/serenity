#pragma once

#include <AK/Assertions.h>
#include <LibC/errno_numbers.h>

enum KSuccessTag { KSuccess };

class KResult {
public:
    explicit KResult(int negative_e) : m_error(negative_e) { ASSERT(negative_e <= 0); }
    KResult(KSuccessTag) : m_error(0) { }
    operator int() const { return m_error; }

    bool is_success() const { return m_error == ESUCCESS; }
    bool is_error() const { return !is_success(); }

private:
    template<typename T> friend class KResultOr;
    KResult() { }

    int m_error { 0 };
};

template<typename T>
class alignas(T) KResultOr {
public:
    KResultOr(KResult error)
        : m_error(error)
        , m_is_error(true)
    { }

    KResultOr(T&& value)
    {
        new (&m_storage) T(move(value));
    }

    ~KResultOr()
    {
        if (!m_is_error)
            value().~T();
    }

    bool is_error() const { return m_is_error; }
    KResult error() const { ASSERT(m_is_error); return m_error; }
    T& value() { ASSERT(!m_is_error); return *reinterpret_cast<T*>(&m_storage); }
    const T& value() const { ASSERT(!m_is_error); return *reinterpret_cast<T*>(&m_storage); }

private:
    char m_storage[sizeof(T)] __attribute__((aligned(sizeof(T))));
    KResult m_error;
    bool m_is_error { false };
};

