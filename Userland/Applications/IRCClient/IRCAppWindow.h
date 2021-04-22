/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "IRCClient.h"
#include "IRCWindow.h"
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class IRCAppWindow : public GUI::Window {
    C_OBJECT(IRCAppWindow);

public:
    virtual ~IRCAppWindow() override;

    static IRCAppWindow& the();

    void set_active_window(IRCWindow&);

private:
    IRCAppWindow(String server, int port);

    void setup_client();
    void setup_actions();
    void setup_menus();
    void setup_widgets();
    void update_title();
    void update_gui_actions();

    NonnullRefPtr<IRCWindow> create_window(void* owner, IRCWindow::Type, const String& name);
    NonnullRefPtr<IRCClient> m_client;
    RefPtr<GUI::StackWidget> m_container;
    RefPtr<GUI::TableView> m_window_list;
    RefPtr<GUI::Action> m_join_action;
    RefPtr<GUI::Action> m_list_channels_action;
    RefPtr<GUI::Action> m_part_action;
    RefPtr<GUI::Action> m_cycle_channel_action;
    RefPtr<GUI::Action> m_whois_action;
    RefPtr<GUI::Action> m_open_query_action;
    RefPtr<GUI::Action> m_close_query_action;
    RefPtr<GUI::Action> m_change_nick_action;
    RefPtr<GUI::Action> m_change_topic_action;
    RefPtr<GUI::Action> m_invite_user_action;
    RefPtr<GUI::Action> m_banlist_action;
    RefPtr<GUI::Action> m_voice_user_action;
    RefPtr<GUI::Action> m_devoice_user_action;
    RefPtr<GUI::Action> m_hop_user_action;
    RefPtr<GUI::Action> m_dehop_user_action;
    RefPtr<GUI::Action> m_op_user_action;
    RefPtr<GUI::Action> m_deop_user_action;
    RefPtr<GUI::Action> m_kick_user_action;
};
