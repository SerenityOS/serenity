/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/Block.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>

namespace Web::CSS::Parser {

class StyleRule : public RefCounted<StyleRule> {
public:
    enum class Type {
        At,
        Qualified,
    };

    static NonnullRefPtr<StyleRule> make_at_rule(FlyString name, Vector<ComponentValue> prelude, RefPtr<Block> block)
    {
        return adopt_ref(*new StyleRule(Type::At, move(name), move(prelude), move(block)));
    }

    static NonnullRefPtr<StyleRule> make_qualified_rule(Vector<ComponentValue> prelude, RefPtr<Block> block)
    {
        return adopt_ref(*new StyleRule(Type::Qualified, {}, move(prelude), move(block)));
    }

    ~StyleRule();

    bool is_qualified_rule() const { return m_type == Type::Qualified; }
    bool is_at_rule() const { return m_type == Type::At; }

    Vector<ComponentValue> const& prelude() const { return m_prelude; }
    RefPtr<Block const> block() const { return m_block; }
    StringView at_rule_name() const { return m_at_rule_name; }

    String to_string() const;

private:
    StyleRule(Type, FlyString name, Vector<ComponentValue> prelude, RefPtr<Block>);

    Type const m_type;
    FlyString m_at_rule_name;
    Vector<ComponentValue> m_prelude;
    RefPtr<Block> m_block;
};

}
