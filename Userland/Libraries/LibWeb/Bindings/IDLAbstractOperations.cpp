/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>

namespace Web::Bindings::IDL {

// https://webidl.spec.whatwg.org/#is-an-array-index
bool is_an_array_index(JS::GlobalObject& global_object, JS::PropertyName const& property_name)
{
    // 1. If Type(P) is not String, then return false.
    if (!property_name.is_number())
        return false;

    // 2. Let index be ! CanonicalNumericIndexString(P).
    auto index = JS::canonical_numeric_index_string(global_object, property_name);

    // 3. If index is undefined, then return false.
    if (index.is_undefined())
        return false;

    // 4. If IsInteger(index) is false, then return false.
    // NOTE: IsInteger is the old name of IsIntegralNumber.
    if (!index.is_integral_number())
        return false;

    // 5. If index is −0, then return false.
    if (index.is_negative_zero())
        return false;

    // FIXME: I'm not sure if this is correct.
    auto index_as_double = index.as_double();

    // 6. If index < 0, then return false.
    if (index_as_double < 0)
        return false;

    // 7. If index ≥ 2 ** 32 − 1, then return false.
    // Note: 2 ** 32 − 1 is the maximum array length allowed by ECMAScript.
    if (index_as_double >= NumericLimits<u32>::max())
        return false;

    // 8. Return true.
    return true;
}

}
