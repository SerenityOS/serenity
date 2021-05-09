/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

namespace Web::CSS {

class StyleComponentValueRule;

class StyleFunctionRule {
    friend class Parser;

public:
    StyleFunctionRule();
    ~StyleFunctionRule();

    String to_string() const;

private:
    String m_name;
    Vector<String> m_values;
};

}
