/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Concepts.h>
#include <LibGUI/Icon.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

namespace Detail {
struct Boolean {
    bool value;
};
using VariantUnderlyingType = AK::Variant<Empty, Boolean, float, double, i32, i64, u32, u64, ByteString, Color, Gfx::IntPoint, Gfx::IntSize, Gfx::IntRect, Gfx::TextAlignment, Gfx::WindowThemeProvider, Gfx::ColorRole, Gfx::AlignmentRole, Gfx::WindowThemeRole, Gfx::FlagRole, Gfx::MetricRole, Gfx::PathRole, NonnullRefPtr<Gfx::Bitmap const>, NonnullRefPtr<Gfx::Font const>, GUI::Icon>;
}

class Variant : public Detail::VariantUnderlyingType {
public:
    using Detail::VariantUnderlyingType::Variant;
    using Detail::VariantUnderlyingType::operator=;

    Variant(bool v)
        : Variant(Detail::Boolean { v })
    {
    }
    Variant& operator=(bool v)
    {
        set(Detail::Boolean { v });
        return *this;
    }

    template<typename T>
    Variant(T&& value)
    requires(IsConstructible<ByteString, T>)
        : Variant(ByteString(forward<T>(value)))
    {
    }
    template<typename T>
    Variant& operator=(T&& v)
    requires(IsConstructible<ByteString, T>)
    {
        set(ByteString(v));
        return *this;
    }

    template<OneOfIgnoringCV<Gfx::Bitmap, Gfx::Font> T>
    Variant(T const& value)
        : Variant(NonnullRefPtr<RemoveCV<T> const>(value))
    {
    }
    template<OneOfIgnoringCV<Gfx::Bitmap, Gfx::Font> T>
    Variant& operator=(T&& value)
    {
        set(NonnullRefPtr<RemoveCV<T>>(forward<T>(value)));
        return *this;
    }

    ~Variant() = default;

    bool is_valid() const { return !has<Empty>(); }
    bool is_bool() const { return has<Detail::Boolean>(); }
    bool is_i32() const { return has<i32>(); }
    bool is_i64() const { return has<i64>(); }
    bool is_u32() const { return has<u32>(); }
    bool is_u64() const { return has<u64>(); }
    bool is_float() const { return has<float>(); }
    bool is_double() const { return has<double>(); }
    bool is_string() const { return has<ByteString>(); }
    bool is_bitmap() const { return has<NonnullRefPtr<Gfx::Bitmap const>>(); }
    bool is_color() const { return has<Color>(); }
    bool is_icon() const { return has<GUI::Icon>(); }
    bool is_point() const { return has<Gfx::IntPoint>(); }
    bool is_size() const { return has<Gfx::IntSize>(); }
    bool is_rect() const { return has<Gfx::IntRect>(); }
    bool is_font() const { return has<NonnullRefPtr<Gfx::Font const>>(); }
    bool is_text_alignment() const { return has<Gfx::TextAlignment>(); }
    bool is_color_role() const { return has<Gfx::ColorRole>(); }
    bool is_alignment_role() const { return has<Gfx::AlignmentRole>(); }
    bool is_flag_role() const { return has<Gfx::FlagRole>(); }
    bool is_metric_role() const { return has<Gfx::MetricRole>(); }
    bool is_path_role() const { return has<Gfx::PathRole>(); }
    bool is_window_theme_role() const { return has<Gfx::WindowThemeRole>(); }

    bool as_bool() const { return get<Detail::Boolean>().value; }

    bool to_bool() const
    {
        return visit(
            [](Empty) { return false; },
            [](Detail::Boolean v) { return v.value; },
            [](Integral auto v) { return v != 0; },
            [](Gfx::IntPoint const& v) { return !v.is_zero(); },
            [](OneOf<Gfx::IntRect, Gfx::IntSize> auto const& v) { return !v.is_empty(); },
            [](Enum auto const&) { return true; },
            [](OneOf<float, double, ByteString, Color, NonnullRefPtr<Gfx::Font const>, NonnullRefPtr<Gfx::Bitmap const>, GUI::Icon> auto const&) { return true; });
    }

    i32 as_i32() const { return get<i32>(); }
    i64 as_i64() const { return get<i64>(); }
    u32 as_u32() const { return get<u32>(); }
    u64 as_u64() const { return get<u64>(); }

    template<Integral T>
    T to_integer() const
    {
        return visit(
            [](Empty) -> T { return 0; },
            [](Integral auto v) { return static_cast<T>(v); },
            [](FloatingPoint auto v) { return (T)v; },
            [](Detail::Boolean v) -> T { return v.value ? 1 : 0; },
            [](ByteString const& v) {
                return v.to_number<T>().value_or(0);
            },
            [](Enum auto const&) -> T { return 0; },
            [](OneOf<Gfx::IntPoint, Gfx::IntRect, Gfx::IntSize, Color, NonnullRefPtr<Gfx::Font const>, NonnullRefPtr<Gfx::Bitmap const>, GUI::Icon> auto const&) -> T { return 0; });
    }

