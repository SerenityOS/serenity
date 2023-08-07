/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
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

WebIDL::ExceptionOr<JS::NonnullGCPtr<Screen>> Screen::create(HTML::Window& window)
{
    return MUST_OR_THROW_OOM(window.heap().allocate<Screen>(window.realm(), window));
}

Screen::Screen(HTML::Window& window)
    : PlatformObject(window.realm())
    , m_window(window)
{
}

void Screen::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ScreenPrototype>(realm, "Screen"));
}

void Screen::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
}

Gfx::IntRect Screen::screen_rect() const
{
    auto screen_rect_in_css_pixels = window().page()->web_exposed_screen_area();
    return {
        screen_rect_in_css_pixels.x().to_int(),
        screen_rect_in_css_pixels.y().to_int(),
        screen_rect_in_css_pixels.width().to_int(),
        screen_rect_in_css_pixels.height().to_int()
    };
}

}
