/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Rect.h>
#include <LibWeb/CSS/Screen.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Page/Page.h>

namespace Web::CSS {

Screen::Screen(DOM::Window& window)
    : RefCountForwarder(window)
{
}

Gfx::IntRect Screen::screen_rect() const
{
    return window().page()->screen_rect();
}

}
