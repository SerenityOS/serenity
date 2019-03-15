#pragma once

#include <AK/AKString.h>
#include <AK/CircularQueue.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class IRCLogBuffer : public Retainable<IRCLogBuffer> {
public:
    static Retained<IRCLogBuffer> create();
    ~IRCLogBuffer();

    struct Message {
        time_t timestamp { 0 };
        char prefix { 0 };
        String sender;
        String text;
    };

    int count() const { return m_messages.size(); }
    const Message& at(int index) const { return m_messages.at(index); }

    void add_message(char prefix, const String& name, const String& text);

    void dump() const;

private:
    IRCLogBuffer();

    CircularQueue<Message, 1000> m_messages;
};
