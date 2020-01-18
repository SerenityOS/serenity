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

#include "IRCChannel.h"
#include "IRCChannelMemberListModel.h"
#include "IRCClient.h"
#include <stdio.h>
#include <time.h>

IRCChannel::IRCChannel(IRCClient& client, const String& name)
    : m_client(client)
    , m_name(name)
    , m_log(IRCLogBuffer::create())
    , m_member_model(IRCChannelMemberListModel::create(*this))
{
    m_window = m_client.aid_create_window(this, IRCWindow::Channel, m_name);
    m_window->set_log_buffer(*m_log);
}

IRCChannel::~IRCChannel()
{
}

NonnullRefPtr<IRCChannel> IRCChannel::create(IRCClient& client, const String& name)
{
    return adopt(*new IRCChannel(client, name));
}

void IRCChannel::add_member(const String& name, char prefix)
{
    for (auto& member : m_members) {
        if (member.name == name) {
            member.prefix = prefix;
            return;
        }
    }
    m_members.append({ name, prefix });
    m_member_model->update();
}

void IRCChannel::remove_member(const String& name)
{
    m_members.remove_first_matching([&](auto& member) { return name == member.name; });
}

void IRCChannel::add_message(char prefix, const String& name, const String& text, Color color)
{
    log().add_message(prefix, name, text, color);
    window().did_add_message();
}

void IRCChannel::add_message(const String& text, Color color)
{
    log().add_message(text, color);
    window().did_add_message();
}

void IRCChannel::dump() const
{
    printf("IRCChannel{%p}: %s\n", this, m_name.characters());
    for (auto& member : m_members)
        printf("   (%c)%s\n", member.prefix ? member.prefix : ' ', member.name.characters());
    log().dump();
}

void IRCChannel::say(const String& text)
{
    m_client.send_privmsg(m_name, text);
    add_message(' ', m_client.nickname(), text);
}

void IRCChannel::handle_join(const String& nick, const String& hostmask)
{
    if (nick == m_client.nickname())
        m_open = true;
    add_message(String::format("*** %s [%s] has joined %s", nick.characters(), hostmask.characters(), m_name.characters()), Color::MidGreen);
}

void IRCChannel::handle_part(const String& nick, const String& hostmask)
{
    if (nick == m_client.nickname()) {
        m_open = false;
        m_members.clear();
        m_client.did_part_from_channel({}, *this);
    } else {
        remove_member(nick);
    }
    m_member_model->update();
    add_message(String::format("*** %s [%s] has parted from %s", nick.characters(), hostmask.characters(), m_name.characters()), Color::MidGreen);
}

void IRCChannel::handle_topic(const String& nick, const String& topic)
{
    if (nick.is_null())
        add_message(String::format("*** Topic is \"%s\"", topic.characters()), Color::MidBlue);
    else
        add_message(String::format("*** %s set topic to \"%s\"", nick.characters(), topic.characters()), Color::MidBlue);
}

void IRCChannel::notify_nick_changed(const String& old_nick, const String& new_nick)
{
    for (auto& member : m_members) {
        if (member.name == old_nick) {
            member.name = new_nick;
            add_message(String::format("~ %s changed nickname to %s", old_nick.characters(), new_nick.characters()), Color::MidMagenta);
            m_member_model->update();
            return;
        }
    }
}
