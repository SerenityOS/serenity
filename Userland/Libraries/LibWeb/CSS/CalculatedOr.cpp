/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatedOr.h"
#include <LibWeb/CSS/StyleValues/AngleStyleValue.h>
#include <LibWeb/CSS/StyleValues/FlexStyleValue.h>
#include <LibWeb/CSS/StyleValues/FrequencyStyleValue.h>
#include <LibWeb/CSS/StyleValues/IntegerStyleValue.h>
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumberStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/ResolutionStyleValue.h>
#include <LibWeb/CSS/StyleValues/TimeStyleValue.h>

namespace Web::CSS {

Angle AngleOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_angle().value();
}

NonnullRefPtr<StyleValue> AngleOrCalculated::create_style_value() const
{
    return AngleStyleValue::create(value());
}

Flex FlexOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_flex().value();
}

NonnullRefPtr<StyleValue> FlexOrCalculated::create_style_value() const
{
    return FlexStyleValue::create(value());
}

Frequency FrequencyOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_frequency().value();
}

NonnullRefPtr<StyleValue> FrequencyOrCalculated::create_style_value() const
{
    return FrequencyStyleValue::create(value());
}

i64 IntegerOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_integer().value();
}

NonnullRefPtr<StyleValue> IntegerOrCalculated::create_style_value() const
{
    return IntegerStyleValue::create(value());
}

Length LengthOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node) const
{
    return calculated->resolve_length(layout_node).value();
}

Length LengthOrCalculated::resolved(Length::ResolutionContext const& context) const
{
    if (is_calculated())
        return calculated()->resolve_length(context).value();
    return value();
}

NonnullRefPtr<StyleValue> LengthOrCalculated::create_style_value() const
{
    return LengthStyleValue::create(value());
}

double NumberOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_number().value();
}

NonnullRefPtr<StyleValue> NumberOrCalculated::create_style_value() const
{
    return NumberStyleValue::create(value());
}

Percentage PercentageOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_percentage().value();
}

NonnullRefPtr<StyleValue> PercentageOrCalculated::create_style_value() const
{
    return PercentageStyleValue::create(value());
}

Resolution ResolutionOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_resolution().value();
}

NonnullRefPtr<StyleValue> ResolutionOrCalculated::create_style_value() const
{
    return ResolutionStyleValue::create(value());
}

Time TimeOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_time().value();
}

NonnullRefPtr<StyleValue> TimeOrCalculated::create_style_value() const
{
    return TimeStyleValue::create(value());
}

}
