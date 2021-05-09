/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibWeb/CSS/Parser/QualifiedStyleRule.h>

namespace Web::CSS {

class AtStyleRule : public QualifiedStyleRule {
    friend class Parser;

public:
    AtStyleRule();
    ~AtStyleRule();
    String to_string() const;

private:
    String m_name;
};

}
