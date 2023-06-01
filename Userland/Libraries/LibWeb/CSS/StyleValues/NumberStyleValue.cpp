/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NumberStyleValue.h"

namespace Web::CSS {

ErrorOr<String> NumberStyleValue::to_string() const
{
    return m_value.visit(
        [](auto value) {
            return String::formatted("{}", value);
        });
}

}
