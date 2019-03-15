#include "IRCChannel.h"
#include "IRCClient.h"
#include <stdio.h>
#include <time.h>

IRCChannel::IRCChannel(IRCClient& client, const String& name)
    : m_client(client)
    , m_name(name)
    , m_log(IRCLogBuffer::create())
{
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
    log().add_message(prefix, name, text);
    dump();
}

void IRCChannel::dump() const
{
    printf("IRCChannel{%p}: %s\n", this, m_name.characters());
    for (auto& member : m_members) {
        printf("   (%c)%s\n", member.prefix ? member.prefix : ' ', member.name.characters());
    }
    log().dump();
}
