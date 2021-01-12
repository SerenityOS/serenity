/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/Page/Page.h>

namespace Web {

Page::Page(PageClient& client)
    : m_client(client)
{
    m_main_frame = Frame::create(*this);
}

Page::~Page()
{
}

Frame& Page::focused_frame()
{
    if (m_focused_frame)
        return *m_focused_frame;
    return main_frame();
}

void Page::set_focused_frame(Badge<EventHandler>, Frame& frame)
{
    m_focused_frame = frame.make_weak_ptr();
}

void Page::load(const URL& url)
{
    main_frame().loader().load(url, FrameLoader::Type::Navigation);
}

void Page::load(const LoadRequest& request)
{
    main_frame().loader().load(request, FrameLoader::Type::Navigation);
}

void Page::load_html(const StringView& html, const URL& url)
{
    main_frame().loader().load_html(html, url);
}

Gfx::Palette Page::palette() const
{
    return m_client.palette();
}

bool Page::handle_mouseup(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    return main_frame().event_handler().handle_mouseup(position, button, modifiers);
}

bool Page::handle_mousedown(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    return main_frame().event_handler().handle_mousedown(position, button, modifiers);
}

bool Page::handle_mousemove(const Gfx::IntPoint& position, unsigned buttons, unsigned modifiers)
{
    return main_frame().event_handler().handle_mousemove(position, buttons, modifiers);
}

bool Page::handle_keydown(KeyCode key, unsigned modifiers, u32 code_point)
{
    return focused_frame().event_handler().handle_keydown(key, modifiers, code_point);
}

}
