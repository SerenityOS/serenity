#pragma once

#include <AK/Platform.h>

namespace AK {

template <typename T, auto NoErrorValue>
class CONSUMABLE(unknown) Error {
public:
    RETURN_TYPESTATE(unknown)
    Error()
        : t(NoErrorValue)
    {}

    RETURN_TYPESTATE(unknown)
    Error(T t)
        : t(t)
    {}

    RETURN_TYPESTATE(unknown)
    Error(Error&& other)
        : t(move(other.t))
    {
    }

    RETURN_TYPESTATE(unknown)
    Error(const Error& other)
        : t(other.t)
    {
    }

    CALLABLE_WHEN("unknown", "consumed")
    ~Error() {}

    SET_TYPESTATE(consumed)
    bool failed() const {
        return t != NoErrorValue;
    }

    [[deprecated]]
    SET_TYPESTATE(consumed)
    void ignore() {}

    const T& value() const { return t; }

    bool operator==(const Error& o) { return t == o.t; }
    bool operator!=(const Error& o) { return t != o.t; }
    T t;
};

}

using AK::Error;
