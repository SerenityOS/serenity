/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
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

        return u64_hash(value.encoded()); // FIXME: Is this the best way to hash pointers, doubles & ints?
    }
    static bool equals(const Value a, const Value b)
    {
        return same_value_zero(a, b);
    }
};

}
