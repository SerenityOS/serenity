/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/JsonValue.h>

namespace Core {

class Property {
    AK_MAKE_NONCOPYABLE(Property);

public:
    Property(String name, Function<JsonValue()> getter, Function<bool(JsonValue const&)> setter = nullptr);
    ~Property() = default;

    bool set(JsonValue const& value)
    {
        if (!m_setter)
            return false;
        return m_setter(value);
    }

    JsonValue get() const { return m_getter(); }

    String const& name() const { return m_name; }
    bool is_readonly() const { return !m_setter; }

private:
    String m_name;
    Function<JsonValue()> m_getter;
    Function<bool(JsonValue const&)> m_setter;
};

}
