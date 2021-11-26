/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Object.h>
#include <LibPDF/Value.h>

namespace PDF {

String Value::to_string(int indent) const
{
    return visit(
        [&](Empty const&) -> String {
            // Return type deduction means that we can't use implicit conversions.
            return "<empty>";
        },
        [&](std::nullptr_t const&) -> String {
            return "null";
        },
        [&](bool const& b) -> String {
            return b ? "true" : "false";
        },
        [&](int const& i) {
            return String::number(i);
        },
        [&](float const& f) {
            return String::number(f);
        },
        [&](Reference const& ref) {
            return String::formatted("{} {} R", ref.as_ref_index(), ref.as_ref_generation_index());
        },
        [&](NonnullRefPtr<Object> const& object) {
            return object->to_string(indent);
        });
}

}
