/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/String.h>
#include <YAK/Vector.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>

namespace Web::CSS {

class StyleDeclarationRule {
    friend class Parser;

public:
    StyleDeclarationRule();
    ~StyleDeclarationRule();

    String to_string() const;

private:
    String m_name;
    Vector<StyleComponentValueRule> m_values;
    bool m_important { false };
};

}
