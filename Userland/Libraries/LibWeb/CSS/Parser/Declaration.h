/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>

namespace Web::CSS::Parser {

class Declaration {
public:
    Declaration(FlyString name, Vector<ComponentValue> values, Important);
    ~Declaration();

    StringView name() const { return m_name; }
    Vector<ComponentValue> const& values() const { return m_values; }
    Important importance() const { return m_important; }

    String to_string() const;

private:
    FlyString m_name;
    Vector<ComponentValue> m_values;
    Important m_important { Important::No };
};

}
