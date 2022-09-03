/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Geometry/DOMPointReadOnly.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMPointReadOnly> DOMPointReadOnly::create_with_global_object(HTML::Window& window, double x, double y, double z, double w)
{
    return *window.heap().allocate<DOMPointReadOnly>(window.realm(), window, x, y, z, w);
}

DOMPointReadOnly::DOMPointReadOnly(HTML::Window& window, double x, double y, double z, double w)
    : PlatformObject(window.realm())
    , m_x(x)
    , m_y(y)
    , m_z(z)
    , m_w(w)
{
    set_prototype(&window.cached_web_prototype("DOMPointReadOnly"));
}

DOMPointReadOnly::~DOMPointReadOnly() = default;

}
