/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
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
#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLLinkElement::HTMLLinkElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_css_loader(*this)
{
    m_css_loader.on_load = [&] {
        document.update_style();
    };
}

HTMLLinkElement::~HTMLLinkElement()
{
}

void HTMLLinkElement::inserted_into(Node& node)
{
    HTMLElement::inserted_into(node);

    if (m_relationship & Relationship::Stylesheet && !(m_relationship & Relationship::Alternate)) {
        m_css_loader.load_from_url(document().complete_url(href()));
        if (auto sheet = m_css_loader.style_sheet())
            document().style_sheets().add_sheet(sheet.release_nonnull());
    }
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
