/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BorderRadiusShorthandStyleValue.h"

namespace Web::CSS {

ErrorOr<String> BorderRadiusShorthandStyleValue::to_string() const
{
    return String::formatted("{} {} {} {} / {} {} {} {}",
        m_properties.top_left->horizontal_radius().to_string(),
        m_properties.top_right->horizontal_radius().to_string(),
        m_properties.bottom_right->horizontal_radius().to_string(),
        m_properties.bottom_left->horizontal_radius().to_string(),
        m_properties.top_left->vertical_radius().to_string(),
        m_properties.top_right->vertical_radius().to_string(),
        m_properties.bottom_right->vertical_radius().to_string(),
        m_properties.bottom_left->vertical_radius().to_string());
}

}
