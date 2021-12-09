/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCountForwarder.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class Screen final
    : public RefCountForwarder<DOM::Window>
    , public Bindings::Wrappable {

public:
    using WrapperType = Bindings::ScreenWrapper;
    using AllowOwnPtr = TrueType;

    static NonnullOwnPtr<Screen> create(Badge<DOM::Window>, DOM::Window& window)
    {
        return adopt_own(*new Screen(window));
    }

    i32 width() const { return screen_rect().width(); }
    i32 height() const { return screen_rect().height(); }
    i32 avail_width() const { return screen_rect().width(); }
    i32 avail_height() const { return screen_rect().height(); }
    u32 color_depth() const { return 24; }
    u32 pixel_depth() const { return 24; }

private:
    explicit Screen(DOM::Window&);

    DOM::Window const& window() const { return ref_count_target(); }

    Gfx::IntRect screen_rect() const;
};

}
