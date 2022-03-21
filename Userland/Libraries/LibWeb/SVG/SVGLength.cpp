/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/SVG/SVGLength.h>

namespace Web::SVG {

NonnullRefPtr<SVGLength> SVGLength::create(u8 unit_type, float value)
{
    return adopt_ref(*new SVGLength(unit_type, value));
}

SVGLength::SVGLength(u8 unit_type, float value)
    : m_unit_type(unit_type)
    , m_value(value)
{
}

// https://www.w3.org/TR/SVG11/types.html#__svg__SVGLength__value
DOM::ExceptionOr<void> SVGLength::set_value(float value)
{
    // FIXME: Raise an exception if this <length> is read-only.
    m_value = value;
    return {};
}

}
