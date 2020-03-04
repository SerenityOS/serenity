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

#include "IRCAppWindow.h"
#include "IRCChannel.h"
#include "IRCWindow.h"
#include "IRCWindowListModel.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/ToolBar.h>
#include <stdio.h>
#include <stdlib.h>

static IRCAppWindow* s_the;

IRCAppWindow& IRCAppWindow::the()
{
    return *s_the;
}

IRCAppWindow::IRCAppWindow()
    : m_client(IRCClient::construct())
{
    ASSERT(!s_the);
    s_the = this;

    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-irc-client.png"));

    update_title();
    set_rect(200, 200, 600, 400);
    setup_actions();
    setup_menus();
    setup_widgets();

    setup_client();
}

IRCAppWindow::~IRCAppWindow()
{
}

void IRCAppWindow::update_title()
{
    set_title(String::format("IRC Client: %s@%s:%d", m_client->nickname().characters(), m_client->hostname().characters(), m_client->port()));
}

void IRCAppWindow::setup_client()
{
    m_client->aid_create_window = [this](void* owner, IRCWindow::Type type, const String& name) {
        return create_window(owner, type, name);
    };
    m_client->aid_get_active_window = [this] {
        return static_cast<IRCWindow*>(m_container->active_widget());
    };
    m_client->aid_update_window_list = [this] {
        m_window_list->model()->update();
    };
    m_client->on_nickname_changed = [this](const String&) {
        update_title();
    };
    m_client->on_part_from_channel = [this](auto&) {
        update_part_action();
    };

    if (m_client->hostname().is_empty()) {
        auto& input_box = add<GUI::InputBox>("Enter server:", "Connect to server");
        auto result = input_box.exec();
        if (result == GUI::InputBox::ExecCancel)
            ::exit(0);

        m_client->set_server(input_box.text_value(), 6667);
    }
    update_title();
    bool success = m_client->connect();
    ASSERT(success);
}

void IRCAppWindow::setup_actions()
{
    m_join_action = GUI::Action::create("Join channel", { Mod_Ctrl, Key_J }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-join.png"), [&](auto&) {
        auto& input_box = add<GUI::InputBox>("Enter channel name:", "Join channel");
        if (input_box.exec() == GUI::InputBox::ExecOK && !input_box.text_value().is_empty())
            m_client->handle_join_action(input_box.text_value());
    });

    m_part_action = GUI::Action::create("Part from channel", { Mod_Ctrl, Key_P }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-part.png"), [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            // FIXME: Perhaps this action should have been disabled instead of allowing us to activate it.
            return;
        }
        m_client->handle_part_action(window->channel().name());
    });

    m_whois_action = GUI::Action::create("Whois user", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-whois.png"), [&](auto&) {
        auto input_box = GUI::InputBox::construct("Enter nickname:", "IRC WHOIS lookup", this);
        if (input_box->exec() == GUI::InputBox::ExecOK && !input_box->text_value().is_empty())
            m_client->handle_whois_action(input_box->text_value());
    });

    m_open_query_action = GUI::Action::create("Open query", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-open-query.png"), [&](auto&) {
        auto input_box = GUI::InputBox::construct("Enter nickname:", "Open IRC query with...", this);
        if (input_box->exec() == GUI::InputBox::ExecOK && !input_box->text_value().is_empty())
            m_client->handle_open_query_action(input_box->text_value());
    });

    m_close_query_action = GUI::Action::create("Close query", { Mod_Ctrl, Key_D }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-close-query.png"), [](auto&) {
        printf("FIXME: Implement close-query action\n");
    });

    m_change_nick_action = GUI::Action::create("Change nickname", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-nick.png"), [this](auto&) {
        auto input_box = GUI::InputBox::construct("Enter nickname:", "Change nickname", this);
        if (input_box->exec() == GUI::InputBox::ExecOK && !input_box->text_value().is_empty())
            m_client->handle_change_nick_action(input_box->text_value());
    });
}

void IRCAppWindow::setup_menus()
{
    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("IRC Client");
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        dbgprintf("Terminal: Quit menu activated!\n");
        GUI::Application::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto server_menu = GUI::Menu::construct("Server");
    server_menu->add_action(*m_change_nick_action);
    server_menu->add_separator();
    server_menu->add_action(*m_join_action);
    server_menu->add_action(*m_part_action);
    server_menu->add_separator();
    server_menu->add_action(*m_whois_action);
    server_menu->add_action(*m_open_query_action);
    server_menu->add_action(*m_close_query_action);
    menubar->add_menu(move(server_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [this](const GUI::Action&) {
        GUI::AboutDialog::show("IRC Client", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-irc-client.png"), this);
    }));
    menubar->add_menu(move(help_menu));

    GUI::Application::the().set_menubar(move(menubar));
}

void IRCAppWindow::setup_widgets()
{
    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(0);

    auto& toolbar = widget.add<GUI::ToolBar>();
    toolbar.set_has_frame(false);
    toolbar.add_action(*m_change_nick_action);
    toolbar.add_separator();
    toolbar.add_action(*m_join_action);
    toolbar.add_action(*m_part_action);
    toolbar.add_separator();
    toolbar.add_action(*m_whois_action);
    toolbar.add_action(*m_open_query_action);
    toolbar.add_action(*m_close_query_action);

    auto& outer_container = widget.add<GUI::Widget>();
    outer_container.set_layout<GUI::VerticalBoxLayout>();
    outer_container.layout()->set_margins({ 2, 0, 2, 2 });

    auto& horizontal_container = outer_container.add<GUI::HorizontalSplitter>();

    m_window_list = horizontal_container.add<GUI::TableView>();
    m_window_list->set_headers_visible(false);
    m_window_list->set_alternating_row_colors(false);
    m_window_list->set_size_columns_to_fit_content(true);
    m_window_list->set_model(m_client->client_window_list_model());
    m_window_list->set_activates_on_selection(true);
    m_window_list->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_window_list->set_preferred_size(100, 0);
    m_window_list->on_activation = [this](auto& index) {
        set_active_window(m_client->window_at(index.row()));
    };

    m_container = horizontal_container.add<GUI::StackWidget>();
    m_container->on_active_widget_change = [this](auto*) {
        update_part_action();
    };

    create_window(&m_client, IRCWindow::Server, "Server");
}

void IRCAppWindow::set_active_window(IRCWindow& window)
{
    m_container->set_active_widget(&window);
    window.clear_unread_count();
    auto index = m_window_list->model()->index(m_client->window_index(window));
    m_window_list->selection().set(index);
}

void IRCAppWindow::update_part_action()
{
    auto* window = static_cast<IRCWindow*>(m_container->active_widget());
    bool is_open_channel = window && window->type() == IRCWindow::Type::Channel && window->channel().is_open();
    m_part_action->set_enabled(is_open_channel);
}

NonnullRefPtr<IRCWindow> IRCAppWindow::create_window(void* owner, IRCWindow::Type type, const String& name)
{
    return m_container->add<IRCWindow>(m_client, owner, type, name);
}
