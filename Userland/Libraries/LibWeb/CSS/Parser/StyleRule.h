/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>

namespace Web::CSS {

class StyleRule : public RefCounted<StyleRule> {
    friend class Parser::Parser;

public:
    enum class Type {
        At,
        Qualified,
    };

    StyleRule(Type);
    ~StyleRule();

    bool is_qualified_rule() const { return m_type == Type::Qualified; }
    bool is_at_rule() const { return m_type == Type::At; }

    Vector<Parser::ComponentValue> const& prelude() const { return m_prelude; }
    RefPtr<StyleBlockRule const> block() const { return m_block; }
    String const& at_rule_name() const { return m_at_rule_name; }

    String to_string() const;

private:
    Type const m_type;
    String m_at_rule_name;
    Vector<Parser::ComponentValue> m_prelude;
    RefPtr<StyleBlockRule> m_block;
};

}
