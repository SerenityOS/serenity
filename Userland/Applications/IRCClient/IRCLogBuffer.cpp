/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IRCLogBuffer.h"
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <time.h>

NonnullRefPtr<IRCLogBuffer> IRCLogBuffer::create()
{
    return adopt_ref(*new IRCLogBuffer);
}

IRCLogBuffer::IRCLogBuffer()
{
    m_document = Web::DOM::Document::create();
    m_document->append_child(adopt_ref(*new Web::DOM::DocumentType(document())));
    auto html_element = m_document->create_element("html");
    m_document->append_child(html_element);
    auto head_element = m_document->create_element("head");
    html_element->append_child(head_element);
    auto style_element = m_document->create_element("style");
    style_element->append_child(adopt_ref(*new Web::DOM::Text(document(), "div { font-family: Csilla; font-weight: lighter; }")));
    head_element->append_child(style_element);
    auto body_element = m_document->create_element("body");
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
    return String::formatted("{:02}:{:02}:{:02} ", tm->tm_hour, tm->tm_min, tm->tm_sec);
}

void IRCLogBuffer::add_message(char prefix, const String& name, const String& text, Color color)
{
    auto nick_string = String::formatted("<{}{}> ", prefix ? prefix : ' ', name.characters());
    auto html = String::formatted(
        "<span>{}</span>"
        "<b>{}</b>"
        "<span>{}</span>",
        timestamp_string(),
        escape_html_entities(nick_string),
        escape_html_entities(text));

    auto wrapper = m_document->create_element(Web::HTML::TagNames::div);
    wrapper->set_attribute(Web::HTML::AttributeNames::style, String::formatted("color: {}", color.to_string()));
    wrapper->set_inner_html(html);
    m_container_element->append_child(wrapper);
    m_document->force_layout();
}

void IRCLogBuffer::add_message(const String& text, Color color)
{
    auto html = String::formatted(
        "<span>{}</span>"
        "<span>{}</span>",
        timestamp_string(),
        escape_html_entities(text));
    auto wrapper = m_document->create_element(Web::HTML::TagNames::div);
    wrapper->set_attribute(Web::HTML::AttributeNames::style, String::formatted("color: {}", color.to_string()));
    wrapper->set_inner_html(html);
    m_container_element->append_child(wrapper);
    m_document->force_layout();
}
