/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>

namespace Web::CSS {

class QualifiedStyleRule {
    friend class Parser;

public:
    QualifiedStyleRule();
    ~QualifiedStyleRule();

    Vector<StyleComponentValueRule> const& prelude() const { return m_prelude; }
    StyleBlockRule const& block() const { return *m_block; }

    String to_string() const;

private:
    Vector<StyleComponentValueRule> m_prelude;
    RefPtr<StyleBlockRule> m_block;
};

}
