/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/Token.h>

namespace Web::CSS {

class StyleComponentValueRule {
    friend class Parser;

public:
    enum class ComponentType {
        Token,
        Function,
        Block
    };

    explicit StyleComponentValueRule(ComponentType);
    ~StyleComponentValueRule();

    String to_string() const;

private:
    ComponentType m_type;
    Token m_token;
    StyleFunctionRule m_function;
    StyleBlockRule m_block;
};
}
