/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Object.h>
#include <LibPDF/Value.h>

namespace PDF {

DeprecatedString Value::to_string(int indent) const
{
    return visit(
        [&](Empty const&) -> DeprecatedString {
            // Return type deduction means that we can't use implicit conversions.
            return "<empty>";
        },
        [&](std::nullptr_t const&) -> DeprecatedString {
            return "null";
        },
        [&](bool const& b) -> DeprecatedString {
            return b ? "true" : "false";
        },
        [&](int const& i) {
            return DeprecatedString::number(i);
        },
        [&](float const& f) {
            return DeprecatedString::number(f);
        },
        [&](Reference const& ref) {
            return DeprecatedString::formatted("{} {} R", ref.as_ref_index(), ref.as_ref_generation_index());
        },
        [&](NonnullRefPtr<Object> const& object) {
            return object->to_string(indent);
        });
}

}
