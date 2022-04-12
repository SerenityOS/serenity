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
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class StyleFunctionRule : public RefCounted<StyleFunctionRule> {
    friend class Parser::Parser;

public:
    explicit StyleFunctionRule(String name);
    StyleFunctionRule(String name, Vector<ComponentValue>&& values);
    ~StyleFunctionRule();

    String const& name() const { return m_name; }
    Vector<ComponentValue> const& values() const { return m_values; }

    String to_string() const;

private:
    String m_name;
    Vector<ComponentValue> m_values;
};
}
