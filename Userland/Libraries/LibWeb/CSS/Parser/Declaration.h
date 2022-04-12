/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>

namespace Web::CSS {

class Declaration {
    friend class Parser::Parser;

public:
    Declaration();
    ~Declaration();

    String const& name() const { return m_name; }
    Vector<ComponentValue> const& values() const { return m_values; }
    Important importance() const { return m_important; }

    String to_string() const;

private:
    String m_name;
    Vector<ComponentValue> m_values;
    Important m_important { Important::No };
};

}
