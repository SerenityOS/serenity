/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PercentageOr.h"

namespace Web::CSS {

Angle AnglePercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&, Angle const& reference_value) const
{
    return calculated->resolve_angle_percentage(reference_value).value();
}

Frequency FrequencyPercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&, Frequency const& reference_value) const
{
    return calculated->resolve_frequency_percentage(reference_value).value();
}

Length LengthPercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node, Length const& reference_value) const
{
    return calculated->resolve_length_percentage(layout_node, reference_value).value();
}

Length LengthPercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node, CSSPixels reference_value) const
{
    return calculated->resolve_length_percentage(layout_node, reference_value).value();
}

Time TimePercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const&, Time const& reference_value) const
{
    return calculated->resolve_time_percentage(reference_value).value();
}

}
