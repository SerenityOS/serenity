/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtr.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Variant.h>

namespace GUI {

Variant::Variant(JsonValue const& value)
{
    *this = value;
}

Variant& Variant::operator=(JsonValue const& value)
{
    if (value.is_null())
        return *this;

    if (value.is_i32()) {
        set(value.as_i32());
        return *this;
    }

    if (value.is_u32()) {
        set(value.as_u32());
        return *this;
    }

    if (value.is_i64()) {
        set(value.as_i64());
        return *this;
    }

    if (value.is_u64()) {
        set(value.as_u64());
        return *this;
    }

    if (value.is_string()) {
        set(value.as_string());
        return *this;
    }

    if (value.is_bool()) {
        set(Detail::Boolean { value.as_bool() });
        return *this;
    }

    VERIFY_NOT_REACHED();
}

bool Variant::operator==(Variant const& other) const
{
    return visit([&]<typename T>(T const& own_value) {
        return other.visit(
            [&](T const& other_value) -> bool {
                if constexpr (requires { own_value == other_value; })
                    return own_value == other_value;
                else if constexpr (IsSame<T, GUI::Icon>)
                    return &own_value.impl() == &other_value.impl();
                // FIXME: Figure out if this silly behaviour is actually used anywhere, then get rid of it.
                else
                    return to_string() == other.to_string();
            },
            [&](auto const&) {
                // FIXME: Figure out if this silly behaviour is actually used anywhere, then get rid of it.
                return to_string() == other.to_string();
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
                // FIXME: Figure out if this silly behaviour is actually used anywhere, then get rid of it.
                else
                    return to_string() < other.to_string();
            },
            [&](auto const&) -> bool {
                return to_string() < other.to_string(); // FIXME: Figure out if this silly behaviour is actually used anywhere, then get rid of it.
            });
    });
}
}
