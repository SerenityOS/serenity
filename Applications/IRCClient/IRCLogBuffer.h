#pragma once

#include <AK/AKString.h>
#include <AK/CircularQueue.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class IRCLogBufferModel;

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

    const IRCLogBufferModel* model() const { return m_model; }
    IRCLogBufferModel* model() { return m_model; }

private:
    IRCLogBuffer();

    IRCLogBufferModel* m_model { nullptr };

    CircularQueue<Message, 1000> m_messages;
};
