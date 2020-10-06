/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "IRCLogBuffer.h"
#include <AK/StringBuilder.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <time.h>

NonnullRefPtr<IRCLogBuffer> IRCLogBuffer::create()
{
    return adopt(*new IRCLogBuffer);
}

IRCLogBuffer::IRCLogBuffer()
{
    m_document = adopt(*new Web::DOM::Document);
    m_document->append_child(adopt(*new Web::DOM::DocumentType(document())));
    auto html_element = create_element(document(), "html");
    m_document->append_child(html_element);
    auto head_element = create_element(document(), "head");
    html_element->append_child(head_element);
    auto style_element = create_element(document(), "style");
    style_element->append_child(adopt(*new Web::DOM::Text(document(), "div { font-family: Csilla; font-weight: lighter; }")));
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

    auto wrapper = Web::DOM::create_element(*m_document, Web::HTML::TagNames::div);
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
    auto wrapper = Web::DOM::create_element(*m_document, Web::HTML::TagNames::div);
    wrapper->set_attribute(Web::HTML::AttributeNames::style, String::formatted("color: {}", color.to_string()));
    wrapper->set_inner_html(html);
    m_container_element->append_child(wrapper);
    m_document->force_layout();
}
