/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
