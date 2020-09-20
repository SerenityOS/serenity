/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "IRCWindow.h"
#include "IRCChannel.h"
#include "IRCChannelMemberListModel.h"
#include "IRCClient.h"
#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Notification.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibWeb/InProcessWebView.h>

IRCWindow::IRCWindow(IRCClient& client, void* owner, Type type, const String& name)
    : m_client(client)
    , m_owner(owner)
    , m_type(type)
    , m_name(name)
{
    set_layout<GUI::VerticalBoxLayout>();

    // Make a container for the log buffer view + (optional) member list.
    auto& container = add<GUI::HorizontalSplitter>();

    m_page_view = container.add<Web::InProcessWebView>();

    if (m_type == Channel) {
        auto& member_view = container.add<GUI::TableView>();
        member_view.set_column_headers_visible(false);
        member_view.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
        member_view.set_preferred_size(100, 0);
        member_view.set_alternating_row_colors(false);
        member_view.set_model(channel().member_model());
        member_view.set_activates_on_selection(true);
        member_view.on_activation = [&](auto& index) {
            if (!index.is_valid())
                return;
            auto nick = channel().member_model()->nick_at(member_view.selection().first());
            if (nick.is_empty())
                return;
            m_client->handle_open_query_action(m_client->nick_without_prefix(nick.characters()));
        };
        member_view.on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
            if (!index.is_valid())
                return;

            m_context_menu = GUI::Menu::construct();

            m_context_menu->add_action(GUI::Action::create("Open query", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-open-query.png"), [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_open_query_action(m_client->nick_without_prefix(nick.characters()));
            }));

            m_context_menu->add_action(GUI::Action::create("Whois", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-whois.png"), [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_whois_action(m_client->nick_without_prefix(nick.characters()));
            }));

            auto& context_control_menu = m_context_menu->add_submenu("Control");

            context_control_menu.add_action(GUI::Action::create("Voice", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_voice_user_action(m_name.characters(), m_client->nick_without_prefix(nick.characters()));
            }));

            context_control_menu.add_action(GUI::Action::create("DeVoice", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_devoice_user_action(m_name.characters(), m_client->nick_without_prefix(nick.characters()));
            }));

            context_control_menu.add_action(GUI::Action::create("Hop", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_hop_user_action(m_name.characters(), m_client->nick_without_prefix(nick.characters()));
            }));

            context_control_menu.add_action(GUI::Action::create("DeHop", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_dehop_user_action(m_name.characters(), m_client->nick_without_prefix(nick.characters()));
            }));

            context_control_menu.add_action(GUI::Action::create("Op", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_op_user_action(m_name.characters(), m_client->nick_without_prefix(nick.characters()));
            }));

            context_control_menu.add_action(GUI::Action::create("DeOp", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_deop_user_action(m_name.characters(), m_client->nick_without_prefix(nick.characters()));
            }));

            context_control_menu.add_separator();

            context_control_menu.add_action(GUI::Action::create("Kick", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                if (IRCClient::is_nick_prefix(nick[0]))
                    nick = nick.substring(1, nick.length() - 1);
                String value;
                if (GUI::InputBox::show(value, window(), "Enter reason:", "Reason") == GUI::InputBox::ExecOK)
                    m_client->handle_kick_user_action(m_name.characters(), m_client->nick_without_prefix(nick.characters()), value);
            }));

            auto& context_ctcp_menu = m_context_menu->add_submenu("CTCP");

            context_ctcp_menu.add_action(GUI::Action::create("User info", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_ctcp_user_action(m_client->nick_without_prefix(nick.characters()), "USERINFO");
            }));

            context_ctcp_menu.add_action(GUI::Action::create("Finger", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_ctcp_user_action(m_client->nick_without_prefix(nick.characters()), "FINGER");
            }));

            context_ctcp_menu.add_action(GUI::Action::create("Time", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_ctcp_user_action(m_client->nick_without_prefix(nick.characters()), "TIME");
            }));

            context_ctcp_menu.add_action(GUI::Action::create("Version", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_ctcp_user_action(m_client->nick_without_prefix(nick.characters()), "VERSION");
            }));

            context_ctcp_menu.add_action(GUI::Action::create("Client info", [&](const GUI::Action&) {
                auto nick = channel().member_model()->nick_at(member_view.selection().first());
                if (nick.is_empty())
                    return;
                m_client->handle_ctcp_user_action(m_client->nick_without_prefix(nick.characters()), "CLIENTINFO");
            }));

            m_context_menu->popup(event.screen_position());
        };
    }

    m_text_box = add<GUI::TextBox>();
    m_text_box->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_text_box->set_preferred_size(0, 19);
    m_text_box->on_return_pressed = [this] {
        if (m_type == Channel)
            m_client->handle_user_input_in_channel(m_name, m_text_box->text());
        else if (m_type == Query)
            m_client->handle_user_input_in_query(m_name, m_text_box->text());
        else if (m_type == Server)
            m_client->handle_user_input_in_server(m_text_box->text());
        m_text_box->add_current_text_to_history();
        m_text_box->clear();
    };
    m_text_box->set_history_enabled(true);
    m_text_box->set_placeholder("Message");

    m_client->register_subwindow(*this);
}

IRCWindow::~IRCWindow()
{
    m_client->unregister_subwindow(*this);
}

void IRCWindow::set_log_buffer(const IRCLogBuffer& log_buffer)
{
    m_log_buffer = &log_buffer;
    m_page_view->set_document(const_cast<Web::DOM::Document*>(&log_buffer.document()));
}

bool IRCWindow::is_active() const
{
    return m_client->current_window() == this;
}

void IRCWindow::post_notification_if_needed(const String& name, const String& message)
{
    if (name.is_null() || message.is_null())
        return;
    if (is_active() && window()->is_active())
        return;

    auto notification = GUI::Notification::construct();

    if (type() == Type::Channel) {
        if (!m_client->notify_on_mention())
            return;
        if (!message.contains(m_client->nickname()))
            return;

        StringBuilder builder;
        builder.append(name);
        builder.append(" in ");
        builder.append(m_name);
        notification->set_title(builder.to_string());
    } else {
        if (!m_client->notify_on_message())
            return;
        notification->set_title(name);
    }

    notification->set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-irc-client.png"));
    notification->set_text(message);
    notification->show();
}

void IRCWindow::did_add_message(const String& name, const String& message)
{
    post_notification_if_needed(name, message);

    if (!is_active()) {
        ++m_unread_count;
        m_client->aid_update_window_list();
        return;
    }
    m_page_view->scroll_to_bottom();
}

void IRCWindow::clear_unread_count()
{
    if (!m_unread_count)
        return;
    m_unread_count = 0;
    m_client->aid_update_window_list();
}

int IRCWindow::unread_count() const
{
    return m_unread_count;
}
