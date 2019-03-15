#pragma once

#include <AK/AKString.h>
#include <AK/CircularQueue.h>
#include <AK/Vector.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include "IRCLogBuffer.h"

class IRCClient;

class IRCQuery : public Retainable<IRCQuery> {
public:
    static Retained<IRCQuery> create(IRCClient&, const String& name);
    ~IRCQuery();

    String name() const { return m_name; }
    void add_message(char prefix, const String& name, const String& text);

    void dump() const;

    const IRCLogBuffer& log() const { return *m_log; }
    IRCLogBuffer& log() { return *m_log; }

    void say(const String&);

private:
    IRCQuery(IRCClient&, const String& name);

    IRCClient& m_client;
    String m_name;

    Retained<IRCLogBuffer> m_log;
};
