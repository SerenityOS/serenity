#pragma once

#include <AK/Assertions.h>
#include <AK/Platform.h>
#include <AK/Optional.h>

namespace AK {

template<typename T, typename E>
class CONSUMABLE(unknown) Result {
public:
    RETURN_TYPESTATE(unknown)
    Result(const T& res)
        : m_result(res)
    {}

    RETURN_TYPESTATE(unknown)
    Result(const E& error)
        : m_error(error)
    {
    }

    RETURN_TYPESTATE(unknown)
    Result(const T& res, const E& error)
        : m_result(res)
        , m_error(error)
    {
    }

    RETURN_TYPESTATE(unknown)
    Result(Result&& other)
        : m_result(move(other.m_result))
        , m_error(move(other.m_error))
    {
    }

    RETURN_TYPESTATE(unknown)
    Result(Result& other)
        : m_result(other.m_result)
        , m_error(other.m_error)
    {
    }

    CALLABLE_WHEN("unknown", "consumed")
    ~Result()
    {}

    CALLABLE_WHEN(consumed)
    T& unwrap() {
        return m_result.value();
    }

    CALLABLE_WHEN(consumed)
    E& error() {
        return m_error.value();
    }

    bool has_error() const {
        return m_error.has_value();
    }
    bool has_value() const {
        return m_result.has_value();
    }

    SET_TYPESTATE(consumed)
    bool failed() const {
        return m_error.value().failed();
    }

private:
    Optional<T> m_result;
    Optional<E> m_error;
};

}

using AK::Result;

