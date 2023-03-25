/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "OverflowStyleValue.h"

namespace Web::CSS {

ErrorOr<String> OverflowStyleValue::to_string() const
{
    return String::formatted("{} {}", TRY(m_properties.overflow_x->to_string()), TRY(m_properties.overflow_y->to_string()));
}

}
