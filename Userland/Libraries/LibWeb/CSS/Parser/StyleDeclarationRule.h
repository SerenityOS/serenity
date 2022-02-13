/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
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
    Important m_important { Important::No };
};

}
