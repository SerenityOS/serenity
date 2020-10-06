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
#include <LibGUI/ToolBarContainer.h>

static IRCAppWindow* s_the;

IRCAppWindow& IRCAppWindow::the()
{
    return *s_the;
}

IRCAppWindow::IRCAppWindow(String server, int port)
    : m_client(IRCClient::construct(server, port))
{
    ASSERT(!s_the);
    s_the = this;

    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-irc-client.png"));

    update_title();
    resize(600, 400);
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
    set_title(String::formatted("{}@{}:{} - IRC Client", m_client->nickname(), m_client->hostname(), m_client->port()));
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
        update_gui_actions();
    };

    if (m_client->hostname().is_empty()) {
        String value;
        if (GUI::InputBox::show(value, this, "Enter server:", "Connect to server") == GUI::InputBox::ExecCancel)
            ::exit(0);

        m_client->set_server(value, 6667);
    }
    update_title();
    bool success = m_client->connect();
    ASSERT(success);
}

void IRCAppWindow::setup_actions()
{
    m_join_action = GUI::Action::create("Join channel", { Mod_Ctrl, Key_J }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-join.png"), [&](auto&) {
        String value;
        if (GUI::InputBox::show(value, this, "Enter channel name:", "Join channel") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_join_action(value);
    });

    m_list_channels_action = GUI::Action::create("List channels", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-list.png"), [&](auto&) {
        m_client->handle_list_channels_action();
    });

    m_part_action = GUI::Action::create("Part from channel", { Mod_Ctrl, Key_P }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-part.png"), [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        m_client->handle_part_action(window->channel().name());
    });

    m_whois_action = GUI::Action::create("Whois user", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-whois.png"), [&](auto&) {
        String value;
        if (GUI::InputBox::show(value, this, "Enter nickname:", "IRC WHOIS lookup") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_whois_action(value);
    });

    m_open_query_action = GUI::Action::create("Open query", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-open-query.png"), [&](auto&) {
        String value;
        if (GUI::InputBox::show(value, this, "Enter nickname:", "Open IRC query with...") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_open_query_action(value);
    });

    m_close_query_action = GUI::Action::create("Close query", { Mod_Ctrl, Key_D }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-close-query.png"), [](auto&) {
        out() << "FIXME: Implement close-query action";
    });

    m_change_nick_action = GUI::Action::create("Change nickname", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-nick.png"), [this](auto&) {
        String value;
        if (GUI::InputBox::show(value, this, "Enter nickname:", "Change nickname") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_change_nick_action(value);
    });

    m_change_topic_action = GUI::Action::create("Change topic", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-topic.png"), [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter topic:", "Change topic") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_change_topic_action(window->channel().name(), value);
    });

    m_invite_user_action = GUI::Action::create("Invite user", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-invite.png"), [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter nick:", "Invite user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_invite_user_action(window->channel().name(), value);
    });

    m_banlist_action = GUI::Action::create("Ban list", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        m_client->handle_banlist_action(window->channel().name());
    });

    m_voice_user_action = GUI::Action::create("Voice user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter nick:", "Voice user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_voice_user_action(window->channel().name(), value);
    });

    m_devoice_user_action = GUI::Action::create("DeVoice user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter nick:", "DeVoice user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_devoice_user_action(window->channel().name(), value);
    });

    m_hop_user_action = GUI::Action::create("Hop user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter nick:", "Hop user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_hop_user_action(window->channel().name(), value);
    });

    m_dehop_user_action = GUI::Action::create("DeHop user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter nick:", "DeHop user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_dehop_user_action(window->channel().name(), value);
    });

    m_op_user_action = GUI::Action::create("Op user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter nick:", "Op user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_op_user_action(window->channel().name(), value);
    });

    m_deop_user_action = GUI::Action::create("DeOp user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(value, this, "Enter nick:", "DeOp user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_deop_user_action(window->channel().name(), value);
    });

    m_kick_user_action = GUI::Action::create("Kick user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String nick_value;
        if (GUI::InputBox::show(nick_value, this, "Enter nick:", "Kick user") != GUI::InputBox::ExecOK || nick_value.is_empty())
            return;
        String reason_value;
        if (GUI::InputBox::show(reason_value, this, "Enter reason:", "Reason") == GUI::InputBox::ExecOK)
            m_client->handle_kick_user_action(window->channel().name(), nick_value, reason_value.characters());
    });

    m_cycle_channel_action = GUI::Action::create("Cycle channel", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        m_client->handle_cycle_channel_action(window->channel().name());
    });
}

