/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Object.h>
#include <LibPDF/Value.h>

namespace PDF {

ByteString Value::to_byte_string(int indent) const
{
    return visit(
        [&](Empty const&) -> ByteString {
            // Return type deduction means that we can't use implicit conversions.
            return "<empty>";
        },
        [&](nullptr_t const&) -> ByteString {
            return "null";
        },
        [&](bool const& b) -> ByteString {
            return b ? "true" : "false";
        },
        [&](int const& i) {
            return ByteString::number(i);
        },
        [&](float const& f) {
            return ByteString::number(f);
        },
        [&](Reference const& ref) {
            return ByteString::formatted("{} {} R", ref.as_ref_index(), ref.as_ref_generation_index());
        },
        [&](NonnullRefPtr<Object> const& object) {
            return object->to_byte_string(indent);
        });
}

}
