/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
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
public:
    static NonnullRefPtr<Function> create(FlyString name, Vector<ComponentValue>&& values)
    {
        return adopt_ref(*new Function(move(name), move(values)));
    }

    ~Function();

    StringView name() const { return m_name; }
    Vector<ComponentValue> const& values() const { return m_values; }

    String to_string() const;

private:
    Function(FlyString name, Vector<ComponentValue>&& values);

    FlyString m_name;
    Vector<ComponentValue> m_values;
};
}
