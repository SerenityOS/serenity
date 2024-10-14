/*
 * Copyright (c) 2022, Frhun <serenitystuff@frhun.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonValue.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <initializer_list>

namespace GUI {

// The constants used for special values
// Their order here, also defines their order among each other for min, max; operations, excluding Regular
enum class SpecialDimension : int {
    Regular = 0, // only really useful for is_one_of
    Grow = -1,
    OpportunisticGrow = -2,
    Fit = -3,
    Shrink = -4,
};

class UIDimension {
    friend constexpr auto AK::max<GUI::UIDimension>(GUI::UIDimension const&, GUI::UIDimension const&) -> GUI::UIDimension;
    friend constexpr auto AK::min<GUI::UIDimension>(GUI::UIDimension const&, GUI::UIDimension const&) -> GUI::UIDimension;

public:
    UIDimension() = delete;

    UIDimension(int value)
        : m_value(value)
    {
        VERIFY(value >= 0);
    }

    UIDimension(SpecialDimension special)
        : m_value(to_underlying(special))
    {
    }

    [[nodiscard]] inline bool is_special_value() const
    {
        return m_value < 0;
    }

    [[nodiscard]] inline bool is_int() const
    {
        return m_value >= 0;
    }

    [[nodiscard]] inline bool is_shrink() const
    {
        return m_value == to_underlying(SpecialDimension::Shrink);
    }

    [[nodiscard]] inline bool is_grow() const
    {
        return m_value == to_underlying(SpecialDimension::Grow);
    }

    [[nodiscard]] inline bool is_opportunistic_grow() const
    {
        return m_value == to_underlying(SpecialDimension::OpportunisticGrow);
    }

    [[nodiscard]] inline bool is_fit() const
    {
        return m_value == to_underlying(SpecialDimension::Fit);
    }

    [[nodiscard]] inline bool is_one_of(std::initializer_list<SpecialDimension> valid_values) const
    {
        for (SpecialDimension v : valid_values) {
            if (m_value == to_underlying(v) || (v == SpecialDimension::Regular && is_int()))
                return true;
        }
        return false;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr bool is(SpecialDimension special_value) const
    {
        return m_value == to_underlying(special_value) || (special_value == SpecialDimension::Regular && is_int());
    }

    template<typename... Ts>
    [[nodiscard]] bool is_one_of(Ts... valid_values) const
    {
        return (... || (is(forward<Ts>(valid_values))));
    }

    [[nodiscard]] inline bool operator==(UIDimension other) const
    {
        return m_value == other.m_value;
    }

    [[nodiscard]] inline UIDimension must_sum_with(UIDimension other) const
    {
        VERIFY(is_int() && other.is_int());
        return UIDimension { m_value + other.m_value };
    }

    inline void must_add(int to_add)
    {
        VERIFY(is_int());
        VERIFY(m_value >= -to_add);
        m_value += to_add;
    }

    inline void add_if_int(int to_add)
    {
        if (is_int()) {
            m_value += to_add;
        }
    }

    [[nodiscard]] inline ErrorOr<int> shrink_value() const
    {
        if (m_value >= 0)
            return m_value;
        if (m_value == to_underlying(SpecialDimension::Shrink))
            return 0;
        return Error::from_string_literal("value is neither shrink nor an integer ≥0");
    }

    [[nodiscard]] inline int as_int() const
    {
        VERIFY(is_int());
        return m_value;
    }

    [[nodiscard]] AK::JsonValue as_json_value() const
    {
        if (is_int())
            return m_value;
        if (is_shrink())
            return "shrink";
        if (is_grow())
            return "grow";
        if (is_opportunistic_grow())
            return "opportunistic_grow";
        if (is_fit())
            return "fit";

        VERIFY_NOT_REACHED();
    }

    /// The returned source code, if any, can be used to construct this UIDimension in C++.
    ErrorOr<String> as_cpp_source() const
    {
        String value_source = {};
        if (is_int())
            value_source = String::number(m_value);
        else if (is_shrink())
            value_source = "GUI::SpecialDimension::Shrink"_string;
        else if (is_grow())
            value_source = "GUI::SpecialDimension::Grow"_string;
        else if (is_opportunistic_grow())
            value_source = "GUI::SpecialDimension::OpportunisticGrow"_string;
        else if (is_fit())
            value_source = "GUI::SpecialDimension::Fit"_string;
        return String::formatted("GUI::UIDimension {{ {} }}", value_source);
    }

    [[nodiscard]] static Optional<UIDimension> construct_from_json_value(AK::JsonValue const value)
    {
        if (value.is_string()) {
            ByteString value_literal = value.as_string();
            if (value_literal == "shrink")
                return UIDimension { SpecialDimension::Shrink };
            else if (value_literal == "grow")
                return UIDimension { SpecialDimension::Grow };
            else if (value_literal == "opportunistic_grow")
                return UIDimension { SpecialDimension::OpportunisticGrow };
            else if (value_literal == "fit")
                return UIDimension { SpecialDimension::Fit };
            else
                return {};
        } else if (value.is_integer<i32>()) {
            auto value_int = value.as_integer<i32>();
            if (value_int < 0)
                return {};
            return UIDimension(value_int);
        }
        return {};
    }

private:
    int m_value;
};

class UISize : public Gfx::Size<UIDimension> {

public:
    UISize() = delete;

    UISize(int in_width, int in_height)
        : Gfx::Size<UIDimension>(in_width, in_height)
    {
    }

    UISize(Gfx::IntSize size)
        : UISize(size.width(), size.height())
    {
    }

    UISize(Array<i64, 2> size)
        : UISize(size[0], size[1])
    {
    }

    UISize(SpecialDimension special)
        : Gfx::Size<UIDimension>(UIDimension { special }, UIDimension { special })
    {
    }

    UISize(UIDimension width, UIDimension height)
        : Gfx::Size<UIDimension>(width, height)
    {
    }

    inline UISize replace_component_if_matching_with(UIDimension to_match, UISize replacement)
    {
        if (width() == to_match)
            set_width(replacement.width());
        if (height() == to_match)
            set_height(replacement.height());
        return *this;
    }

    [[nodiscard]] inline bool has_only_int_values() const
    {
        return width().is_int() && height().is_int();
    }

    [[nodiscard]] inline bool either_is(UIDimension to_match) const
    {
        return (width() == to_match || height() == to_match);
    }

    explicit operator Gfx::IntSize() const
    {
        return Gfx::IntSize(width().as_int(), height().as_int());
    }
};

}

namespace AK {

template<>
inline auto max<GUI::UIDimension>(GUI::UIDimension const& a, GUI::UIDimension const& b) -> GUI::UIDimension
{
    if ((a.is_int() && b.is_int()) || (a.is_special_value() && b.is_special_value()))
        return a.m_value > b.m_value ? a : b;
    if (a.is_grow() || b.is_grow())
        return GUI::SpecialDimension::Grow;
    if (a.is_opportunistic_grow() || b.is_opportunistic_grow())
        return GUI::SpecialDimension::OpportunisticGrow;
    if (a.is_fit() || b.is_fit())
        return GUI::SpecialDimension::Fit;
    if (a.is_shrink())
        return b;
    if (b.is_shrink())
        return a;
    VERIFY_NOT_REACHED();
}

template<>
inline auto min<GUI::UIDimension>(GUI::UIDimension const& a, GUI::UIDimension const& b) -> GUI::UIDimension
{
    if ((a.is_int() && b.is_int()) || (a.is_special_value() && b.is_special_value()))
        return a.m_value < b.m_value ? a : b;
    if (a.is_shrink() || b.is_shrink())
        return GUI::SpecialDimension::Shrink;
    if (a.is_int())
        return a;
    if (b.is_int())
        return b;
    if (a.is_fit() || b.is_fit())
        return GUI::SpecialDimension::Fit;
    if (a.is_opportunistic_grow() || b.is_opportunistic_grow())
        return GUI::SpecialDimension::OpportunisticGrow;
    VERIFY_NOT_REACHED();
}

template<>
inline auto clamp<GUI::UIDimension>(GUI::UIDimension const& input, GUI::UIDimension const& lower_bound, GUI::UIDimension const& upper_bound) -> GUI::UIDimension
{
    return min(max(input, lower_bound), upper_bound);
}

}

#define REGISTER_UI_DIMENSION_PROPERTY(property_name, getter, setter) \
    register_property(                                                \
        property_name##sv,                                            \
        [this] {                                                      \
            return this->getter().as_json_value();                    \
        },                                                            \
        ::GUI::PropertyDeserializer<::GUI::UIDimension> {},           \
        [this](auto const& value) { return setter(value); });

#define REGISTER_READONLY_UI_DIMENSION_PROPERTY(property_name, getter) \
    register_property(                                                 \
        property_name##sv,                                             \
        [this] {                                                       \
            return this->getter().as_json_value();                     \
        },                                                             \
        nullptr, nullptr);

#define REGISTER_UI_SIZE_PROPERTY(property_name, getter, setter)        \
    register_property(                                                  \
        property_name##sv,                                              \
        [this] {                                                        \
            auto size = this->getter();                                 \
            JsonObject size_object;                                     \
            size_object.set("width"sv, size.width().as_json_value());   \
            size_object.set("height"sv, size.height().as_json_value()); \
            return size_object;                                         \
        },                                                              \
        ::GUI::PropertyDeserializer<::GUI::UISize> {},                  \
        [this](auto const& value) { return setter(value); });

#define REGISTER_READONLY_UI_SIZE_PROPERTY(property_name, getter)     \
    register_property(                                                \
        property_name##sv,                                            \
        [this] {                                                      \
            auto size = this->getter();                               \
            JsonObject size_object;                                   \
            size_object.set("width", size.width().as_json_value());   \
            size_object.set("height", size.height().as_json_value()); \
            return size_object;                                       \
        },                                                            \
        nullptr, nullptr);
