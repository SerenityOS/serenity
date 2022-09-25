/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/AvailableSpace.h>
#include <math.h>

namespace Web::Layout {

AvailableSpace AvailableSpace::make_definite(float value)
{
    return AvailableSpace { Type::Definite, value };
}

AvailableSpace AvailableSpace::make_indefinite()
{
    return AvailableSpace { Type::Indefinite, INFINITY };
}

AvailableSpace AvailableSpace::make_min_content()
{
    return AvailableSpace { Type::MinContent, 0 };
}

AvailableSpace AvailableSpace::make_max_content()
{
    return AvailableSpace { Type::MaxContent, INFINITY };
}

String AvailableSpace::to_string() const
{
    switch (m_type) {
    case Type::Definite:
        return String::formatted("definite({})", m_value);
    case Type::Indefinite:
        return "indefinite";
    case Type::MinContent:
        return "min-content";
    case Type::MaxContent:
        return "max-content";
    }
    VERIFY_NOT_REACHED();
}

AvailableSpace::AvailableSpace(Type type, float value)
    : m_type(type)
    , m_value(value)
{
}

}
