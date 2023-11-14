/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonValue.h>
#include <AK/NonnullRefPtr.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Variant.h>

namespace GUI {

bool Variant::operator==(Variant const& other) const
{
    return visit([&]<typename T>(T const& own_value) {
        return other.visit(
            [&](T const& other_value) -> bool {
                if constexpr (requires { own_value == other_value; })
                    return own_value == other_value;
                else if constexpr (IsSame<T, GUI::Icon>)
                    return &own_value.impl() == &other_value.impl();
                // FIXME: Figure out if this silly behavior is actually used anywhere, then get rid of it.
                else
                    return to_byte_string() == other.to_byte_string();
            },
            [&](auto const&) {
                // FIXME: Figure out if this silly behavior is actually used anywhere, then get rid of it.
                return to_byte_string() == other.to_byte_string();
            });
    });
}

bool Variant::operator<(Variant const& other) const
{
    return visit([&]<typename T>(T const& own_value) {
        return other.visit(
            [&](T const& other_value) -> bool {
                // FIXME: Maybe compare icons somehow differently?
                if constexpr (IsSame<T, GUI::Icon>)
                    return &own_value.impl() < &other_value.impl();
                // FIXME: Maybe compare bitmaps somehow differently?
                else if constexpr (IsSame<T, NonnullRefPtr<Gfx::Bitmap>>)
                    return own_value.ptr() < other_value.ptr();
                else if constexpr (IsSame<T, NonnullRefPtr<Gfx::Font>>)
                    return own_value->name() < other_value->name();
                else if constexpr (requires { own_value < other_value; })
                    return own_value < other_value;
                // FIXME: Figure out if this silly behavior is actually used anywhere, then get rid of it.
                else
                    return to_byte_string() < other.to_byte_string();
            },
            [&](auto const&) -> bool {
                return to_byte_string() < other.to_byte_string(); // FIXME: Figure out if this silly behavior is actually used anywhere, then get rid of it.
            });
    });
}
}
