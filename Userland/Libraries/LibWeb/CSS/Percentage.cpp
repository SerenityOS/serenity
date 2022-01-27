/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

Length LengthPercentage::resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const& calculated, Layout::Node const& layout_node, Length const& reference_value) const
{
    return calculated->resolve_length_percentage(layout_node, reference_value)->resolved(layout_node, reference_value);
}

}
