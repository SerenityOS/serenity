/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class Screen final
    : public RefCounted<Screen>
    , public Bindings::Wrappable {

public:
    using WrapperType = Bindings::ScreenWrapper;

    static NonnullRefPtr<Screen> create(DOM::Window& window)
    {
        return adopt_ref(*new Screen(window));
    }

    i32 width() const { return screen_rect().width(); }
    i32 height() const { return screen_rect().height(); }
    i32 avail_width() const { return screen_rect().width(); }
    i32 avail_height() const { return screen_rect().height(); }
    u32 color_depth() const { return 24; }
    u32 pixel_depth() const { return 24; }

private:
    explicit Screen(DOM::Window&);

    Gfx::IntRect screen_rect() const;

    DOM::Window& m_window;
};

}
