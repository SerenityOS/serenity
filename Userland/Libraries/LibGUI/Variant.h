/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/String.h>
#include <LibGUI/Icon.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

namespace Detail {
struct Boolean {
    bool value;
};
using VariantUnderlyingType = AK::Variant<Empty, Boolean, float, i32, i64, u32, u64, String, Color, Gfx::IntPoint, Gfx::IntSize, Gfx::IntRect, Gfx::TextAlignment, Gfx::ColorRole, Gfx::AlignmentRole, Gfx::FlagRole, Gfx::MetricRole, Gfx::PathRole, NonnullRefPtr<Gfx::Bitmap>, NonnullRefPtr<Gfx::Font>, GUI::Icon>;
}

class Variant : public Detail::VariantUnderlyingType {
public:
    using Detail::VariantUnderlyingType::Variant;
    using Detail::VariantUnderlyingType::operator=;

    Variant(JsonValue const&);
    Variant& operator=(JsonValue const&);
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
    Variant(T&& value) requires(IsConstructible<String, T>)
        : Variant(String(forward<T>(value)))
    {
    }
    template<typename T>
    Variant& operator=(T&& v) requires(IsConstructible<String, T>)
    {
        set(String(v));
        return *this;
    }

    template<OneOfIgnoringCV<Gfx::Bitmap, Gfx::Font> T>
    Variant(T const& value)
        : Variant(NonnullRefPtr<RemoveCV<T>>(value))
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
    bool is_string() const { return has<String>(); }
    bool is_bitmap() const { return has<NonnullRefPtr<Gfx::Bitmap>>(); }
    bool is_color() const { return has<Color>(); }
    bool is_icon() const { return has<GUI::Icon>(); }
    bool is_point() const { return has<Gfx::IntPoint>(); }
    bool is_size() const { return has<Gfx::IntSize>(); }
    bool is_rect() const { return has<Gfx::IntRect>(); }
    bool is_font() const { return has<NonnullRefPtr<Gfx::Font>>(); }
    bool is_text_alignment() const { return has<Gfx::TextAlignment>(); }
    bool is_color_role() const { return has<Gfx::ColorRole>(); }
    bool is_alignment_role() const { return has<Gfx::AlignmentRole>(); }
    bool is_flag_role() const { return has<Gfx::FlagRole>(); }
    bool is_metric_role() const { return has<Gfx::MetricRole>(); }
    bool is_path_role() const { return has<Gfx::PathRole>(); }

    bool as_bool() const { return get<Detail::Boolean>().value; }

    bool to_bool() const
    {
        return visit(
            [](Empty) { return false; },
            [](Detail::Boolean v) { return v.value; },
            [](String const& v) { return !v.is_null(); },
            [](Integral auto v) { return v != 0; },
            [](OneOf<Gfx::IntRect, Gfx::IntPoint, Gfx::IntSize> auto const& v) { return !v.is_null(); },
            [](Enum auto const&) { return true; },
            [](OneOf<float, String, Color, NonnullRefPtr<Gfx::Font>, NonnullRefPtr<Gfx::Bitmap>, GUI::Icon> auto const&) { return true; });
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
            [](String const& v) {
                if constexpr (IsUnsigned<T>)
                    return v.to_uint<T>().value_or(0u);
                else
                    return v.to_int<T>().value_or(0);
            },
            [](Enum auto const&) -> T { return 0; },
            [](OneOf<Gfx::IntPoint, Gfx::IntRect, Gfx::IntSize, Color, NonnullRefPtr<Gfx::Font>, NonnullRefPtr<Gfx::Bitmap>, GUI::Icon> auto const&) -> T { return 0; });
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

    Gfx::IntPoint as_point() const { return get<Gfx::IntPoint>(); }
    Gfx::IntSize as_size() const { return get<Gfx::IntSize>(); }
    Gfx::IntRect as_rect() const { return get<Gfx::IntRect>(); }
    String as_string() const { return get<String>(); }
    Gfx::Bitmap const& as_bitmap() const { return *get<NonnullRefPtr<Gfx::Bitmap>>(); }
    GUI::Icon as_icon() const { return get<GUI::Icon>(); }
    Color as_color() const { return get<Color>(); }
    Gfx::Font const& as_font() const { return *get<NonnullRefPtr<Gfx::Font>>(); }

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
        if (auto const* p = get_pointer<String>())
            return Color::from_string(*p).value_or(default_value);
        return default_value;
    }

    String to_string() const
    {
        return visit(
            [](Empty) -> String { return "[null]"; },
            [](Gfx::TextAlignment v) { return String::formatted("Gfx::TextAlignment::{}", Gfx::to_string(v)); },
            [](Gfx::ColorRole v) { return String::formatted("Gfx::ColorRole::{}", Gfx::to_string(v)); },
            [](Gfx::AlignmentRole v) { return String::formatted("Gfx::AlignmentRole::{}", Gfx::to_string(v)); },
            [](Gfx::FlagRole v) { return String::formatted("Gfx::FlagRole::{}", Gfx::to_string(v)); },
            [](Gfx::MetricRole v) { return String::formatted("Gfx::MetricRole::{}", Gfx::to_string(v)); },
            [](Gfx::PathRole v) { return String::formatted("Gfx::PathRole::{}", Gfx::to_string(v)); },
            [](NonnullRefPtr<Gfx::Font> const& font) { return String::formatted("[Font: {}]", font->name()); },
            [](NonnullRefPtr<Gfx::Bitmap> const&) -> String { return "[Gfx::Bitmap]"; },
            [](GUI::Icon const&) -> String { return "[GUI::Icon]"; },
            [](Detail::Boolean v) { return String::formatted("{}", v.value); },
            [](auto const& v) { return String::formatted("{}", v); });
    }

    bool operator==(Variant const&) const;
    bool operator<(Variant const&) const;
};

}
