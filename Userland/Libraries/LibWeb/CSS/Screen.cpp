/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Rect.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/ScreenPrototype.h>
#include <LibWeb/CSS/Screen.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Page/Page.h>

namespace Web::CSS {

JS::NonnullGCPtr<Screen> Screen::create(HTML::Window& window)
{
    return *window.heap().allocate<Screen>(window.realm(), window);
}

Screen::Screen(HTML::Window& window)
    : PlatformObject(window.realm())
    , m_window(window)
{
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ScreenPrototype>(window.realm(), "Screen"));
}

void Screen::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
}

Gfx::IntRect Screen::screen_rect() const
{
    return window().page()->screen_rect();
}

}
