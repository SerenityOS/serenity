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
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLBRElement.h>
#include <LibWeb/HTML/HTMLBlinkElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLFontElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLHRElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHeadingElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/HTML/HTMLObjectElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLStyleElement.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>
#include <LibWeb/HTML/HTMLTableElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>
#include <LibWeb/SVG/TagNames.h>

namespace Web {

NonnullRefPtr<Element> create_element(Document& document, const FlyString& tag_name)
{
    auto lowercase_tag_name = tag_name.to_lowercase();
    if (lowercase_tag_name == HTML::TagNames::a)
        return adopt(*new HTMLAnchorElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::html)
        return adopt(*new HTMLHtmlElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::head)
        return adopt(*new HTMLHeadElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::body)
        return adopt(*new HTMLBodyElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::font)
        return adopt(*new HTMLFontElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::hr)
        return adopt(*new HTMLHRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::style)
        return adopt(*new HTMLStyleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::title)
        return adopt(*new HTMLTitleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::link)
        return adopt(*new HTMLLinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::img)
        return adopt(*new HTMLImageElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::blink)
        return adopt(*new HTMLBlinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::form)
        return adopt(*new HTMLFormElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::input)
        return adopt(*new HTMLInputElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::br)
        return adopt(*new HTMLBRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::iframe)
        return adopt(*new HTMLIFrameElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::table)
        return adopt(*new HTMLTableElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::tr)
        return adopt(*new HTMLTableRowElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::td || lowercase_tag_name == HTML::TagNames::th)
        return adopt(*new HTMLTableCellElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6))
        return adopt(*new HTMLHeadingElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::script)
        return adopt(*new HTMLScriptElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::canvas)
        return adopt(*new HTMLCanvasElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::object)
        return adopt(*new HTMLObjectElement(document, lowercase_tag_name));
    if (lowercase_tag_name == SVG::TagNames::svg)
        return adopt(*new SVG::SVGSVGElement(document, lowercase_tag_name));
    if (lowercase_tag_name == SVG::TagNames::path)
        return adopt(*new SVG::SVGPathElement(document, lowercase_tag_name));
    return adopt(*new Element(document, lowercase_tag_name));
}

}
