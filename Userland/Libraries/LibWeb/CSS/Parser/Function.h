/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/Forward.h>

namespace Web::CSS::Parser {

class Function : public RefCounted<Function> {
public:
    static NonnullRefPtr<Function> create(DeprecatedFlyString name, Vector<ComponentValue>&& values)
    {
        return adopt_ref(*new Function(move(name), move(values)));
    }

    ~Function();

    StringView name() const { return m_name; }
    Vector<ComponentValue> const& values() const { return m_values; }

    DeprecatedString to_deprecated_string() const;

private:
    Function(DeprecatedFlyString name, Vector<ComponentValue>&& values);

    DeprecatedFlyString m_name;
    Vector<ComponentValue> m_values;
};
}
