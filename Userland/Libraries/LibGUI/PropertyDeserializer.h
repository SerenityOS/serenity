/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonValue.h>
#include <LibGfx/Forward.h>

namespace GUI {

template<typename T>
struct PropertyDeserializer {
    ErrorOr<T> operator()(JsonValue const& value) const;
};

template<Integral T>
requires(!IsSame<T, bool>)
struct PropertyDeserializer<T> {
    ErrorOr<T> operator()(JsonValue const& value) const
    {
        if (!value.is_integer<T>())
            return Error::from_string_literal("Value is either not an integer or out of range for requested type");
        return value.as_integer<T>();
    }
};

}
