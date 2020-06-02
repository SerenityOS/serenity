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
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLLinkElement.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Parser/CSSParser.h>

namespace Web {

HTMLLinkElement::HTMLLinkElement(Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLLinkElement::~HTMLLinkElement()
{
}

void HTMLLinkElement::inserted_into(Node& node)
{
    HTMLElement::inserted_into(node);

    if (rel() == "stylesheet")
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

    auto sheet = parse_css(resource()->encoded_data());
    if (!sheet) {
        dbg() << "HTMLLinkElement: Failed to parse stylesheet: " << href();
        return;
    }
    document().add_sheet(*sheet);
    document().update_style();
}

void HTMLLinkElement::load_stylesheet(const URL& url)
{
    LoadRequest request;
    request.set_url(url);
    set_resource(ResourceLoader::the().load_resource(request));
}

}
