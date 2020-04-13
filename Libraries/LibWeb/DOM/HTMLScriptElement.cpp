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
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLScriptElement.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/ResourceLoader.h>

namespace Web {

HTMLScriptElement::HTMLScriptElement(Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLScriptElement::~HTMLScriptElement()
{
}

void HTMLScriptElement::children_changed()
{
    HTMLElement::children_changed();

    if (has_attribute("src"))
        return;

    StringBuilder builder;
    for_each_child([&](auto& child) {
        if (is<Text>(child))
            builder.append(to<Text>(child).text_content());
    });
    auto source = builder.to_string();
    if (source.is_empty())
        return;

    auto parser = JS::Parser(JS::Lexer(source));
    auto program = parser.parse_program();
    if (parser.has_errors())
        return;

    document().interpreter().run(*program);
}

void HTMLScriptElement::inserted_into(Node& new_parent)
{
    HTMLElement::inserted_into(new_parent);

    auto src = attribute("src");
    if (src.is_null())
        return;

    URL src_url = document().complete_url(src);
    if (src_url.protocol() == "file" && document().url().protocol() != src_url.protocol()) {
        dbg() << "HTMLScriptElement: Forbidden to load " << src_url.to_string() << " from " << document().url().to_string();
        return;
    }

    String source;
    ResourceLoader::the().load_sync(src_url, [&](auto& data) {
        if (data.is_null()) {
            dbg() << "HTMLScriptElement: Failed to load " << src;
            return;
        }
        source = String::copy(data);
    });
    if (source.is_empty()) {
        dbg() << "HTMLScriptElement: No source to parse :(";
        return;
    }

    auto parser = JS::Parser(JS::Lexer(source));
    auto program = parser.parse_program();
    if (parser.has_errors())
        return;

    document().interpreter().run(*program);
}

}
