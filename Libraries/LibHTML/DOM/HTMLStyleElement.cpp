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

#include <AK/StringBuilder.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLStyleElement.h>
#include <LibHTML/DOM/Text.h>
#include <LibHTML/Parser/CSSParser.h>

HTMLStyleElement::HTMLStyleElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLStyleElement::~HTMLStyleElement()
{
}

void HTMLStyleElement::inserted_into(Node& new_parent)
{
    StringBuilder builder;
    for_each_child([&](auto& child) {
        if (is<Text>(child))
            builder.append(to<Text>(child).text_content());
    });
    m_stylesheet = parse_css(builder.to_string());
    if (m_stylesheet)
        document().add_sheet(*m_stylesheet);
    HTMLElement::inserted_into(new_parent);
}

void HTMLStyleElement::removed_from(Node& old_parent)
{
    if (m_stylesheet) {
        // FIXME: Remove the sheet from the document
    }
    return HTMLElement::removed_from(old_parent);
}
