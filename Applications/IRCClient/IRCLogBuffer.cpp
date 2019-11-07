#include "IRCLogBuffer.h"
#include <AK/StringBuilder.h>
#include <LibHTML/DOM/DocumentFragment.h>
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

static String timestamp_string()
{
    auto now = time(nullptr);
    auto* tm = localtime(&now);
    return String::format("%02u:%02u:%02u ", tm->tm_hour, tm->tm_min, tm->tm_sec);
}

void IRCLogBuffer::add_message(char prefix, const String& name, const String& text, Color color)
{
    auto nick_string = String::format("<%c%s> ", prefix ? prefix : ' ', name.characters());
    auto html = String::format(
        "<div style=\"color: %s\">"
        "<span>%s</span>"
        "<b>%s</b>"
        "<span>%s</span>"
        "</div>",
        color.to_string().characters(),
        timestamp_string().characters(),
        escape_html_entities(nick_string).characters(),
        escape_html_entities(text).characters());
    auto fragment = parse_html_fragment(*m_document, html);
    m_container_element->append_child(fragment->remove_child(*fragment->first_child()));
    m_document->force_layout();
}

void IRCLogBuffer::add_message(const String& text, Color color)
{
    auto html = String::format(
        "<div style=\"color: %s\">"
        "<span>%s</span>"
        "<span>%s</span>"
        "</div>",
        color.to_string().characters(),
        timestamp_string().characters(),
        escape_html_entities(text).characters());
    auto fragment = parse_html_fragment(*m_document, html);
    m_container_element->append_child(fragment->remove_child(*fragment->first_child()));
    m_document->force_layout();
}

void IRCLogBuffer::dump() const
{
    // FIXME: Remove me?
}
