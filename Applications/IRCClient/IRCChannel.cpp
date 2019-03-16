#include "IRCChannel.h"
#include "IRCClient.h"
#include "IRCChannelMemberListModel.h"
#include <stdio.h>
#include <time.h>

IRCChannel::IRCChannel(IRCClient& client, const String& name)
    : m_client(client)
    , m_name(name)
    , m_log(IRCLogBuffer::create())
{
    m_member_model = new IRCChannelMemberListModel(*this);
    m_window = m_client.aid_create_window(this, IRCWindow::Channel, m_name);
    m_window->set_log_buffer(*m_log);
}

IRCChannel::~IRCChannel()
{
}

Retained<IRCChannel> IRCChannel::create(IRCClient& client, const String& name)
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
    dump();
}

void IRCChannel::add_message(char prefix, const String& name, const String& text)
{
    window().did_add_message();
    log().add_message(prefix, name, text);
    dump();
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
    add_message(' ', "", String::format("*** %s [%s] has joined %s", nick.characters(), hostmask.characters(), m_name.characters()));
}

void IRCChannel::handle_part(const String& nick, const String& hostmask)
{
    if (nick == m_client.nickname())
        m_open = false;
    add_message(' ', "", String::format("*** %s [%s] has parted from %s", nick.characters(), hostmask.characters(), m_name.characters()));
}

void IRCChannel::handle_topic(const String& nick, const String& topic)
{
    if (nick == m_client.nickname())
        m_open = false;
    add_message(' ', "", String::format("*** %s set topic to \"%s\"", nick.characters(), topic.characters()));
}
