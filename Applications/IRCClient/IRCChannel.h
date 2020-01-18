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

#include "IRCLogBuffer.h"
#include <AK/String.h>
#include <AK/CircularQueue.h>
#include <AK/RefPtr.h>
#include <AK/RefCounted.h>
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

    void dump() const;

    void say(const String&);

    const IRCLogBuffer& log() const { return *m_log; }
    IRCLogBuffer& log() { return *m_log; }

    IRCChannelMemberListModel* member_model() { return m_member_model.ptr(); }
    const IRCChannelMemberListModel* member_model() const { return m_member_model.ptr(); }

    int member_count() const { return m_members.size(); }
    String member_at(int i) { return m_members[i].name; }

    void handle_join(const String& nick, const String& hostmask);
    void handle_part(const String& nick, const String& hostmask);
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
