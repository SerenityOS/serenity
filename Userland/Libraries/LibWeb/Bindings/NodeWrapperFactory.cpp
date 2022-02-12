/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CharacterDataWrapper.h>
#include <LibWeb/Bindings/CommentWrapper.h>
#include <LibWeb/Bindings/DocumentFragmentWrapper.h>
#include <LibWeb/Bindings/DocumentTypeWrapper.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/HTMLAnchorElementWrapper.h>
#include <LibWeb/Bindings/HTMLAreaElementWrapper.h>
#include <LibWeb/Bindings/HTMLAudioElementWrapper.h>
#include <LibWeb/Bindings/HTMLBRElementWrapper.h>
#include <LibWeb/Bindings/HTMLBaseElementWrapper.h>
#include <LibWeb/Bindings/HTMLBodyElementWrapper.h>
#include <LibWeb/Bindings/HTMLButtonElementWrapper.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/Bindings/HTMLDListElementWrapper.h>
#include <LibWeb/Bindings/HTMLDataElementWrapper.h>
#include <LibWeb/Bindings/HTMLDataListElementWrapper.h>
#include <LibWeb/Bindings/HTMLDetailsElementWrapper.h>
#include <LibWeb/Bindings/HTMLDialogElementWrapper.h>
#include <LibWeb/Bindings/HTMLDirectoryElementWrapper.h>
#include <LibWeb/Bindings/HTMLDivElementWrapper.h>
#include <LibWeb/Bindings/HTMLElementWrapper.h>
#include <LibWeb/Bindings/HTMLEmbedElementWrapper.h>
#include <LibWeb/Bindings/HTMLFieldSetElementWrapper.h>
#include <LibWeb/Bindings/HTMLFontElementWrapper.h>
#include <LibWeb/Bindings/HTMLFormElementWrapper.h>
#include <LibWeb/Bindings/HTMLFrameElementWrapper.h>
#include <LibWeb/Bindings/HTMLFrameSetElementWrapper.h>
#include <LibWeb/Bindings/HTMLHRElementWrapper.h>
#include <LibWeb/Bindings/HTMLHeadElementWrapper.h>
#include <LibWeb/Bindings/HTMLHeadingElementWrapper.h>
#include <LibWeb/Bindings/HTMLHtmlElementWrapper.h>
#include <LibWeb/Bindings/HTMLIFrameElementWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/HTMLInputElementWrapper.h>
#include <LibWeb/Bindings/HTMLLIElementWrapper.h>
#include <LibWeb/Bindings/HTMLLabelElementWrapper.h>
#include <LibWeb/Bindings/HTMLLegendElementWrapper.h>
#include <LibWeb/Bindings/HTMLLinkElementWrapper.h>
#include <LibWeb/Bindings/HTMLMapElementWrapper.h>
#include <LibWeb/Bindings/HTMLMarqueeElementWrapper.h>
#include <LibWeb/Bindings/HTMLMenuElementWrapper.h>
#include <LibWeb/Bindings/HTMLMetaElementWrapper.h>
#include <LibWeb/Bindings/HTMLMeterElementWrapper.h>
#include <LibWeb/Bindings/HTMLModElementWrapper.h>
#include <LibWeb/Bindings/HTMLOListElementWrapper.h>
#include <LibWeb/Bindings/HTMLObjectElementWrapper.h>
#include <LibWeb/Bindings/HTMLOptGroupElementWrapper.h>
#include <LibWeb/Bindings/HTMLOptionElementWrapper.h>
#include <LibWeb/Bindings/HTMLOutputElementWrapper.h>
#include <LibWeb/Bindings/HTMLParagraphElementWrapper.h>
#include <LibWeb/Bindings/HTMLParamElementWrapper.h>
#include <LibWeb/Bindings/HTMLPictureElementWrapper.h>
#include <LibWeb/Bindings/HTMLPreElementWrapper.h>
#include <LibWeb/Bindings/HTMLProgressElementWrapper.h>
#include <LibWeb/Bindings/HTMLQuoteElementWrapper.h>
#include <LibWeb/Bindings/HTMLScriptElementWrapper.h>
#include <LibWeb/Bindings/HTMLSelectElementWrapper.h>
#include <LibWeb/Bindings/HTMLSlotElementWrapper.h>
#include <LibWeb/Bindings/HTMLSourceElementWrapper.h>
#include <LibWeb/Bindings/HTMLSpanElementWrapper.h>
#include <LibWeb/Bindings/HTMLStyleElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableCaptionElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableCellElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableColElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableRowElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableSectionElementWrapper.h>
#include <LibWeb/Bindings/HTMLTemplateElementWrapper.h>
#include <LibWeb/Bindings/HTMLTextAreaElementWrapper.h>
#include <LibWeb/Bindings/HTMLTimeElementWrapper.h>
#include <LibWeb/Bindings/HTMLTitleElementWrapper.h>
#include <LibWeb/Bindings/HTMLTrackElementWrapper.h>
#include <LibWeb/Bindings/HTMLUListElementWrapper.h>
#include <LibWeb/Bindings/HTMLUnknownElementWrapper.h>
#include <LibWeb/Bindings/HTMLVideoElementWrapper.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/SVGCircleElementWrapper.h>
#include <LibWeb/Bindings/SVGEllipseElementWrapper.h>
#include <LibWeb/Bindings/SVGLineElementWrapper.h>
#include <LibWeb/Bindings/SVGPathElementWrapper.h>
#include <LibWeb/Bindings/SVGPolygonElementWrapper.h>
#include <LibWeb/Bindings/SVGPolylineElementWrapper.h>
#include <LibWeb/Bindings/SVGRectElementWrapper.h>
#include <LibWeb/Bindings/SVGSVGElementWrapper.h>
#include <LibWeb/Bindings/TextWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/HTMLAudioElement.h>
#include <LibWeb/HTML/HTMLBRElement.h>
#include <LibWeb/HTML/HTMLBaseElement.h>
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
#include <LibWeb/SVG/SVGEllipseElement.h>
#include <LibWeb/SVG/SVGLineElement.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGPolygonElement.h>
#include <LibWeb/SVG/SVGPolylineElement.h>
#include <LibWeb/SVG/SVGRectElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Bindings {

