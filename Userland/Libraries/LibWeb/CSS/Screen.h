/*
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        return adopt(*new Screen(window));
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
