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
    IRCAppWindow();

    void setup_client();
    void setup_actions();
    void setup_menus();
    void setup_widgets();
    void update_title();
    void update_part_action();

    IRCWindow& create_window(void* owner, IRCWindow::Type, const String& name);
    NonnullRefPtr<IRCClient> m_client;
    RefPtr<GUI::StackWidget> m_container;
    RefPtr<GUI::TableView> m_window_list;
    RefPtr<GUI::Action> m_join_action;
    RefPtr<GUI::Action> m_part_action;
    RefPtr<GUI::Action> m_whois_action;
    RefPtr<GUI::Action> m_open_query_action;
    RefPtr<GUI::Action> m_close_query_action;
    RefPtr<GUI::Action> m_change_nick_action;
};