NodeWrapper* wrap(JS::GlobalObject& global_object, DOM::Node& node)
{
    if (is<DOM::Document>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<DOM::Document>(node)));
    if (is<DOM::DocumentType>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<DOM::DocumentType>(node)));
    if (is<HTML::HTMLAnchorElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLAnchorElement>(node)));
    if (is<HTML::HTMLAreaElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLAreaElement>(node)));
    if (is<HTML::HTMLAudioElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLAudioElement>(node)));
    if (is<HTML::HTMLBaseElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLBaseElement>(node)));
    if (is<HTML::HTMLBodyElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLBodyElement>(node)));
    if (is<HTML::HTMLBRElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLBRElement>(node)));
    if (is<HTML::HTMLButtonElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLButtonElement>(node)));
    if (is<HTML::HTMLCanvasElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLCanvasElement>(node)));
    if (is<HTML::HTMLDataElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLDataElement>(node)));
    if (is<HTML::HTMLDataListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLDataListElement>(node)));
    if (is<HTML::HTMLDetailsElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLDetailsElement>(node)));
    if (is<HTML::HTMLDialogElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLDialogElement>(node)));
    if (is<HTML::HTMLDirectoryElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLDirectoryElement>(node)));
    if (is<HTML::HTMLDivElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLDivElement>(node)));
    if (is<HTML::HTMLDListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLDListElement>(node)));
    if (is<HTML::HTMLEmbedElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLEmbedElement>(node)));
    if (is<HTML::HTMLFieldSetElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLFieldSetElement>(node)));
    if (is<HTML::HTMLFontElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLFontElement>(node)));
    if (is<HTML::HTMLFormElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLFormElement>(node)));
    if (is<HTML::HTMLFrameElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLFrameElement>(node)));
    if (is<HTML::HTMLFrameSetElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLFrameSetElement>(node)));
    if (is<HTML::HTMLHeadElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLHeadElement>(node)));
    if (is<HTML::HTMLHeadingElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLHeadingElement>(node)));
    if (is<HTML::HTMLHRElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLHRElement>(node)));
    if (is<HTML::HTMLHtmlElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLHtmlElement>(node)));
    if (is<HTML::HTMLIFrameElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLIFrameElement>(node)));
    if (is<HTML::HTMLImageElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLImageElement>(node)));
    if (is<HTML::HTMLInputElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLInputElement>(node)));
    if (is<HTML::HTMLLabelElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLLabelElement>(node)));
    if (is<HTML::HTMLLegendElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLLegendElement>(node)));
    if (is<HTML::HTMLLIElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLLIElement>(node)));
    if (is<HTML::HTMLLinkElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLLinkElement>(node)));
    if (is<HTML::HTMLMapElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLMapElement>(node)));
    if (is<HTML::HTMLMarqueeElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLMarqueeElement>(node)));
    if (is<HTML::HTMLMenuElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLMenuElement>(node)));
    if (is<HTML::HTMLMetaElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLMetaElement>(node)));
    if (is<HTML::HTMLMeterElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLMeterElement>(node)));
    if (is<HTML::HTMLModElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLModElement>(node)));
    if (is<HTML::HTMLObjectElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLObjectElement>(node)));
    if (is<HTML::HTMLOListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLOListElement>(node)));
    if (is<HTML::HTMLOptGroupElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLOptGroupElement>(node)));
    if (is<HTML::HTMLOptionElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLOptionElement>(node)));
    if (is<HTML::HTMLOutputElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLOutputElement>(node)));
    if (is<HTML::HTMLParagraphElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLParagraphElement>(node)));
    if (is<HTML::HTMLParamElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLParamElement>(node)));
    if (is<HTML::HTMLPictureElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLPictureElement>(node)));
    if (is<HTML::HTMLPreElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLPreElement>(node)));
    if (is<HTML::HTMLProgressElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLProgressElement>(node)));
    if (is<HTML::HTMLQuoteElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLQuoteElement>(node)));
    if (is<HTML::HTMLScriptElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLScriptElement>(node)));
    if (is<HTML::HTMLSelectElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLSelectElement>(node)));
    if (is<HTML::HTMLSlotElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLSlotElement>(node)));
    if (is<HTML::HTMLSourceElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLSourceElement>(node)));
    if (is<HTML::HTMLSpanElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLSpanElement>(node)));
    if (is<HTML::HTMLStyleElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLStyleElement>(node)));
    if (is<HTML::HTMLTableCaptionElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTableCaptionElement>(node)));
    if (is<HTML::HTMLTableCellElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTableCellElement>(node)));
    if (is<HTML::HTMLTableColElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTableColElement>(node)));
    if (is<HTML::HTMLTableElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTableElement>(node)));
    if (is<HTML::HTMLTableRowElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTableRowElement>(node)));
    if (is<HTML::HTMLTableSectionElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTableSectionElement>(node)));
    if (is<HTML::HTMLTemplateElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTemplateElement>(node)));
    if (is<HTML::HTMLTextAreaElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTextAreaElement>(node)));
    if (is<HTML::HTMLTimeElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTimeElement>(node)));
    if (is<HTML::HTMLTitleElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTitleElement>(node)));
    if (is<HTML::HTMLTrackElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLTrackElement>(node)));
    if (is<HTML::HTMLUListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLUListElement>(node)));
    if (is<HTML::HTMLUnknownElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLUnknownElement>(node)));
    if (is<HTML::HTMLVideoElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLVideoElement>(node)));
    if (is<HTML::HTMLElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<HTML::HTMLElement>(node)));
    if (is<SVG::SVGSVGElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGSVGElement>(node)));
    if (is<SVG::SVGCircleElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGCircleElement>(node)));
    if (is<SVG::SVGEllipseElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGEllipseElement>(node)));
    if (is<SVG::SVGLineElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGLineElement>(node)));
    if (is<SVG::SVGPolygonElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGPolygonElement>(node)));
    if (is<SVG::SVGPolylineElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGPolylineElement>(node)));
    if (is<SVG::SVGPathElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGPathElement>(node)));
    if (is<SVG::SVGRectElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<SVG::SVGRectElement>(node)));
    if (is<DOM::Element>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<DOM::Element>(node)));
    if (is<DOM::DocumentFragment>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<DOM::DocumentFragment>(node)));
    if (is<DOM::Comment>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<DOM::Comment>(node)));
    if (is<DOM::Text>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<DOM::Text>(node)));
    if (is<DOM::CharacterData>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, verify_cast<DOM::CharacterData>(node)));
    return static_cast<NodeWrapper*>(wrap_impl(global_object, node));
}

}
