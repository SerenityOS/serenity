/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/Forward.h>

namespace Web::CSS::Parser {

class Function : public RefCounted<Function> {
    friend class Parser;

public:
    explicit Function(FlyString name);
    Function(FlyString name, Vector<ComponentValue>&& values);
    ~Function();

    StringView name() const { return m_name; }
    Vector<ComponentValue> const& values() const { return m_values; }

    String to_string() const;

private:
    FlyString m_name;
    Vector<ComponentValue> m_values;
};
}