    i32 to_i32() const { return to_integer<i32>(); }
    i64 to_i64() const { return to_integer<i64>(); }
    float as_float() const { return get<float>(); }

    float as_float_or(float fallback) const
    {
        if (auto const* p = get_pointer<float>())
            return *p;
        return fallback;
    }

    double as_double() const { return get<double>(); }
    Gfx::IntPoint as_point() const { return get<Gfx::IntPoint>(); }
    Gfx::IntSize as_size() const { return get<Gfx::IntSize>(); }
    Gfx::IntRect as_rect() const { return get<Gfx::IntRect>(); }
    ByteString as_string() const { return get<ByteString>(); }
    Gfx::Bitmap const& as_bitmap() const { return *get<NonnullRefPtr<Gfx::Bitmap const>>(); }
    GUI::Icon as_icon() const { return get<GUI::Icon>(); }
    Color as_color() const { return get<Color>(); }
    Gfx::Font const& as_font() const { return *get<NonnullRefPtr<Gfx::Font const>>(); }

    Gfx::WindowThemeProvider to_window_theme_provider(Gfx::WindowThemeProvider default_value) const
    {
        if (auto const* p = get_pointer<Gfx::WindowThemeProvider>())
            return *p;
        return default_value;
    }

    Gfx::TextAlignment to_text_alignment(Gfx::TextAlignment default_value) const
    {
        if (auto const* p = get_pointer<Gfx::TextAlignment>())
            return *p;
        return default_value;
    }

    Gfx::ColorRole to_color_role() const
    {
        if (auto const* p = get_pointer<Gfx::ColorRole>())
            return *p;
        return Gfx::ColorRole::NoRole;
    }

    Gfx::AlignmentRole to_alignment_role() const
    {
        if (auto const* p = get_pointer<Gfx::AlignmentRole>())
            return *p;
        return Gfx::AlignmentRole::NoRole;
    }

    Gfx::WindowThemeRole to_window_theme_role() const
    {
        if (auto const* p = get_pointer<Gfx::WindowThemeRole>())
            return *p;
        return Gfx::WindowThemeRole::NoRole;
    }

    Gfx::FlagRole to_flag_role() const
    {
        if (auto const* p = get_pointer<Gfx::FlagRole>())
            return *p;
        return Gfx::FlagRole::NoRole;
    }

    Gfx::MetricRole to_metric_role() const
    {
        if (auto const* p = get_pointer<Gfx::MetricRole>())
            return *p;
        return Gfx::MetricRole::NoRole;
    }

    Gfx::PathRole to_path_role() const
    {
        if (auto const* p = get_pointer<Gfx::PathRole>())
            return *p;
        return Gfx::PathRole::NoRole;
    }

    Color to_color(Color default_value = {}) const
    {
        if (auto const* p = get_pointer<Color>())
            return *p;
        if (auto const* p = get_pointer<ByteString>())
            return Color::from_string(*p).value_or(default_value);
        return default_value;
    }

    ByteString to_byte_string() const
    {
        return visit(
            [](Empty) -> ByteString { return "[null]"; },
            [](ByteString v) { return v; },
            [](Gfx::TextAlignment v) { return ByteString::formatted("Gfx::TextAlignment::{}", Gfx::to_string(v)); },
            [](Gfx::WindowThemeProvider v) { return ByteString::formatted("Gfx::WindowThemeProvider::{}", Gfx::to_string(v)); },
            [](Gfx::ColorRole v) { return ByteString::formatted("Gfx::ColorRole::{}", Gfx::to_string(v)); },
            [](Gfx::AlignmentRole v) { return ByteString::formatted("Gfx::AlignmentRole::{}", Gfx::to_string(v)); },
            [](Gfx::WindowThemeRole v) { return ByteString::formatted("Gfx::WindowThemeRole::{}", Gfx::to_string(v)); },
            [](Gfx::FlagRole v) { return ByteString::formatted("Gfx::FlagRole::{}", Gfx::to_string(v)); },
            [](Gfx::MetricRole v) { return ByteString::formatted("Gfx::MetricRole::{}", Gfx::to_string(v)); },
            [](Gfx::PathRole v) { return ByteString::formatted("Gfx::PathRole::{}", Gfx::to_string(v)); },
            [](NonnullRefPtr<Gfx::Font const> const& font) { return ByteString::formatted("[Font: {}]", font->name()); },
            [](NonnullRefPtr<Gfx::Bitmap const> const&) -> ByteString { return "[Gfx::Bitmap]"; },
            [](GUI::Icon const&) -> ByteString { return "[GUI::Icon]"; },
            [](Detail::Boolean v) { return ByteString::formatted("{}", v.value); },
            [](auto const& v) { return ByteString::formatted("{}", v); });
    }

    bool operator==(Variant const&) const;
    bool operator<(Variant const&) const;
};

}
