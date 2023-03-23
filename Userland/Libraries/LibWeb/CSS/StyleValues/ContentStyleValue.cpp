/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ContentStyleValue.h"

namespace Web::CSS {

ErrorOr<String> ContentStyleValue::to_string() const
{
    if (has_alt_text())
        return String::formatted("{} / {}", TRY(m_properties.content->to_string()), TRY(m_properties.alt_text->to_string()));
    return m_properties.content->to_string();
}

bool ContentStyleValue::Properties::operator==(ContentStyleValue::Properties const&) const = default;

}
