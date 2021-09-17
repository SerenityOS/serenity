/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibWeb/HTML/HTMLDirectoryElement.h>
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
#include <LibWeb/SVG/SVGGElement.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>
#include <LibWeb/SVG/TagNames.h>

namespace Web::DOM {

NonnullRefPtr<Element> create_element(Document& document, const FlyString& tag_name, const FlyString& namespace_)
{
    auto lowercase_tag_name = tag_name.to_lowercase();
    // FIXME: Add prefix when we support it.
    auto qualified_name = QualifiedName(tag_name, {}, namespace_);
    if (lowercase_tag_name == HTML::TagNames::a)
        return adopt_ref(*new HTML::HTMLAnchorElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::area)
        return adopt_ref(*new HTML::HTMLAreaElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::audio)
        return adopt_ref(*new HTML::HTMLAudioElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::base)
        return adopt_ref(*new HTML::HTMLBaseElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::blink)
        return adopt_ref(*new HTML::HTMLBlinkElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::body)
        return adopt_ref(*new HTML::HTMLBodyElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::br)
        return adopt_ref(*new HTML::HTMLBRElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::button)
        return adopt_ref(*new HTML::HTMLButtonElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::canvas)
        return adopt_ref(*new HTML::HTMLCanvasElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::data)
        return adopt_ref(*new HTML::HTMLDataElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::datalist)
        return adopt_ref(*new HTML::HTMLDataListElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::details)
        return adopt_ref(*new HTML::HTMLDetailsElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::dialog)
        return adopt_ref(*new HTML::HTMLDialogElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::dir)
        return adopt_ref(*new HTML::HTMLDirectoryElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::div)
        return adopt_ref(*new HTML::HTMLDivElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::dl)
        return adopt_ref(*new HTML::HTMLDListElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::embed)
        return adopt_ref(*new HTML::HTMLEmbedElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::fieldset)
        return adopt_ref(*new HTML::HTMLFieldSetElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::font)
        return adopt_ref(*new HTML::HTMLFontElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::form)
        return adopt_ref(*new HTML::HTMLFormElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::frame)
        return adopt_ref(*new HTML::HTMLFrameElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::frameset)
        return adopt_ref(*new HTML::HTMLFrameSetElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::head)
        return adopt_ref(*new HTML::HTMLHeadElement(document, move(qualified_name)));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6))
        return adopt_ref(*new HTML::HTMLHeadingElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::hr)
        return adopt_ref(*new HTML::HTMLHRElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::html)
        return adopt_ref(*new HTML::HTMLHtmlElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::iframe)
        return adopt_ref(*new HTML::HTMLIFrameElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::img)
        return adopt_ref(*new HTML::HTMLImageElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::input)
        return adopt_ref(*new HTML::HTMLInputElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::label)
        return adopt_ref(*new HTML::HTMLLabelElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::legend)
        return adopt_ref(*new HTML::HTMLLegendElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::li)
        return adopt_ref(*new HTML::HTMLLIElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::link)
        return adopt_ref(*new HTML::HTMLLinkElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::map)
        return adopt_ref(*new HTML::HTMLMapElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::marquee)
        return adopt_ref(*new HTML::HTMLMarqueeElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::menu)
        return adopt_ref(*new HTML::HTMLMenuElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::meta)
        return adopt_ref(*new HTML::HTMLMetaElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::meter)
        return adopt_ref(*new HTML::HTMLMeterElement(document, move(qualified_name)));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::ins, HTML::TagNames::del))
        return adopt_ref(*new HTML::HTMLModElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::object)
        return adopt_ref(*new HTML::HTMLObjectElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::ol)
        return adopt_ref(*new HTML::HTMLOListElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::optgroup)
        return adopt_ref(*new HTML::HTMLOptGroupElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::option)
        return adopt_ref(*new HTML::HTMLOptionElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::output)
        return adopt_ref(*new HTML::HTMLOutputElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::p)
        return adopt_ref(*new HTML::HTMLParagraphElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::param)
        return adopt_ref(*new HTML::HTMLParamElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::picture)
        return adopt_ref(*new HTML::HTMLPictureElement(document, move(qualified_name)));
    // NOTE: The obsolete elements "listing" and "xmp" are explicitly mapped to HTMLPreElement in the specification.
    if (lowercase_tag_name.is_one_of(HTML::TagNames::pre, HTML::TagNames::listing, HTML::TagNames::xmp))
        return adopt_ref(*new HTML::HTMLPreElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::progress)
        return adopt_ref(*new HTML::HTMLProgressElement(document, move(qualified_name)));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::blockquote, HTML::TagNames::q))
        return adopt_ref(*new HTML::HTMLQuoteElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::script)
        return adopt_ref(*new HTML::HTMLScriptElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::select)
        return adopt_ref(*new HTML::HTMLSelectElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::slot)
        return adopt_ref(*new HTML::HTMLSlotElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::source)
        return adopt_ref(*new HTML::HTMLSourceElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::span)
        return adopt_ref(*new HTML::HTMLSpanElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::style)
        return adopt_ref(*new HTML::HTMLStyleElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::caption)
        return adopt_ref(*new HTML::HTMLTableCaptionElement(document, move(qualified_name)));
    if (lowercase_tag_name.is_one_of(Web::HTML::TagNames::td, Web::HTML::TagNames::th))
        return adopt_ref(*new HTML::HTMLTableCellElement(document, move(qualified_name)));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::colgroup, HTML::TagNames::col))
        return adopt_ref(*new HTML::HTMLTableColElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::table)
        return adopt_ref(*new HTML::HTMLTableElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::tr)
        return adopt_ref(*new HTML::HTMLTableRowElement(document, move(qualified_name)));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::tbody, HTML::TagNames::thead, HTML::TagNames::tfoot))
        return adopt_ref(*new HTML::HTMLTableSectionElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::template_)
        return adopt_ref(*new HTML::HTMLTemplateElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::textarea)
        return adopt_ref(*new HTML::HTMLTextAreaElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::time)
        return adopt_ref(*new HTML::HTMLTimeElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::title)
        return adopt_ref(*new HTML::HTMLTitleElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::track)
        return adopt_ref(*new HTML::HTMLTrackElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::ul)
        return adopt_ref(*new HTML::HTMLUListElement(document, move(qualified_name)));
    if (lowercase_tag_name == HTML::TagNames::video)
        return adopt_ref(*new HTML::HTMLVideoElement(document, move(qualified_name)));
    if (lowercase_tag_name.is_one_of(
            HTML::TagNames::article, HTML::TagNames::section, HTML::TagNames::nav, HTML::TagNames::aside, HTML::TagNames::hgroup, HTML::TagNames::header, HTML::TagNames::footer, HTML::TagNames::address, HTML::TagNames::dt, HTML::TagNames::dd, HTML::TagNames::figure, HTML::TagNames::figcaption, HTML::TagNames::main, HTML::TagNames::em, HTML::TagNames::strong, HTML::TagNames::small, HTML::TagNames::s, HTML::TagNames::cite, HTML::TagNames::dfn, HTML::TagNames::abbr, HTML::TagNames::ruby, HTML::TagNames::rt, HTML::TagNames::rp, HTML::TagNames::code, HTML::TagNames::var, HTML::TagNames::samp, HTML::TagNames::kbd, HTML::TagNames::sub, HTML::TagNames::sup, HTML::TagNames::i, HTML::TagNames::b, HTML::TagNames::u, HTML::TagNames::mark, HTML::TagNames::bdi, HTML::TagNames::bdo, HTML::TagNames::wbr, HTML::TagNames::summary, HTML::TagNames::noscript,
            // Obsolete
            HTML::TagNames::acronym, HTML::TagNames::basefont, HTML::TagNames::big, HTML::TagNames::center, HTML::TagNames::nobr, HTML::TagNames::noembed, HTML::TagNames::noframes, HTML::TagNames::plaintext, HTML::TagNames::rb, HTML::TagNames::rtc, HTML::TagNames::strike, HTML::TagNames::tt))
        return adopt_ref(*new HTML::HTMLElement(document, move(qualified_name)));
    if (lowercase_tag_name == SVG::TagNames::svg)
        return adopt_ref(*new SVG::SVGSVGElement(document, move(qualified_name)));
    if (lowercase_tag_name == SVG::TagNames::path)
        return adopt_ref(*new SVG::SVGPathElement(document, move(qualified_name)));
    if (lowercase_tag_name == SVG::TagNames::g)
        return adopt_ref(*new SVG::SVGGElement(document, move(qualified_name)));

    // FIXME: If name is a valid custom element name, then return HTMLElement.

    return adopt_ref(*new HTML::HTMLUnknownElement(document, move(qualified_name)));
}

}
