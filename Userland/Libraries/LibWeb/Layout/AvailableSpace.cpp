/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/AvailableSpace.h>
#include <math.h>

namespace Web::Layout {

AvailableSize AvailableSize::make_definite(CSSPixels value)
{
    VERIFY(!value.might_be_saturated());
    return AvailableSize { Type::Definite, value };
}

AvailableSize AvailableSize::make_indefinite()
{
    return AvailableSize { Type::Indefinite, INFINITY };
}

AvailableSize AvailableSize::make_min_content()
{
    return AvailableSize { Type::MinContent, 0 };
}

AvailableSize AvailableSize::make_max_content()
{
    return AvailableSize { Type::MaxContent, INFINITY };
}

DeprecatedString AvailableSize::to_deprecated_string() const
{
    switch (m_type) {
    case Type::Definite:
        return DeprecatedString::formatted("definite({})", m_value);
    case Type::Indefinite:
        return "indefinite";
    case Type::MinContent:
        return "min-content";
    case Type::MaxContent:
        return "max-content";
    }
    VERIFY_NOT_REACHED();
}

DeprecatedString AvailableSpace::to_deprecated_string() const
{
    return DeprecatedString::formatted("{} x {}", width, height);
}

AvailableSize::AvailableSize(Type type, CSSPixels value)
    : m_type(type)
    , m_value(value)
{
}

}
