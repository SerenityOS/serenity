/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/HTMLAudioElement.h>
#include <LibWeb/HTML/HTMLBRElement.h>
#include <LibWeb/HTML/HTMLBaseElement.h>
#include <LibWeb/HTML/HTMLBlinkElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLDListElement.h>
#include <LibWeb/HTML/HTMLDataElement.h>
#include <LibWeb/HTML/HTMLDataListElement.h>
#include <LibWeb/HTML/HTMLDetailsElement.h>
#include <LibWeb/HTML/HTMLDialogElement.h>
#include <LibWeb/HTML/HTMLDivElement.h>
#include <LibWeb/HTML/HTMLEmbedElement.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/HTML/HTMLFontElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLFrameElement.h>
#include <LibWeb/HTML/HTMLFrameSetElement.h>
#include <LibWeb/HTML/HTMLHRElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHeadingElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLLIElement.h>
#include <LibWeb/HTML/HTMLLabelElement.h>
#include <LibWeb/HTML/HTMLLegendElement.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/HTML/HTMLMapElement.h>
#include <LibWeb/HTML/HTMLMarqueeElement.h>
#include <LibWeb/HTML/HTMLMenuElement.h>
#include <LibWeb/HTML/HTMLMetaElement.h>
#include <LibWeb/HTML/HTMLMeterElement.h>
#include <LibWeb/HTML/HTMLModElement.h>
#include <LibWeb/HTML/HTMLOListElement.h>
#include <LibWeb/HTML/HTMLObjectElement.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLOutputElement.h>
#include <LibWeb/HTML/HTMLParagraphElement.h>
#include <LibWeb/HTML/HTMLParamElement.h>
#include <LibWeb/HTML/HTMLPictureElement.h>
#include <LibWeb/HTML/HTMLPreElement.h>
#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/HTML/HTMLQuoteElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/HTML/HTMLSlotElement.h>
#include <LibWeb/HTML/HTMLSourceElement.h>
#include <LibWeb/HTML/HTMLSpanElement.h>
#include <LibWeb/HTML/HTMLStyleElement.h>
#include <LibWeb/HTML/HTMLTableCaptionElement.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>
#include <LibWeb/HTML/HTMLTableColElement.h>
#include <LibWeb/HTML/HTMLTableElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTableSectionElement.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>
#include <LibWeb/HTML/HTMLTimeElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/HTML/HTMLTrackElement.h>
#include <LibWeb/HTML/HTMLUListElement.h>
#include <LibWeb/HTML/HTMLUnknownElement.h>
#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>
#include <LibWeb/SVG/TagNames.h>

