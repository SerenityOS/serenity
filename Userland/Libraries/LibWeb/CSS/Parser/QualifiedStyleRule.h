/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>

namespace Web::CSS {

class StyleComponentValueRule;

class QualifiedStyleRule {
    friend class Parser;

public:
    QualifiedStyleRule();
    ~QualifiedStyleRule();
    String to_string() const;

private:
    Vector<String> m_prelude;
    StyleBlockRule m_block;
};

}
