/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

class Screen final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Screen, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Screen>> create(HTML::Window&);

    i32 width() const { return screen_rect().width(); }
    i32 height() const { return screen_rect().height(); }
    i32 avail_width() const { return screen_rect().width(); }
    i32 avail_height() const { return screen_rect().height(); }
    u32 color_depth() const { return 24; }
    u32 pixel_depth() const { return 24; }

private:
    explicit Screen(HTML::Window&);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    HTML::Window const& window() const { return *m_window; }

    Gfx::IntRect screen_rect() const;

    JS::NonnullGCPtr<HTML::Window> m_window;
};

}
