/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>

namespace Web::CSS {

class StyleComponentValueRule;

class StyleFunctionRule : public RefCounted<StyleFunctionRule> {
    friend class Parser;

public:
    explicit StyleFunctionRule(String name);
    StyleFunctionRule(String name, Vector<StyleComponentValueRule>&& values);
    ~StyleFunctionRule();

    String const& name() const { return m_name; }
    Vector<StyleComponentValueRule> const& values() const { return m_values; }

    String to_string() const;

private:
    String m_name;
    Vector<StyleComponentValueRule> m_values;
};
}
