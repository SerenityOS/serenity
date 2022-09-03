/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Window.h>
#include <LibWeb/SVG/SVGLength.h>

namespace Web::SVG {

JS::NonnullGCPtr<SVGLength> SVGLength::create(HTML::Window& window, u8 unit_type, float value)
{
    return *window.heap().allocate<SVGLength>(window.realm(), window, unit_type, value);
}

SVGLength::SVGLength(HTML::Window& window, u8 unit_type, float value)
    : PlatformObject(window.realm())
    , m_unit_type(unit_type)
    , m_value(value)
{
    set_prototype(&window.cached_web_prototype("SVGLength"));
}

SVGLength::~SVGLength() = default;

// https://www.w3.org/TR/SVG11/types.html#__svg__SVGLength__value
DOM::ExceptionOr<void> SVGLength::set_value(float value)
{
    // FIXME: Raise an exception if this <length> is read-only.
    m_value = value;
    return {};
}

}
