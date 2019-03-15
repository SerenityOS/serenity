#include "IRCQuery.h"
#include "IRCClient.h"
#include <stdio.h>
#include <time.h>

IRCQuery::IRCQuery(IRCClient& client, const String& name)
    : m_client(client)
    , m_name(name)
    , m_log(IRCLogBuffer::create())
{
}

IRCQuery::~IRCQuery()
{
}

Retained<IRCQuery> IRCQuery::create(IRCClient& client, const String& name)
{
    return adopt(*new IRCQuery(client, name));
}

void IRCQuery::dump() const
{
    printf("IRCQuery{%p}: %s\n", this, m_name.characters());
    log().dump();
}

void IRCQuery::add_message(char prefix, const String& name, const String& text)
{
    log().add_message(prefix, name, text);
    dump();
}

void IRCQuery::say(const String& text)
{
    m_client.send_privmsg(m_name, text);
    add_message(' ', m_client.nickname(), text);
}
