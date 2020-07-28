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

#include <AK/ByteBuffer.h>
#include <AK/URL.h>
#include <LibCore/File.h>
#include <LibWeb/CSS/Parser/CSSParser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLLinkElement::HTMLLinkElement(DOM::Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLLinkElement::~HTMLLinkElement()
{
}

void HTMLLinkElement::inserted_into(Node& node)
{
    HTMLElement::inserted_into(node);

    if (m_relationship & Relationship::Stylesheet && !(m_relationship & Relationship::Alternate))
        load_stylesheet(document().complete_url(href()));
}

void HTMLLinkElement::resource_did_fail()
{
}

void HTMLLinkElement::resource_did_load()
{
    ASSERT(resource());
    if (!resource()->has_encoded_data())
        return;

    dbg() << "HTMLLinkElement: Resource did load, looks good! " << href();

    auto sheet = parse_css(CSS::ParsingContext(document()), resource()->encoded_data());
    if (!sheet) {
        dbg() << "HTMLLinkElement: Failed to parse stylesheet: " << href();
        return;
    }

    // Transfer the rules from the successfully parsed sheet into the sheet we've already inserted.
    m_style_sheet->rules() = sheet->rules();

    document().update_style();
}

void HTMLLinkElement::load_stylesheet(const URL& url)
{
    // First insert an empty style sheet in the document sheet list.
    // There's probably a nicer way to do this, but this ensures that sheets are in document order.
    m_style_sheet = CSS::StyleSheet::create({});
    document().style_sheets().add_sheet(*m_style_sheet);

    LoadRequest request;
    request.set_url(url);
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
}

void HTMLLinkElement::parse_attribute(const FlyString& name, const String& value)
{
    if (name == HTML::AttributeNames::rel) {
        m_relationship = 0;
        auto parts = value.split_view(' ');
        for (auto& part : parts) {
            if (part == "stylesheet")
                m_relationship |= Relationship::Stylesheet;
            else if (part == "alternate")
                m_relationship |= Relationship::Alternate;
        }
    }
}

}
