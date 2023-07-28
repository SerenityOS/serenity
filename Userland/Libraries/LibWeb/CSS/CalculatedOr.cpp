/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatedOr.h"

namespace Web::CSS {

Angle AngleOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_angle().value();
}

Frequency FrequencyOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_frequency().value();
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

Percentage PercentageOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_percentage().value();
}

Time TimeOrCalculated::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&) const
{
    return calculated->resolve_time().value();
}

}
