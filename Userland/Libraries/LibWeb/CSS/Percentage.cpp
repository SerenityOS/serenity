/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

Angle AnglePercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node, Angle const& reference_value) const
{
    return calculated->resolve_angle_percentage(reference_value)->resolved(layout_node, reference_value);
}

Frequency FrequencyPercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node, Frequency const& reference_value) const
{
    return calculated->resolve_frequency_percentage(reference_value)->resolved(layout_node, reference_value);
}

Length LengthPercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node, Length const& reference_value) const
{
    return calculated->resolve_length_percentage(layout_node, reference_value)->resolved(layout_node, reference_value);
}

Time TimePercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node, Time const& reference_value) const
{
    return calculated->resolve_time_percentage(reference_value)->resolved(layout_node, reference_value);
}

}
