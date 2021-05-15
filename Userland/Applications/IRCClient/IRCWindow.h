/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibWeb/Forward.h>

class IRCChannel;
class IRCClient;
class IRCQuery;
class IRCLogBuffer;

class IRCWindow : public GUI::Widget {
    C_OBJECT(IRCWindow)
public:
    enum Type {
        Server,
        Channel,
        Query,
    };

    virtual ~IRCWindow() override;

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    Type type() const { return m_type; }

    void set_log_buffer(const IRCLogBuffer&);

    bool is_active() const;

    int unread_count() const;
    void clear_unread_count();

    void did_add_message(const String& name = {}, const String& message = {});

    IRCChannel& channel() { return *(IRCChannel*)m_owner; }
    const IRCChannel& channel() const { return *(const IRCChannel*)m_owner; }

    IRCQuery& query() { return *(IRCQuery*)m_owner; }
    const IRCQuery& query() const { return *(const IRCQuery*)m_owner; }

private:
    IRCWindow(IRCClient&, void* owner, Type, const String& name);

    void post_notification_if_needed(const String& name, const String& message);

    NonnullRefPtr<IRCClient> m_client;
    void* m_owner { nullptr };
    Type m_type;
    String m_name;
    RefPtr<Web::InProcessWebView> m_page_view;
    RefPtr<GUI::TextBox> m_text_box;
    RefPtr<IRCLogBuffer> m_log_buffer;
    RefPtr<GUI::Menu> m_context_menu;
    int m_unread_count { 0 };
};
