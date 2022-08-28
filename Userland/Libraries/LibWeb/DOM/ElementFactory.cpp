/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
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
#include <LibWeb/SVG/SVGCircleElement.h>
#include <LibWeb/SVG/SVGClipPathElement.h>
#include <LibWeb/SVG/SVGDefsElement.h>
#include <LibWeb/SVG/SVGEllipseElement.h>
#include <LibWeb/SVG/SVGGElement.h>
#include <LibWeb/SVG/SVGLineElement.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGPolygonElement.h>
#include <LibWeb/SVG/SVGPolylineElement.h>
#include <LibWeb/SVG/SVGRectElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>
#include <LibWeb/SVG/SVGTextContentElement.h>
#include <LibWeb/SVG/TagNames.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#concept-create-element
JS::NonnullGCPtr<Element> create_element(Document& document, FlyString local_name, FlyString namespace_, FlyString prefix)
{
    // 1. If prefix was not given, let prefix be null.
    // NOTE: This is already taken care of by `prefix` having a default value.

    // FIXME: 2. If is was not given, let is be null.
    // FIXME: 3. Let result be null.
    // FIXME: 4. Let definition be the result of looking up a custom element definition given document, namespace, localName, and is.
    // FIXME: 5. If definition is non-null, and definition’s name is not equal to its local name (i.e., definition represents a customized built-in element), then: ...
    // FIXME: 6. Otherwise, if definition is non-null, then: ...

    // 7. Otherwise:
    //    1. Let interface be the element interface for localName and namespace.
    //    2. Set result to a new element that implements interface, with no attributes, namespace set to namespace, namespace prefix set to prefix,
    //       local name set to localName, custom element state set to "uncustomized", custom element definition set to null, is value set to is,
    //       and node document set to document.
    //    FIXME: 3. If namespace is the HTML namespace, and either localName is a valid custom element name or is is non-null,
    //           then set result’s custom element state to "undefined".
    // 8. Return result.

    auto& realm = document.realm();
    auto lowercase_tag_name = local_name.to_lowercase();

    auto qualified_name = QualifiedName { local_name, prefix, namespace_ };
    if (lowercase_tag_name == HTML::TagNames::a)
        return *realm.heap().allocate<HTML::HTMLAnchorElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::area)
        return *realm.heap().allocate<HTML::HTMLAreaElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::audio)
        return *realm.heap().allocate<HTML::HTMLAudioElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::base)
        return *realm.heap().allocate<HTML::HTMLBaseElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::blink)
        return *realm.heap().allocate<HTML::HTMLBlinkElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::body)
        return *realm.heap().allocate<HTML::HTMLBodyElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::br)
        return *realm.heap().allocate<HTML::HTMLBRElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::button)
        return *realm.heap().allocate<HTML::HTMLButtonElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::canvas)
        return *realm.heap().allocate<HTML::HTMLCanvasElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::data)
        return *realm.heap().allocate<HTML::HTMLDataElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::datalist)
        return *realm.heap().allocate<HTML::HTMLDataListElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::details)
        return *realm.heap().allocate<HTML::HTMLDetailsElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::dialog)
        return *realm.heap().allocate<HTML::HTMLDialogElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::dir)
        return *realm.heap().allocate<HTML::HTMLDirectoryElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::div)
        return *realm.heap().allocate<HTML::HTMLDivElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::dl)
        return *realm.heap().allocate<HTML::HTMLDListElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::embed)
        return *realm.heap().allocate<HTML::HTMLEmbedElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::fieldset)
        return *realm.heap().allocate<HTML::HTMLFieldSetElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::font)
        return *realm.heap().allocate<HTML::HTMLFontElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::form)
        return *realm.heap().allocate<HTML::HTMLFormElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::frame)
        return *realm.heap().allocate<HTML::HTMLFrameElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::frameset)
        return *realm.heap().allocate<HTML::HTMLFrameSetElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::head)
        return *realm.heap().allocate<HTML::HTMLHeadElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6))
        return *realm.heap().allocate<HTML::HTMLHeadingElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::hr)
        return *realm.heap().allocate<HTML::HTMLHRElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::html)
        return *realm.heap().allocate<HTML::HTMLHtmlElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::iframe)
        return *realm.heap().allocate<HTML::HTMLIFrameElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::img)
        return *realm.heap().allocate<HTML::HTMLImageElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::input)
        return *realm.heap().allocate<HTML::HTMLInputElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::label)
        return *realm.heap().allocate<HTML::HTMLLabelElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::legend)
        return *realm.heap().allocate<HTML::HTMLLegendElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::li)
        return *realm.heap().allocate<HTML::HTMLLIElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::link)
        return *realm.heap().allocate<HTML::HTMLLinkElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::map)
        return *realm.heap().allocate<HTML::HTMLMapElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::marquee)
        return *realm.heap().allocate<HTML::HTMLMarqueeElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::menu)
        return *realm.heap().allocate<HTML::HTMLMenuElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::meta)
        return *realm.heap().allocate<HTML::HTMLMetaElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::meter)
        return *realm.heap().allocate<HTML::HTMLMeterElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::ins, HTML::TagNames::del))
        return *realm.heap().allocate<HTML::HTMLModElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::object)
        return *realm.heap().allocate<HTML::HTMLObjectElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::ol)
        return *realm.heap().allocate<HTML::HTMLOListElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::optgroup)
        return *realm.heap().allocate<HTML::HTMLOptGroupElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::option)
        return *realm.heap().allocate<HTML::HTMLOptionElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::output)
        return *realm.heap().allocate<HTML::HTMLOutputElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::p)
        return *realm.heap().allocate<HTML::HTMLParagraphElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::param)
        return *realm.heap().allocate<HTML::HTMLParamElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::picture)
        return *realm.heap().allocate<HTML::HTMLPictureElement>(realm, document, move(qualified_name));
    // NOTE: The obsolete elements "listing" and "xmp" are explicitly mapped to HTMLPreElement in the specification.
    if (lowercase_tag_name.is_one_of(HTML::TagNames::pre, HTML::TagNames::listing, HTML::TagNames::xmp))
        return *realm.heap().allocate<HTML::HTMLPreElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::progress)
        return *realm.heap().allocate<HTML::HTMLProgressElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::blockquote, HTML::TagNames::q))
        return *realm.heap().allocate<HTML::HTMLQuoteElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::script)
        return *realm.heap().allocate<HTML::HTMLScriptElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::select)
        return *realm.heap().allocate<HTML::HTMLSelectElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::slot)
        return *realm.heap().allocate<HTML::HTMLSlotElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::source)
        return *realm.heap().allocate<HTML::HTMLSourceElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::span)
        return *realm.heap().allocate<HTML::HTMLSpanElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::style)
        return *realm.heap().allocate<HTML::HTMLStyleElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::caption)
        return *realm.heap().allocate<HTML::HTMLTableCaptionElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.is_one_of(Web::HTML::TagNames::td, Web::HTML::TagNames::th))
        return *realm.heap().allocate<HTML::HTMLTableCellElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::colgroup, HTML::TagNames::col))
        return *realm.heap().allocate<HTML::HTMLTableColElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::table)
        return *realm.heap().allocate<HTML::HTMLTableElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::tr)
        return *realm.heap().allocate<HTML::HTMLTableRowElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.is_one_of(HTML::TagNames::tbody, HTML::TagNames::thead, HTML::TagNames::tfoot))
        return *realm.heap().allocate<HTML::HTMLTableSectionElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::template_)
        return *realm.heap().allocate<HTML::HTMLTemplateElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::textarea)
        return *realm.heap().allocate<HTML::HTMLTextAreaElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::time)
        return *realm.heap().allocate<HTML::HTMLTimeElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::title)
        return *realm.heap().allocate<HTML::HTMLTitleElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::track)
        return *realm.heap().allocate<HTML::HTMLTrackElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::ul)
        return *realm.heap().allocate<HTML::HTMLUListElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == HTML::TagNames::video)
        return *realm.heap().allocate<HTML::HTMLVideoElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.is_one_of(
            HTML::TagNames::article, HTML::TagNames::section, HTML::TagNames::nav, HTML::TagNames::aside, HTML::TagNames::hgroup, HTML::TagNames::header, HTML::TagNames::footer, HTML::TagNames::address, HTML::TagNames::dt, HTML::TagNames::dd, HTML::TagNames::figure, HTML::TagNames::figcaption, HTML::TagNames::main, HTML::TagNames::em, HTML::TagNames::strong, HTML::TagNames::small, HTML::TagNames::s, HTML::TagNames::cite, HTML::TagNames::dfn, HTML::TagNames::abbr, HTML::TagNames::ruby, HTML::TagNames::rt, HTML::TagNames::rp, HTML::TagNames::code, HTML::TagNames::var, HTML::TagNames::samp, HTML::TagNames::kbd, HTML::TagNames::sub, HTML::TagNames::sup, HTML::TagNames::i, HTML::TagNames::b, HTML::TagNames::u, HTML::TagNames::mark, HTML::TagNames::bdi, HTML::TagNames::bdo, HTML::TagNames::wbr, HTML::TagNames::summary, HTML::TagNames::noscript,
            // Obsolete
            HTML::TagNames::acronym, HTML::TagNames::basefont, HTML::TagNames::big, HTML::TagNames::center, HTML::TagNames::nobr, HTML::TagNames::noembed, HTML::TagNames::noframes, HTML::TagNames::plaintext, HTML::TagNames::rb, HTML::TagNames::rtc, HTML::TagNames::strike, HTML::TagNames::tt))
        return *realm.heap().allocate<HTML::HTMLElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::svg)
        return *realm.heap().allocate<SVG::SVGSVGElement>(realm, document, move(qualified_name));
    // FIXME: Support SVG's mixedCase tag names properly.
    if (lowercase_tag_name.equals_ignoring_case(SVG::TagNames::clipPath))
        return *realm.heap().allocate<SVG::SVGClipPathElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::circle)
        return *realm.heap().allocate<SVG::SVGCircleElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name.equals_ignoring_case(SVG::TagNames::defs))
        return *realm.heap().allocate<SVG::SVGDefsElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::ellipse)
        return *realm.heap().allocate<SVG::SVGEllipseElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::line)
        return *realm.heap().allocate<SVG::SVGLineElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::path)
        return *realm.heap().allocate<SVG::SVGPathElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::polygon)
        return *realm.heap().allocate<SVG::SVGPolygonElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::polyline)
        return *realm.heap().allocate<SVG::SVGPolylineElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::rect)
        return *realm.heap().allocate<SVG::SVGRectElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::g)
        return *realm.heap().allocate<SVG::SVGGElement>(realm, document, move(qualified_name));
    if (lowercase_tag_name == SVG::TagNames::text)
        return *realm.heap().allocate<SVG::SVGTextContentElement>(realm, document, move(qualified_name));

    // FIXME: If name is a valid custom element name, then return HTMLElement.

    return *realm.heap().allocate<HTML::HTMLUnknownElement>(realm, document, move(qualified_name));
}

}
