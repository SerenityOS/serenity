/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web::DOM {

class Attribute {
public:
    Attribute(const FlyString& name, const String& value)
        : m_name(name)
        , m_value(value)
    {
    }

    const FlyString& name() const { return m_name; }
    const String& value() const { return m_value; }

    void set_value(const String& value) { m_value = value; }

private:
    FlyString m_name;
    String m_value;
};

}
