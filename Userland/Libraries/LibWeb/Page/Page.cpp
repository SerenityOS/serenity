/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

Gfx::IntRect Page::screen_rect() const
{
    return m_client.screen_rect();
}

bool Page::handle_mousewheel(const Gfx::IntPoint& position, unsigned button, unsigned modifiers, int wheel_delta)
{
    return main_frame().event_handler().handle_mousewheel(position, button, modifiers, wheel_delta);
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
