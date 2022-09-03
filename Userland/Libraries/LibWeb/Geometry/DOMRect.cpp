/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Geometry/DOMRect.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMRect> DOMRect::create_with_global_object(HTML::Window& window, double x, double y, double width, double height)
{
    return *window.heap().allocate<DOMRect>(window.realm(), window, x, y, width, height);
}

JS::NonnullGCPtr<DOMRect> DOMRect::create(HTML::Window& window, Gfx::FloatRect const& rect)
{
    return create_with_global_object(window, rect.x(), rect.y(), rect.width(), rect.height());
}

DOMRect::DOMRect(HTML::Window& window, double x, double y, double width, double height)
    : DOMRectReadOnly(window, x, y, width, height)
{
    set_prototype(&window.cached_web_prototype("DOMRect"));
}

DOMRect::~DOMRect() = default;

}
