/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
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

    String to_string() const;

private:
    Token m_token;
    Vector<String> m_values;
};

}
