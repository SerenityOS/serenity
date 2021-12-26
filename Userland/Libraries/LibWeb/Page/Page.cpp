/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Page/BrowsingContext.h>
#include <LibWeb/Page/Page.h>

namespace Web {

Page::Page(PageClient& client)
    : m_client(client)
{
    m_top_level_browsing_context = BrowsingContext::create(*this);
}

Page::~Page()
{
}

BrowsingContext& Page::focused_context()
{
    if (m_focused_context)
        return *m_focused_context;
    return top_level_browsing_context();
}

void Page::set_focused_browsing_context(Badge<EventHandler>, BrowsingContext& browsing_context)
{
    m_focused_context = browsing_context.make_weak_ptr();
}

void Page::load(const URL& url)
{
    top_level_browsing_context().loader().load(url, FrameLoader::Type::Navigation);
}

void Page::load(const LoadRequest& request)
{
    top_level_browsing_context().loader().load(request, FrameLoader::Type::Navigation);
}

void Page::load_html(const StringView& html, const URL& url)
{
    top_level_browsing_context().loader().load_html(html, url);
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
    return top_level_browsing_context().event_handler().handle_mousewheel(position, button, modifiers, wheel_delta);
}

bool Page::handle_mouseup(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    return top_level_browsing_context().event_handler().handle_mouseup(position, button, modifiers);
}

bool Page::handle_mousedown(const Gfx::IntPoint& position, unsigned button, unsigned modifiers)
{
    return top_level_browsing_context().event_handler().handle_mousedown(position, button, modifiers);
}

bool Page::handle_mousemove(const Gfx::IntPoint& position, unsigned buttons, unsigned modifiers)
{
    return top_level_browsing_context().event_handler().handle_mousemove(position, buttons, modifiers);
}

bool Page::handle_keydown(KeyCode key, unsigned modifiers, u32 code_point)
{
    return focused_context().event_handler().handle_keydown(key, modifiers, code_point);
}

}
