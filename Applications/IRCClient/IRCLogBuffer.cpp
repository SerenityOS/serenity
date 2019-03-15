#include "IRCLogBuffer.h"
#include "IRCLogBufferModel.h"
#include <stdio.h>
#include <time.h>

Retained<IRCLogBuffer> IRCLogBuffer::create()
{
    return adopt(*new IRCLogBuffer);
}

IRCLogBuffer::IRCLogBuffer()
{
    m_model = new IRCLogBufferModel(*this);
}

IRCLogBuffer::~IRCLogBuffer()
{
}

void IRCLogBuffer::add_message(char prefix, const String& name, const String& text)
{
    m_messages.enqueue({ time(nullptr), prefix, name, text });
    m_model->update();
}

void IRCLogBuffer::dump() const
{
    for (auto& message : m_messages) {
        printf("%u <%c%8s> %s\n", message.timestamp, message.prefix, message.sender.characters(), message.text.characters());
    }
}
