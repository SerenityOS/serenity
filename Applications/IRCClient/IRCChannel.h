#pragma once

#include <AK/AKString.h>
#include <AK/CircularQueue.h>
#include <AK/Vector.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include "IRCLogBuffer.h"

class IRCClient;

class IRCChannel : public Retainable<IRCChannel> {
public:
    static Retained<IRCChannel> create(IRCClient&, const String&);
    ~IRCChannel();

    bool is_open() const { return m_open; }
    void set_open(bool b) { m_open = b; }

    String name() const { return m_name; }

    void add_member(const String& name, char prefix);
    void remove_member(const String& name);

    void add_message(char prefix, const String& name, const String& text);

    void dump() const;

    void say(const String&);

    const IRCLogBuffer& log() const { return *m_log; }
    IRCLogBuffer& log() { return *m_log; }

private:
    IRCChannel(IRCClient&, const String&);

    IRCClient& m_client;
    String m_name;
    struct Member {
        String name;
        char prefix { 0 };
    };
    Vector<Member> m_members;
    bool m_open { false };

    Retained<IRCLogBuffer> m_log;
};
