#pragma once

#include "IRCLogBuffer.h"
#include <AK/AKString.h>
#include <AK/CircularQueue.h>
#include <AK/RetainPtr.h>
#include <AK/Retainable.h>
#include <AK/Vector.h>

class IRCClient;
class IRCWindow;

class IRCQuery : public Retainable<IRCQuery> {
public:
    static Retained<IRCQuery> create(IRCClient&, const String& name);
    ~IRCQuery();

    String name() const { return m_name; }
    void add_message(char prefix, const String& name, const String& text, Color = Color::Black);

    void dump() const;

    const IRCLogBuffer& log() const { return *m_log; }
    IRCLogBuffer& log() { return *m_log; }

    void say(const String&);

    IRCWindow& window() { return *m_window; }
    const IRCWindow& window() const { return *m_window; }

private:
    IRCQuery(IRCClient&, const String& name);

    IRCClient& m_client;
    String m_name;
    IRCWindow* m_window { nullptr };

    Retained<IRCLogBuffer> m_log;
};
