/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "IRCLogBuffer.h"
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>

class IRCClient;
class IRCChannelMemberListModel;
class IRCWindow;

class IRCChannel : public RefCounted<IRCChannel> {
public:
    static NonnullRefPtr<IRCChannel> create(IRCClient&, const String&);
    ~IRCChannel();

    bool is_open() const { return m_open; }
    void set_open(bool b) { m_open = b; }

    String name() const { return m_name; }

    void add_member(const String& name, char prefix);
    void remove_member(const String& name);

    void add_message(char prefix, const String& name, const String& text, Color = Color::Black);
    void add_message(const String& text, Color = Color::Black);

    void say(const String&);

    const IRCLogBuffer& log() const { return *m_log; }
    IRCLogBuffer& log() { return *m_log; }

    IRCChannelMemberListModel* member_model() { return m_member_model.ptr(); }
    const IRCChannelMemberListModel* member_model() const { return m_member_model.ptr(); }

    int member_count() const { return m_members.size(); }
    String member_at(int i) { return m_members[i].name; }

    void handle_join(const String& nick, const String& hostmask);
    void handle_part(const String& nick, const String& hostmask);
    void handle_quit(const String& nick, const String& hostmask, const String& message);
    void handle_topic(const String& nick, const String& topic);

    IRCWindow& window() { return *m_window; }
    const IRCWindow& window() const { return *m_window; }

    String topic() const { return m_topic; }

    void notify_nick_changed(const String& old_nick, const String& new_nick);

private:
    IRCChannel(IRCClient&, const String&);

    IRCClient& m_client;
    String m_name;
    String m_topic;
    struct Member {
        String name;
        char prefix { 0 };
    };
    Vector<Member> m_members;
    bool m_open { false };

    NonnullRefPtr<IRCLogBuffer> m_log;
    NonnullRefPtr<IRCChannelMemberListModel> m_member_model;
    IRCWindow* m_window { nullptr };
};
