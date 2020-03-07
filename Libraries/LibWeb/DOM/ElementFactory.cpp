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

#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLAnchorElement.h>
#include <LibWeb/DOM/HTMLBRElement.h>
#include <LibWeb/DOM/HTMLBlinkElement.h>
#include <LibWeb/DOM/HTMLBodyElement.h>
#include <LibWeb/DOM/HTMLFontElement.h>
#include <LibWeb/DOM/HTMLFormElement.h>
#include <LibWeb/DOM/HTMLHRElement.h>
#include <LibWeb/DOM/HTMLHeadElement.h>
#include <LibWeb/DOM/HTMLHeadingElement.h>
#include <LibWeb/DOM/HTMLHtmlElement.h>
#include <LibWeb/DOM/HTMLImageElement.h>
#include <LibWeb/DOM/HTMLInputElement.h>
#include <LibWeb/DOM/HTMLLinkElement.h>
#include <LibWeb/DOM/HTMLStyleElement.h>
#include <LibWeb/DOM/HTMLTitleElement.h>

namespace Web {

NonnullRefPtr<Element> create_element(Document& document, const String& tag_name)
{
    auto lowercase_tag_name = tag_name.to_lowercase();
    if (lowercase_tag_name == "a")
        return adopt(*new HTMLAnchorElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "html")
        return adopt(*new HTMLHtmlElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "head")
        return adopt(*new HTMLHeadElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "body")
        return adopt(*new HTMLBodyElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "font")
        return adopt(*new HTMLFontElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "hr")
        return adopt(*new HTMLHRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "style")
        return adopt(*new HTMLStyleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "title")
        return adopt(*new HTMLTitleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "link")
        return adopt(*new HTMLLinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "img")
        return adopt(*new HTMLImageElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "blink")
        return adopt(*new HTMLBlinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "form")
        return adopt(*new HTMLFormElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "input")
        return adopt(*new HTMLInputElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "br")
        return adopt(*new HTMLBRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "h1"
        || lowercase_tag_name == "h2"
        || lowercase_tag_name == "h3"
        || lowercase_tag_name == "h4"
        || lowercase_tag_name == "h5"
        || lowercase_tag_name == "h6") {
        return adopt(*new HTMLHeadingElement(document, lowercase_tag_name));
    }
    return adopt(*new Element(document, lowercase_tag_name));
}

}
