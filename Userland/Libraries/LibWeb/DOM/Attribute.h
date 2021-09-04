/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web {

class Attribute {
public:
    Attribute(FlyString const& name, String const& value)
        : m_name(name)
        , m_value(value)
    {
    }

    FlyString const& name() const { return m_name; }
    String const& value() const { return m_value; }

    void set_value(String const& value) { m_value = value; }

private:
    FlyString m_name;
    String m_value;
};

}
