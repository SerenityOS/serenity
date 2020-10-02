/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
#include <LibWeb/Bindings/HTMLDivElementWrapper.h>
#include <LibWeb/Bindings/HTMLElementWrapper.h>
#include <LibWeb/Bindings/HTMLEmbedElementWrapper.h>
#include <LibWeb/Bindings/HTMLFieldSetElementWrapper.h>
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
#include <LibWeb/Bindings/SVGPathElementWrapper.h>
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
#include <LibWeb/HTML/HTMLDivElement.h>
#include <LibWeb/HTML/HTMLEmbedElement.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
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

namespace Web::Bindings {

NodeWrapper* wrap(JS::GlobalObject& global_object, DOM::Node& node)
{
    if (is<DOM::Document>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::Document>(node)));
    if (is<DOM::DocumentType>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::DocumentType>(node)));
    if (is<HTML::HTMLAnchorElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLAnchorElement>(node)));
    if (is<HTML::HTMLAreaElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLAreaElement>(node)));
    if (is<HTML::HTMLAudioElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLAudioElement>(node)));
    if (is<HTML::HTMLBaseElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLBaseElement>(node)));
    if (is<HTML::HTMLBodyElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLBodyElement>(node)));
    if (is<HTML::HTMLBRElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLBRElement>(node)));
    if (is<HTML::HTMLButtonElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLButtonElement>(node)));
    if (is<HTML::HTMLCanvasElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLCanvasElement>(node)));
    if (is<HTML::HTMLDataElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLDataElement>(node)));
    if (is<HTML::HTMLDataListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLDataListElement>(node)));
    if (is<HTML::HTMLDetailsElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLDetailsElement>(node)));
    if (is<HTML::HTMLDialogElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLDialogElement>(node)));
    if (is<HTML::HTMLDivElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLDivElement>(node)));
    if (is<HTML::HTMLDListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLDListElement>(node)));
    if (is<HTML::HTMLEmbedElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLEmbedElement>(node)));
    if (is<HTML::HTMLFieldSetElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLFieldSetElement>(node)));
    if (is<HTML::HTMLFormElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLFormElement>(node)));
    if (is<HTML::HTMLFrameElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLFrameElement>(node)));
    if (is<HTML::HTMLFrameSetElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLFrameSetElement>(node)));
    if (is<HTML::HTMLHeadElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLHeadElement>(node)));
    if (is<HTML::HTMLHeadingElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLHeadingElement>(node)));
    if (is<HTML::HTMLHRElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLHRElement>(node)));
    if (is<HTML::HTMLHtmlElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLHtmlElement>(node)));
    if (is<HTML::HTMLIFrameElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLIFrameElement>(node)));
    if (is<HTML::HTMLImageElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLImageElement>(node)));
    if (is<HTML::HTMLInputElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLInputElement>(node)));
    if (is<HTML::HTMLLabelElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLLabelElement>(node)));
    if (is<HTML::HTMLLegendElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLLegendElement>(node)));
    if (is<HTML::HTMLLIElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLLIElement>(node)));
    if (is<HTML::HTMLLinkElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLLinkElement>(node)));
    if (is<HTML::HTMLMapElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLMapElement>(node)));
    if (is<HTML::HTMLMarqueeElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLMarqueeElement>(node)));
    if (is<HTML::HTMLMenuElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLMenuElement>(node)));
    if (is<HTML::HTMLMetaElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLMetaElement>(node)));
    if (is<HTML::HTMLMeterElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLMeterElement>(node)));
    if (is<HTML::HTMLModElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLModElement>(node)));
    if (is<HTML::HTMLObjectElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLObjectElement>(node)));
    if (is<HTML::HTMLOListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLOListElement>(node)));
    if (is<HTML::HTMLOptGroupElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLOptGroupElement>(node)));
    if (is<HTML::HTMLOptionElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLOptionElement>(node)));
    if (is<HTML::HTMLOutputElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLOutputElement>(node)));
    if (is<HTML::HTMLParagraphElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLParagraphElement>(node)));
    if (is<HTML::HTMLParamElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLParamElement>(node)));
    if (is<HTML::HTMLPictureElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLPictureElement>(node)));
    if (is<HTML::HTMLPreElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLPreElement>(node)));
    if (is<HTML::HTMLProgressElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLProgressElement>(node)));
    if (is<HTML::HTMLQuoteElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLQuoteElement>(node)));
    if (is<HTML::HTMLScriptElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLScriptElement>(node)));
    if (is<HTML::HTMLSelectElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLSelectElement>(node)));
    if (is<HTML::HTMLSlotElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLSlotElement>(node)));
    if (is<HTML::HTMLSourceElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLSourceElement>(node)));
    if (is<HTML::HTMLSpanElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLSpanElement>(node)));
    if (is<HTML::HTMLStyleElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLStyleElement>(node)));
    if (is<HTML::HTMLTableCaptionElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTableCaptionElement>(node)));
    if (is<HTML::HTMLTableCellElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTableCellElement>(node)));
    if (is<HTML::HTMLTableColElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTableColElement>(node)));
    if (is<HTML::HTMLTableElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTableElement>(node)));
    if (is<HTML::HTMLTableRowElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTableRowElement>(node)));
    if (is<HTML::HTMLTableSectionElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTableSectionElement>(node)));
    if (is<HTML::HTMLTemplateElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTemplateElement>(node)));
    if (is<HTML::HTMLTextAreaElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTextAreaElement>(node)));
    if (is<HTML::HTMLTimeElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTimeElement>(node)));
    if (is<HTML::HTMLTitleElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTitleElement>(node)));
    if (is<HTML::HTMLTrackElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLTrackElement>(node)));
    if (is<HTML::HTMLUListElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLUListElement>(node)));
    if (is<HTML::HTMLUnknownElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLUnknownElement>(node)));
    if (is<HTML::HTMLVideoElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLVideoElement>(node)));
    if (is<HTML::HTMLElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTML::HTMLElement>(node)));
    if (is<SVG::SVGSVGElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<SVG::SVGSVGElement>(node)));
    if (is<SVG::SVGPathElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<SVG::SVGPathElement>(node)));
    if (is<DOM::Element>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::Element>(node)));
    if (is<DOM::DocumentFragment>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::DocumentFragment>(node)));
    if (is<DOM::Comment>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::Comment>(node)));
    if (is<DOM::Text>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::Text>(node)));
    if (is<DOM::CharacterData>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::CharacterData>(node)));
    return static_cast<NodeWrapper*>(wrap_impl(global_object, node));
}

}
