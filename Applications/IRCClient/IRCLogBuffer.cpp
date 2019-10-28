#include "IRCLogBuffer.h"
#include <LibHTML/DOM/DocumentType.h>
#include <LibHTML/DOM/ElementFactory.h>
#include <LibHTML/DOM/HTMLBodyElement.h>
#include <LibHTML/DOM/Text.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <stdio.h>
#include <time.h>

NonnullRefPtr<IRCLogBuffer> IRCLogBuffer::create()
{
    return adopt(*new IRCLogBuffer);
}

IRCLogBuffer::IRCLogBuffer()
{
    m_document = adopt(*new Document);
    m_document->append_child(adopt(*new DocumentType(document())));
    auto html_element = create_element(document(), "html");
    m_document->append_child(html_element);
    auto head_element = create_element(document(), "head");
    html_element->append_child(head_element);
    auto style_element = create_element(document(), "style");
    style_element->append_child(adopt(*new Text(document(), "div { font-family: Csilla; font-weight: lighter; }")));
    head_element->append_child(style_element);
    auto body_element = create_element(document(), "body");
    html_element->append_child(body_element);
    m_container_element = body_element;
}

IRCLogBuffer::~IRCLogBuffer()
{
}

void IRCLogBuffer::add_message(char prefix, const String& name, const String& text, Color color)
{
    auto message_element = create_element(document(), "div");
    message_element->set_attribute("style", String::format("color: %s;", color.to_string().characters()));
    auto timestamp_element = create_element(document(), "span");
    auto now = time(nullptr);
    auto* tm = localtime(&now);
    auto timestamp_string = String::format("%02u:%02u:%02u ", tm->tm_hour, tm->tm_min, tm->tm_sec);
    timestamp_element->append_child(adopt(*new Text(document(), timestamp_string)));
    auto nick_element = create_element(document(), "b");
    nick_element->append_child(*new Text(document(), String::format("<%c%s> ", prefix ? prefix : ' ', name.characters())));
    auto text_element = create_element(document(), "span");
    text_element->append_child(*new Text(document(), text));
    message_element->append_child(timestamp_element);
    message_element->append_child(nick_element);
    message_element->append_child(text_element);
    m_container_element->append_child(message_element);

    m_document->force_layout();
}

void IRCLogBuffer::add_message(const String& text, Color color)
{
    auto message_element = create_element(document(), "div");
    message_element->set_attribute("style", String::format("color: %s;", color.to_string().characters()));
    auto timestamp_element = create_element(document(), "span");
    auto now = time(nullptr);
    auto* tm = localtime(&now);
    auto timestamp_string = String::format("%02u:%02u:%02u ", tm->tm_hour, tm->tm_min, tm->tm_sec);
    timestamp_element->append_child(adopt(*new Text(document(), timestamp_string)));
    auto text_element = create_element(document(), "span");
    text_element->append_child(*new Text(document(), text));
    message_element->append_child(timestamp_element);
    message_element->append_child(text_element);
    m_container_element->append_child(message_element);

    m_document->force_layout();
}

void IRCLogBuffer::dump() const
{
    // FIXME: Remove me?
}
