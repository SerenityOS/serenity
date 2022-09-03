/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Geometry/DOMRectReadOnly.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMRectReadOnly> DOMRectReadOnly::create_with_global_object(HTML::Window& window, double x, double y, double width, double height)
{
    return *window.heap().allocate<DOMRectReadOnly>(window.realm(), window, x, y, width, height);
}

DOMRectReadOnly::DOMRectReadOnly(HTML::Window& window, double x, double y, double width, double height)
    : PlatformObject(window.realm())
    , m_rect(x, y, width, height)
{
    set_prototype(&window.cached_web_prototype("DOMRectReadOnly"));
}

DOMRectReadOnly::~DOMRectReadOnly() = default;

}
