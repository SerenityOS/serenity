/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SourceLocation.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/EventLoopPlugin.h>

namespace Web {

Page::Page(PageClient& client)
    : m_client(client)
{
    m_top_level_browsing_context = JS::make_handle(*HTML::BrowsingContext::create_a_new_top_level_browsing_context(*this));
}

Page::~Page() = default;

HTML::BrowsingContext& Page::focused_context()
{
    if (m_focused_context)
        return *m_focused_context;
    return top_level_browsing_context();
}

void Page::set_focused_browsing_context(Badge<EventHandler>, HTML::BrowsingContext& browsing_context)
{
    m_focused_context = browsing_context.make_weak_ptr();
}

void Page::load(const AK::URL& url)
{
    top_level_browsing_context().loader().load(url, FrameLoader::Type::Navigation);
}

void Page::load(LoadRequest& request)
{
    top_level_browsing_context().loader().load(request, FrameLoader::Type::Navigation);
}

void Page::load_html(StringView html, const AK::URL& url)
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

CSS::PreferredColorScheme Page::preferred_color_scheme() const
{
    return m_client.preferred_color_scheme();
}

bool Page::handle_mousewheel(Gfx::IntPoint position, unsigned button, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y)
{
    return top_level_browsing_context().event_handler().handle_mousewheel(position, button, buttons, modifiers, wheel_delta_x, wheel_delta_y);
}

bool Page::handle_mouseup(Gfx::IntPoint position, unsigned button, unsigned buttons, unsigned modifiers)
{
    return top_level_browsing_context().event_handler().handle_mouseup(position, button, buttons, modifiers);
}

bool Page::handle_mousedown(Gfx::IntPoint position, unsigned button, unsigned buttons, unsigned modifiers)
{
    return top_level_browsing_context().event_handler().handle_mousedown(position, button, buttons, modifiers);
}

bool Page::handle_mousemove(Gfx::IntPoint position, unsigned buttons, unsigned modifiers)
{
    return top_level_browsing_context().event_handler().handle_mousemove(position, buttons, modifiers);
}

bool Page::handle_doubleclick(Gfx::IntPoint position, unsigned button, unsigned buttons, unsigned modifiers)
{
    return top_level_browsing_context().event_handler().handle_doubleclick(position, button, buttons, modifiers);
}

bool Page::handle_keydown(KeyCode key, unsigned modifiers, u32 code_point)
{
    return focused_context().event_handler().handle_keydown(key, modifiers, code_point);
}

bool Page::handle_keyup(KeyCode key, unsigned modifiers, u32 code_point)
{
    return focused_context().event_handler().handle_keyup(key, modifiers, code_point);
}

HTML::BrowsingContext& Page::top_level_browsing_context()
{
    return *m_top_level_browsing_context;
}

HTML::BrowsingContext const& Page::top_level_browsing_context() const
{
    return *m_top_level_browsing_context;
}

template<typename ResponseType>
static ResponseType spin_event_loop_until_dialog_closed(PageClient& client, Optional<ResponseType>& response, SourceLocation location = SourceLocation::current())
{
    auto& event_loop = Web::HTML::current_settings_object().responsible_event_loop();

    ScopeGuard guard { [&] { event_loop.set_execution_paused(false); } };
    event_loop.set_execution_paused(true);

    Web::Platform::EventLoopPlugin::the().spin_until([&]() {
        return response.has_value() || !client.is_connection_open();
    });

    if (!client.is_connection_open()) {
        dbgln("WebContent client disconnected during {}. Exiting peacefully.", location.function_name());
        exit(0);
    }

    return response.release_value();
}

void Page::did_request_alert(DeprecatedString const& message)
{
    m_pending_dialog = PendingDialog::Alert;
    m_client.page_did_request_alert(message);

    if (!message.is_empty())
        m_pending_dialog_text = message;

    spin_event_loop_until_dialog_closed(m_client, m_pending_alert_response);
}

void Page::alert_closed()
{
    if (m_pending_dialog == PendingDialog::Alert) {
        m_pending_dialog = PendingDialog::None;
        m_pending_alert_response = Empty {};
        m_pending_dialog_text.clear();
    }
}

bool Page::did_request_confirm(DeprecatedString const& message)
{
    m_pending_dialog = PendingDialog::Confirm;
    m_client.page_did_request_confirm(message);

    if (!message.is_empty())
        m_pending_dialog_text = message;

    return spin_event_loop_until_dialog_closed(m_client, m_pending_confirm_response);
}

void Page::confirm_closed(bool accepted)
{
    if (m_pending_dialog == PendingDialog::Confirm) {
        m_pending_dialog = PendingDialog::None;
        m_pending_confirm_response = accepted;
        m_pending_dialog_text.clear();
    }
}

DeprecatedString Page::did_request_prompt(DeprecatedString const& message, DeprecatedString const& default_)
{
    m_pending_dialog = PendingDialog::Prompt;
    m_client.page_did_request_prompt(message, default_);

    if (!message.is_empty())
        m_pending_dialog_text = message;

    return spin_event_loop_until_dialog_closed(m_client, m_pending_prompt_response);
}

void Page::prompt_closed(DeprecatedString response)
{
    if (m_pending_dialog == PendingDialog::Prompt) {
        m_pending_dialog = PendingDialog::None;
        m_pending_prompt_response = move(response);
        m_pending_dialog_text.clear();
    }
}

void Page::dismiss_dialog()
{
    switch (m_pending_dialog) {
    case PendingDialog::None:
        break;
    case PendingDialog::Alert:
        m_client.page_did_request_accept_dialog();
        break;
    case PendingDialog::Confirm:
    case PendingDialog::Prompt:
        m_client.page_did_request_dismiss_dialog();
        break;
    }
}

void Page::accept_dialog()
{
    switch (m_pending_dialog) {
    case PendingDialog::None:
        break;
    case PendingDialog::Alert:
    case PendingDialog::Confirm:
    case PendingDialog::Prompt:
        m_client.page_did_request_accept_dialog();
        break;
    }
}

}
