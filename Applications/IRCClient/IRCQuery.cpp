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
