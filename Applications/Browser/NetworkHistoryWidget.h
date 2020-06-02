/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include <AK/URL.h>
#include <AK/HashMap.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Widget.h>
#include <LibWeb/NetworkHistoryModel.h>

namespace Browser {

class NetworkHistoryWidget final : public GUI::Widget {
    C_OBJECT(NetworkHistoryWidget)
public:
    virtual ~NetworkHistoryWidget();

    void register_callbacks();
    void unregister_callbacks();

    void on_page_navigation();

    Function<void(const URL&)> on_tab_open_request;

private:
    NetworkHistoryWidget();

    void update_view();

    bool m_clear_on_navigation { true };
    bool m_paused { false };
    bool m_cache_disabled { false };

    HashMap<u32, Web::NetworkHistoryModel::Entry> m_network_history;

    RefPtr<GUI::TableView> m_history_view;
    RefPtr<GUI::Menu> m_context_menu;
    URL m_context_menu_url;
};

}
