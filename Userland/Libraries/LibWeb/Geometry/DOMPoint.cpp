/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Geometry/DOMPoint.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMPoint> DOMPoint::create_with_global_object(HTML::Window& window, double x, double y, double z, double w)
{
    return *window.heap().allocate<DOMPoint>(window.realm(), window, x, y, z, w);
}

DOMPoint::DOMPoint(HTML::Window& window, double x, double y, double z, double w)
    : DOMPointReadOnly(window, x, y, z, w)
{
    set_prototype(&window.cached_web_prototype("DOMPoint"));
}

DOMPoint::~DOMPoint() = default;

}
