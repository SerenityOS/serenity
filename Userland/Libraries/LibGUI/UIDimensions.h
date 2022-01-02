/*
 * Copyright (c) 2022, Frhun <serenitystuff@frhun.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonValue.h>
#include <AK/Optional.h>
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

    friend constexpr auto AK::max<GUI::UIDimension>(const GUI::UIDimension&, const GUI::UIDimension&) -> GUI::UIDimension;
    friend constexpr auto AK::min<GUI::UIDimension>(const GUI::UIDimension&, const GUI::UIDimension&) -> GUI::UIDimension;

private:
    int m_value;

public:
    UIDimension() = delete;

    UIDimension(int value)
    {
        VERIFY(value >= 0);
        m_value = value;
    }

    UIDimension(SpecialDimension special)
        : m_value { int(special) }
    {
    }

    [[nodiscard]] inline bool is_special_value() const
    {
        return m_value < 0;
    }

    [[nodiscard]] inline bool is_regular_value() const
    {
        return m_value >= 0;
    }

    [[nodiscard]] inline bool is_shrink() const
    {
        return m_value == int(SpecialDimension::Shrink);
    }

    [[nodiscard]] inline bool is_grow() const
    {
        return m_value == int(SpecialDimension::Grow);
    }

    [[nodiscard]] inline bool is_opportunistic_grow() const
    {
        return m_value == int(SpecialDimension::OpportunisticGrow);
    }

    [[nodiscard]] inline bool is_fit() const
    {
        return m_value == int(SpecialDimension::Fit);
    }

    [[nodiscard]] inline bool is_one_of(std::initializer_list<SpecialDimension> valid_values)
    {
        for (SpecialDimension v : valid_values) {
            if (m_value == int(v) || (v == SpecialDimension::Regular && is_regular_value()))
                return true;
        }
        return false;
    }

    [[nodiscard]] inline bool operator==(UIDimension const& other) const
    {
        return m_value == other.m_value;
    }

    [[nodiscard]] inline UIDimension regular_sum_with_verify(UIDimension const& other) const
    {
        VERIFY(is_regular_value() && other.is_regular_value());
        return UIDimension { m_value + other.m_value };
    }

    inline void add_to_regular_with_verify(int to_add)
    {
        VERIFY(is_regular_value());
        VERIFY(m_value >= -to_add);
        m_value += to_add;
    }

    inline void add_if_regular(int to_add)
    {
        if (is_regular_value()) {
            m_value += to_add;
        }
    }

    [[nodiscard]] inline int value_or_zero_if_shrink_with_verify() const
    {
        if (m_value >= 0)
            return m_value;
        if (m_value == int(SpecialDimension::Shrink))
            return 0;
        VERIFY_NOT_REACHED();
    }

    [[nodiscard]] inline int value_verify_regular() const
    {
        VERIFY(is_regular_value());
        return m_value;
    }

    [[nodiscard]] AK::JsonValue as_json_value() const
    {
        if (is_regular_value())
            return m_value;
        else {
            if (is_shrink())
                return "shrink";
            else if (is_grow())
                return "grow";
            else if (is_opportunistic_grow())
                return "opportunistic_grow";
            else if (is_fit())
                return "fit";
            else
                VERIFY_NOT_REACHED();
        }
    }

    [[nodiscard]] static Optional<UIDimension> construct_from_json_value(AK::JsonValue const value)
    {
        if (value.is_string()) {
            String value_literal = value.as_string();
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
        } else {
            int value_int = value.to_i32();
            if (value_int < 0)
                return {};
            return UIDimension(value_int);
        }
    }
};

class UISize : public Gfx::Size<UIDimension> {

public:
    UISize() = delete;

    UISize(int in_width, int in_height)
        : Gfx::Size<UIDimension>(in_width, in_height)
    {
        VERIFY(width().is_regular_value() && height().is_regular_value());
    }

    UISize(Gfx::IntSize size)
        : UISize(size.width(), size.height())
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

    [[nodiscard]] inline bool has_only_regular_values() const
    {
        return width().is_regular_value() && height().is_regular_value();
    }

    [[nodiscard]] inline bool either_is(UIDimension to_match) const
    {
        return (width() == to_match || height() == to_match);
    }

    explicit operator Gfx::IntSize() const
    {
        return Gfx::IntSize(width().value_verify_regular(), height().value_verify_regular());
    }
};

}

namespace AK {

template<>
inline auto max<GUI::UIDimension>(const GUI::UIDimension& a, const GUI::UIDimension& b) -> GUI::UIDimension
{
    if ((a.is_regular_value() && b.is_regular_value()) || (a.is_special_value() && b.is_special_value()))
        return a.m_value > b.m_value ? a : b;
    if (a.is_grow() || b.is_grow())
        return GUI::UIDimension { GUI::SpecialDimension::Grow };
    if (a.is_opportunistic_grow() || b.is_opportunistic_grow())
        return GUI::UIDimension { GUI::SpecialDimension::OpportunisticGrow };
    if (a.is_fit() || b.is_fit())
        return GUI::UIDimension { GUI::SpecialDimension::Fit };
    if (a.is_shrink())
        return b;
    if (b.is_shrink())
        return a;
    VERIFY_NOT_REACHED();
}

template<>
inline auto min<GUI::UIDimension>(const GUI::UIDimension& a, const GUI::UIDimension& b) -> GUI::UIDimension
{
    if ((a.is_regular_value() && b.is_regular_value()) || (a.is_special_value() && b.is_special_value()))
        return a.m_value < b.m_value ? a : b;
    if (a.is_shrink() || b.is_shrink())
        return GUI::UIDimension { GUI::SpecialDimension::Shrink };
    if (a.is_regular_value())
        return a;
    if (b.is_regular_value())
        return b;
    if (a.is_fit() || b.is_fit())
        return GUI::UIDimension { GUI::SpecialDimension::Fit };
    if (a.is_opportunistic_grow() || b.is_opportunistic_grow())
        return GUI::UIDimension { GUI::SpecialDimension::OpportunisticGrow };
    VERIFY_NOT_REACHED();
}

template<>
inline auto clamp<GUI::UIDimension>(const GUI::UIDimension& input, const GUI::UIDimension& lower_bound, const GUI::UIDimension& upper_bound) -> GUI::UIDimension
{
    return min(max(input, lower_bound), upper_bound);
}

}

#define REGISTER_UI_DIMENSION_PROPERTY(property_name, getter, setter)         \
    register_property(                                                        \
        property_name,                                                        \
        [this] {                                                              \
            return this->getter().as_json_value();                            \
        },                                                                    \
        [this](auto& value) {                                                 \
            auto result = GUI::UIDimension::construct_from_json_value(value); \
            if (result.has_value())                                           \
                this->setter(result.value());                                 \
            return result.has_value();                                        \
        });

#define REGISTER_UI_SIZE_PROPERTY(property_name, getter, setter)               \
    register_property(                                                         \
        property_name,                                                         \
        [this] {                                                               \
            auto size = this->getter();                                        \
            JsonObject size_object;                                            \
            size_object.set("width", size.width().as_json_value());            \
            size_object.set("height", size.height().as_json_value());          \
            return size_object;                                                \
        },                                                                     \
        [this](auto& value) {                                                  \
            if (!value.is_object())                                            \
                return false;                                                  \
            auto result_width = GUI::UIDimension::construct_from_json_value(   \
                value.as_object().get("width"));                               \
            auto result_height = GUI::UIDimension::construct_from_json_value(  \
                value.as_object().get("height"));                              \
            if (result_width.has_value() && result_height.has_value()) {       \
                GUI::UISize size(result_width.value(), result_height.value()); \
                setter(size);                                                  \
                return true;                                                   \
            }                                                                  \
            return false;                                                      \
        });
