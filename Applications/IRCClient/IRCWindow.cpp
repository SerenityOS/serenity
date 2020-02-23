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
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibHTML/HtmlView.h>

IRCWindow::IRCWindow(IRCClient& client, void* owner, Type type, const String& name, GUI::Widget* parent)
    : GUI::Widget(parent)
    , m_client(client)
    , m_owner(owner)
    , m_type(type)
    , m_name(name)
{
    set_layout(make<GUI::VerticalBoxLayout>());

    // Make a container for the log buffer view + (optional) member list.
    auto container = add<GUI::HorizontalSplitter>();

    m_html_view = container->add<HtmlView>();

    if (m_type == Channel) {
        auto member_view = container->add<GUI::TableView>();
        member_view->set_headers_visible(false);
        member_view->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
        member_view->set_preferred_size(100, 0);
        member_view->set_alternating_row_colors(false);
        member_view->set_model(channel().member_model());
        member_view->set_activates_on_selection(true);
    }

    m_text_editor = add<GUI::TextBox>();
    m_text_editor->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_text_editor->set_preferred_size(0, 19);
    m_text_editor->on_return_pressed = [this] {
        if (m_type == Channel)
            m_client.handle_user_input_in_channel(m_name, m_text_editor->text());
        else if (m_type == Query)
            m_client.handle_user_input_in_query(m_name, m_text_editor->text());
        else if (m_type == Server)
            m_client.handle_user_input_in_server(m_text_editor->text());
        m_text_editor->clear();
    };

    m_client.register_subwindow(*this);
}

IRCWindow::~IRCWindow()
{
    m_client.unregister_subwindow(*this);
}

void IRCWindow::set_log_buffer(const IRCLogBuffer& log_buffer)
{
    m_log_buffer = &log_buffer;
    m_html_view->set_document(const_cast<Document*>(&log_buffer.document()));
}

bool IRCWindow::is_active() const
{
    return m_client.current_window() == this;
}

void IRCWindow::did_add_message()
{
    if (!is_active()) {
        ++m_unread_count;
        m_client.aid_update_window_list();
        return;
    }
    m_html_view->scroll_to_bottom();
}

void IRCWindow::clear_unread_count()
{
    if (!m_unread_count)
        return;
    m_unread_count = 0;
    m_client.aid_update_window_list();
}

int IRCWindow::unread_count() const
{
    return m_unread_count;
}
