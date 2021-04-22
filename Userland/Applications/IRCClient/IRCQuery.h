/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "IRCLogBuffer.h"
#include <AK/CircularQueue.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>

class IRCClient;
class IRCWindow;

class IRCQuery : public RefCounted<IRCQuery> {
public:
    static NonnullRefPtr<IRCQuery> create(IRCClient&, const String& name);
    ~IRCQuery();

    String name() const { return m_name; }
    void add_message(char prefix, const String& name, const String& text, Color = Color::Black);
    void add_message(const String& text, Color = Color::Black);

    const IRCLogBuffer& log() const { return *m_log; }
    IRCLogBuffer& log() { return *m_log; }

    void say(const String&);

    IRCWindow& window() { return *m_window; }
    const IRCWindow& window() const { return *m_window; }

private:
    IRCQuery(IRCClient&, const String& name);

    NonnullRefPtr<IRCClient> m_client;
    String m_name;
    RefPtr<IRCWindow> m_window;

    NonnullRefPtr<IRCLogBuffer> m_log;
};