void IRCAppWindow::setup_menus()
{
    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("IRC Client");
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        dbg() << "Terminal: Quit menu activated!";
        GUI::Application::the()->quit();
        return;
    }));

    auto& server_menu = menubar->add_menu("Server");
    server_menu.add_action(*m_change_nick_action);
    server_menu.add_separator();
    server_menu.add_action(*m_join_action);
    server_menu.add_action(*m_list_channels_action);
    server_menu.add_separator();
    server_menu.add_action(*m_whois_action);
    server_menu.add_action(*m_open_query_action);
    server_menu.add_action(*m_close_query_action);

    auto& channel_menu = menubar->add_menu("Channel");
    channel_menu.add_action(*m_change_topic_action);
    channel_menu.add_action(*m_invite_user_action);
    channel_menu.add_action(*m_banlist_action);

    auto& channel_control_menu = channel_menu.add_submenu("Control");
    channel_control_menu.add_action(*m_voice_user_action);
    channel_control_menu.add_action(*m_devoice_user_action);
    channel_control_menu.add_action(*m_hop_user_action);
    channel_control_menu.add_action(*m_dehop_user_action);
    channel_control_menu.add_action(*m_op_user_action);
    channel_control_menu.add_action(*m_deop_user_action);
    channel_control_menu.add_separator();
    channel_control_menu.add_action(*m_kick_user_action);

    channel_menu.add_separator();
    channel_menu.add_action(*m_cycle_channel_action);
    channel_menu.add_action(*m_part_action);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [this](auto&) {
        GUI::AboutDialog::show("IRC Client", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-irc-client.png"), this);
    }));

    GUI::Application::the()->set_menubar(move(menubar));
}

void IRCAppWindow::setup_widgets()
{
    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(0);

    auto& toolbar_container = widget.add<GUI::ToolBarContainer>();
    auto& toolbar = toolbar_container.add<GUI::ToolBar>();
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
    m_window_list->set_column_headers_visible(false);
    m_window_list->set_alternating_row_colors(false);
    m_window_list->set_model(m_client->client_window_list_model());
    m_window_list->set_activates_on_selection(true);
    m_window_list->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_window_list->set_preferred_size(100, 0);
    m_window_list->on_activation = [this](auto& index) {
        set_active_window(m_client->window_at(index.row()));
    };

    m_container = horizontal_container.add<GUI::StackWidget>();
    m_container->on_active_widget_change = [this](auto*) {
        update_gui_actions();
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

void IRCAppWindow::update_gui_actions()
{
    auto* window = static_cast<IRCWindow*>(m_container->active_widget());
    bool is_open_channel = window && window->type() == IRCWindow::Type::Channel && window->channel().is_open();
    m_change_topic_action->set_enabled(is_open_channel);
    m_invite_user_action->set_enabled(is_open_channel);
    m_banlist_action->set_enabled(is_open_channel);
    m_voice_user_action->set_enabled(is_open_channel);
    m_devoice_user_action->set_enabled(is_open_channel);
    m_hop_user_action->set_enabled(is_open_channel);
    m_dehop_user_action->set_enabled(is_open_channel);
    m_op_user_action->set_enabled(is_open_channel);
    m_deop_user_action->set_enabled(is_open_channel);
    m_kick_user_action->set_enabled(is_open_channel);
    m_cycle_channel_action->set_enabled(is_open_channel);
    m_part_action->set_enabled(is_open_channel);
}

NonnullRefPtr<IRCWindow> IRCAppWindow::create_window(void* owner, IRCWindow::Type type, const String& name)
{
    return m_container->add<IRCWindow>(m_client, owner, type, name);
}
