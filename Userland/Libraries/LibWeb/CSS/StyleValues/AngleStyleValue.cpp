/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AngleStyleValue.h"

namespace Web::CSS {

AngleStyleValue::AngleStyleValue(Angle angle)
    : StyleValueWithDefaultOperators(Type::Angle)
    , m_angle(move(angle))
{
}

AngleStyleValue::~AngleStyleValue() = default;

String AngleStyleValue::to_string() const
{
    return m_angle.to_string();
}

bool AngleStyleValue::properties_equal(AngleStyleValue const& other) const
{
    return m_angle == other.m_angle;
}

}
