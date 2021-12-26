#include "IRCQuery.h"
#include "IRCClient.h"
#include <stdio.h>
#include <time.h>

IRCQuery::IRCQuery(IRCClient& client, const String& name)
    : m_client(client)
    , m_name(name)
    , m_log(IRCLogBuffer::create())
{
    m_window = m_client.aid_create_window(this, IRCWindow::Query, m_name);
    m_window->set_log_buffer(*m_log);
}

IRCQuery::~IRCQuery()
{
}

NonnullRefPtr<IRCQuery> IRCQuery::create(IRCClient& client, const String& name)
{
    return adopt(*new IRCQuery(client, name));
}

void IRCQuery::dump() const
{
    printf("IRCQuery{%p}: %s\n", this, m_name.characters());
    log().dump();
}

void IRCQuery::add_message(char prefix, const String& name, const String& text, Color color)
{
    log().add_message(prefix, name, text, color);
    window().did_add_message();
}

void IRCQuery::say(const String& text)
{
    m_client.send_privmsg(m_name, text);
    add_message(' ', m_client.nickname(), text);
}