namespace Web::DOM {

NonnullRefPtr<Element> create_element(Document& document, const FlyString& tag_name)
{
    auto lowercase_tag_name = tag_name.to_lowercase();
    if (lowercase_tag_name == HTML::TagNames::a)
        return adopt(*new HTML::HTMLAnchorElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::area)
        return adopt(*new HTML::HTMLAreaElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::audio)
        return adopt(*new HTML::HTMLAudioElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::base)
        return adopt(*new HTML::HTMLBaseElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::blink)
        return adopt(*new HTML::HTMLBlinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::body)
        return adopt(*new HTML::HTMLBodyElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::br)
        return adopt(*new HTML::HTMLBRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::button)
        return adopt(*new HTML::HTMLButtonElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::canvas)
        return adopt(*new HTML::HTMLCanvasElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::data)
        return adopt(*new HTML::HTMLDataElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::datalist)
        return adopt(*new HTML::HTMLDataListElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::details)
        return adopt(*new HTML::HTMLDetailsElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::dialog)
        return adopt(*new HTML::HTMLDialogElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::div)
        return adopt(*new HTML::HTMLDivElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::dl)
        return adopt(*new HTML::HTMLDListElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::embed)
        return adopt(*new HTML::HTMLEmbedElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::fieldset)
        return adopt(*new HTML::HTMLFieldSetElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::font)
        return adopt(*new HTML::HTMLFontElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::form)
        return adopt(*new HTML::HTMLFormElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::frame)
        return adopt(*new HTML::HTMLFrameElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::frameset)
        return adopt(*new HTML::HTMLFrameSetElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::head)
        return adopt(*new HTML::HTMLHeadElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6))
        return adopt(*new HTML::HTMLHeadingElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::hr)
        return adopt(*new HTML::HTMLHRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::html)
        return adopt(*new HTML::HTMLHtmlElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::iframe)
        return adopt(*new HTML::HTMLIFrameElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::img)
        return adopt(*new HTML::HTMLImageElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::input)
        return adopt(*new HTML::HTMLInputElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::label)
        return adopt(*new HTML::HTMLLabelElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::legend)
        return adopt(*new HTML::HTMLLegendElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::li)
        return adopt(*new HTML::HTMLLIElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::link)
        return adopt(*new HTML::HTMLLinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::map)
        return adopt(*new HTML::HTMLMapElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::marquee)
        return adopt(*new HTML::HTMLMarqueeElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::menu)
        return adopt(*new HTML::HTMLMenuElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::meta)
        return adopt(*new HTML::HTMLMetaElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::meter)
        return adopt(*new HTML::HTMLMeterElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::ins, HTML::TagNames::del))
        return adopt(*new HTML::HTMLModElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::object)
        return adopt(*new HTML::HTMLObjectElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::ol)
        return adopt(*new HTML::HTMLOListElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::optgroup)
        return adopt(*new HTML::HTMLOptGroupElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::option)
        return adopt(*new HTML::HTMLOptionElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::output)
        return adopt(*new HTML::HTMLOutputElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::p)
        return adopt(*new HTML::HTMLParagraphElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::param)
        return adopt(*new HTML::HTMLParamElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::picture)
        return adopt(*new HTML::HTMLPictureElement(document, lowercase_tag_name));
    // NOTE: The obsolete elements "listing" and "xmp" are explicitly mapped to HTMLPreElement in the specification.
    if (lowercase_tag_name.is_one_of(HTML::TagNames::pre, HTML::TagNames::listing, HTML::TagNames::xmp))
        return adopt(*new HTML::HTMLPreElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::progress)
        return adopt(*new HTML::HTMLProgressElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::blockquote, HTML::TagNames::q))
        return adopt(*new HTML::HTMLQuoteElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::script)
        return adopt(*new HTML::HTMLScriptElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::select)
        return adopt(*new HTML::HTMLSelectElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::slot)
        return adopt(*new HTML::HTMLSlotElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::source)
        return adopt(*new HTML::HTMLSourceElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::span)
        return adopt(*new HTML::HTMLSpanElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::style)
        return adopt(*new HTML::HTMLStyleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::caption)
        return adopt(*new HTML::HTMLTableCaptionElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(Web::HTML::TagNames::td, Web::HTML::TagNames::th))
        return adopt(*new HTML::HTMLTableCellElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::colgroup, HTML::TagNames::col))
        return adopt(*new HTML::HTMLTableColElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::table)
        return adopt(*new HTML::HTMLTableElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::tr)
        return adopt(*new HTML::HTMLTableRowElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::tbody, HTML::TagNames::thead, HTML::TagNames::tfoot))
        return adopt(*new HTML::HTMLTableSectionElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::template_)
        return adopt(*new HTML::HTMLTemplateElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::textarea)
        return adopt(*new HTML::HTMLTextAreaElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::time)
        return adopt(*new HTML::HTMLTimeElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::title)
        return adopt(*new HTML::HTMLTitleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::track)
        return adopt(*new HTML::HTMLTrackElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::ul)
        return adopt(*new HTML::HTMLUListElement(document, lowercase_tag_name));
    if (lowercase_tag_name == HTML::TagNames::video)
        return adopt(*new HTML::HTMLVideoElement(document, lowercase_tag_name));
    if (lowercase_tag_name.is_one_of(
            HTML::TagNames::article, HTML::TagNames::section, HTML::TagNames::nav, HTML::TagNames::aside, HTML::TagNames::hgroup, HTML::TagNames::header, HTML::TagNames::footer, HTML::TagNames::address, HTML::TagNames::dt, HTML::TagNames::dd, HTML::TagNames::figure, HTML::TagNames::figcaption, HTML::TagNames::main, HTML::TagNames::em, HTML::TagNames::strong, HTML::TagNames::small, HTML::TagNames::s, HTML::TagNames::cite, HTML::TagNames::dfn, HTML::TagNames::abbr, HTML::TagNames::ruby, HTML::TagNames::rt, HTML::TagNames::rp, HTML::TagNames::code, HTML::TagNames::var, HTML::TagNames::samp, HTML::TagNames::kbd, HTML::TagNames::sub, HTML::TagNames::sup, HTML::TagNames::i, HTML::TagNames::b, HTML::TagNames::u, HTML::TagNames::mark, HTML::TagNames::bdi, HTML::TagNames::bdo, HTML::TagNames::wbr, HTML::TagNames::summary, HTML::TagNames::noscript,
            // Obsolete
            HTML::TagNames::acronym, HTML::TagNames::basefont, HTML::TagNames::big, HTML::TagNames::center, HTML::TagNames::nobr, HTML::TagNames::noembed, HTML::TagNames::noframes, HTML::TagNames::plaintext, HTML::TagNames::rb, HTML::TagNames::rtc, HTML::TagNames::strike, HTML::TagNames::tt))
        return adopt(*new HTML::HTMLElement(document, lowercase_tag_name));
    if (lowercase_tag_name == SVG::TagNames::svg)
        return adopt(*new SVG::SVGSVGElement(document, lowercase_tag_name));
    if (lowercase_tag_name == SVG::TagNames::path)
        return adopt(*new SVG::SVGPathElement(document, lowercase_tag_name));

    // FIXME: If name is a valid custom element name, then return HTMLElement.

    return adopt(*new HTML::HTMLUnknownElement(document, lowercase_tag_name));
}

}
