/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/Token.h>

namespace Web::CSS {

class StyleBlockRule {
    friend class Parser;

public:
    StyleBlockRule();
    ~StyleBlockRule();

    bool is_curly() const { return m_token.is_open_curly(); }
    bool is_paren() const { return m_token.is_open_paren(); }
    bool is_square() const { return m_token.is_open_square(); }

    Vector<String> const& values() const { return m_values; }

    String to_string() const;

private:
    Token m_token;
    Vector<String> m_values;
};

}
