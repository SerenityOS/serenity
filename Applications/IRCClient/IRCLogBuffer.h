#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibDraw/Color.h>
#include <LibHTML/DOM/Document.h>

class IRCLogBuffer : public RefCounted<IRCLogBuffer> {
public:
    static NonnullRefPtr<IRCLogBuffer> create();
    ~IRCLogBuffer();

    struct Message {
        time_t timestamp { 0 };
        char prefix { 0 };
        String sender;
        String text;
        Color color { Color::Black };
    };

    void add_message(char prefix, const String& name, const String& text, Color = Color::Black);
    void add_message(const String& text, Color = Color::Black);
    void dump() const;

    const Document& document() const { return *m_document; }
    Document& document() { return *m_document; }

private:
    IRCLogBuffer();
    RefPtr<Document> m_document;
    RefPtr<Element> m_container_element;
};
