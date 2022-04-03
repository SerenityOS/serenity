/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020-2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>

namespace JS {
struct ValueTraits : public Traits<Value> {
    static unsigned hash(Value value)
    {
        VERIFY(!value.is_empty());
        if (value.is_string())
            return value.as_string().string().hash();

        if (value.is_bigint())
            return value.as_bigint().big_integer().hash();

        if (value.is_negative_zero())
            value = Value(0);
        // In the IEEE 754 standard a NaN value is encoded as any value from 0x7ff0000000000001 to 0x7fffffffffffffff,
        // with the least significant bits (referred to as the 'payload') carrying some kind of diagnostic information
        // indicating the source of the NaN. Since ECMA262 does not differentiate between different kinds of NaN values,
        // Sets and Maps must not differentiate between them either.
        // This is achieved by replacing any NaN value by a canonical qNaN.
        else if (value.is_nan())
            value = js_nan();

        return u64_hash(value.encoded()); // FIXME: Is this the best way to hash pointers, doubles & ints?
    }
    static bool equals(const Value a, const Value b)
    {
        return same_value_zero(a, b);
    }
};

}
