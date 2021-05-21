/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IRCAppWindow.h"
#include "IRCChannel.h"
#include "IRCWindow.h"
#include "IRCWindowListModel.h"
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>

static IRCAppWindow* s_the;

IRCAppWindow& IRCAppWindow::the()
{
    return *s_the;
}

IRCAppWindow::IRCAppWindow(String server, int port)
    : m_client(IRCClient::construct(server, port))
{
    VERIFY(!s_the);
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
        if (GUI::InputBox::show(this, value, "Enter server:", "Connect to server") == GUI::InputBox::ExecCancel)
            ::exit(0);

        m_client->set_server(value, 6667);
    }
    update_title();
    bool success = m_client->connect();
    VERIFY(success);
}

void IRCAppWindow::setup_actions()
{
    m_join_action = GUI::Action::create("&Join Channel...", { Mod_Ctrl, Key_J }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-join.png"), [&](auto&) {
        String value;
        if (GUI::InputBox::show(this, value, "Enter channel name:", "Join Channel") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_join_action(value);
    });

    m_list_channels_action = GUI::Action::create("&List Channels", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-list.png"), [&](auto&) {
        m_client->handle_list_channels_action();
    });

    m_part_action = GUI::Action::create("&Part from Channel", { Mod_Ctrl, Key_P }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-part.png"), [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        m_client->handle_part_action(window->channel().name());
    });

    m_whois_action = GUI::Action::create("&Whois User...", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-whois.png"), [&](auto&) {
        String value;
        if (GUI::InputBox::show(this, value, "Enter nickname:", "Whois User") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_whois_action(value);
    });

    m_open_query_action = GUI::Action::create("Open &Query...", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-open-query.png"), [&](auto&) {
        String value;
        if (GUI::InputBox::show(this, value, "Enter nickname:", "Open Query") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_open_query_action(value);
    });

    m_close_query_action = GUI::Action::create("&Close Query", { Mod_Ctrl, Key_D }, Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-close-query.png"), [](auto&) {
        outln("FIXME: Implement close-query action");
    });

    m_change_nick_action = GUI::Action::create("Change &Nickname...", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-nick.png"), [this](auto&) {
        String value;
        if (GUI::InputBox::show(this, value, "Enter nickname:", "Change Nickname") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_change_nick_action(value);
    });

    m_change_topic_action = GUI::Action::create("Change &Topic...", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-topic.png"), [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter topic:", "Change Topic") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_change_topic_action(window->channel().name(), value);
    });

    m_invite_user_action = GUI::Action::create("&Invite User...", Gfx::Bitmap::load_from_file("/res/icons/16x16/irc-invite.png"), [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter nick:", "Invite User") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_invite_user_action(window->channel().name(), value);
    });

    m_banlist_action = GUI::Action::create("&Ban List", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        m_client->handle_banlist_action(window->channel().name());
    });

    m_voice_user_action = GUI::Action::create("&Voice User...", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter nick:", "Voice User") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_voice_user_action(window->channel().name(), value);
    });

    m_devoice_user_action = GUI::Action::create("DeVoice User...", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter nick:", "DeVoice user") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_devoice_user_action(window->channel().name(), value);
    });

    m_hop_user_action = GUI::Action::create("Hop User", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter nick:", "Hop User") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_hop_user_action(window->channel().name(), value);
    });

    m_dehop_user_action = GUI::Action::create("DeHop User", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter nick:", "DeHop User") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_dehop_user_action(window->channel().name(), value);
    });

    m_op_user_action = GUI::Action::create("&Op User", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter nick:", "Op User") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_op_user_action(window->channel().name(), value);
    });

    m_deop_user_action = GUI::Action::create("DeOp user", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String value;
        if (GUI::InputBox::show(this, value, "Enter nick:", "DeOp User") == GUI::InputBox::ExecOK && !value.is_empty())
            m_client->handle_deop_user_action(window->channel().name(), value);
    });

    m_kick_user_action = GUI::Action::create("&Kick User", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        String nick_value;
        if (GUI::InputBox::show(this, nick_value, "Enter nick:", "Kick User") != GUI::InputBox::ExecOK || nick_value.is_empty())
            return;
        String reason_value;
        if (GUI::InputBox::show(this, reason_value, "Enter reason:", "Reason") == GUI::InputBox::ExecOK)
            m_client->handle_kick_user_action(window->channel().name(), nick_value, reason_value.characters());
    });

    m_cycle_channel_action = GUI::Action::create("C&ycle Channel", [this](auto&) {
        auto* window = m_client->current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            return;
        }
        m_client->handle_cycle_channel_action(window->channel().name());
    });
}

void IRCAppWindow::setup_menus()
{
    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& server_menu = menubar->add_menu("&Server");
    server_menu.add_action(*m_change_nick_action);
    server_menu.add_separator();
    server_menu.add_action(*m_join_action);
    server_menu.add_action(*m_list_channels_action);
    server_menu.add_separator();
    server_menu.add_action(*m_whois_action);
    server_menu.add_action(*m_open_query_action);
    server_menu.add_action(*m_close_query_action);

    auto& channel_menu = menubar->add_menu("&Channel");
    channel_menu.add_action(*m_change_topic_action);
    channel_menu.add_action(*m_invite_user_action);
    channel_menu.add_action(*m_banlist_action);

    auto& channel_control_menu = channel_menu.add_submenu("Con&trol");
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

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("IRC Client", GUI::Icon::default_icon("app-irc-client"), this));

    set_menubar(move(menubar));
}

void IRCAppWindow::setup_widgets()
{
    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(0);

    auto& toolbar_container = widget.add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();
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
    m_window_list->set_fixed_width(100);
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
