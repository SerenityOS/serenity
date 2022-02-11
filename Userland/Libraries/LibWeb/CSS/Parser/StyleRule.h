/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>

namespace Web::CSS {

class StyleRule : public RefCounted<StyleRule> {
    friend class Parser;

public:
    enum class Type {
        At,
        Qualified,
    };

    StyleRule(Type);
    ~StyleRule();

    Vector<StyleComponentValueRule> const& prelude() const { return m_prelude; }
    RefPtr<StyleBlockRule> const block() const { return m_block; }

    String to_string() const;

private:
    Type const m_type;
    String m_name; // At-rules only
    Vector<StyleComponentValueRule> m_prelude;
    RefPtr<StyleBlockRule> m_block;
};

}
