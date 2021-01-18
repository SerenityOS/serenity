/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#pragma once

// FIXME: Find a way to generate all of this

#include <LibWeb/Bindings/CanvasRenderingContext2DConstructor.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DPrototype.h>
#include <LibWeb/Bindings/CharacterDataConstructor.h>
#include <LibWeb/Bindings/CharacterDataPrototype.h>
#include <LibWeb/Bindings/CommentConstructor.h>
#include <LibWeb/Bindings/CommentPrototype.h>
#include <LibWeb/Bindings/DOMImplementationConstructor.h>
#include <LibWeb/Bindings/DOMImplementationPrototype.h>
#include <LibWeb/Bindings/DocumentConstructor.h>
#include <LibWeb/Bindings/DocumentFragmentConstructor.h>
#include <LibWeb/Bindings/DocumentFragmentPrototype.h>
#include <LibWeb/Bindings/DocumentPrototype.h>
#include <LibWeb/Bindings/DocumentTypeConstructor.h>
#include <LibWeb/Bindings/DocumentTypePrototype.h>
#include <LibWeb/Bindings/ElementConstructor.h>
#include <LibWeb/Bindings/ElementPrototype.h>
#include <LibWeb/Bindings/EventConstructor.h>
#include <LibWeb/Bindings/EventPrototype.h>
#include <LibWeb/Bindings/EventTargetConstructor.h>
#include <LibWeb/Bindings/EventTargetPrototype.h>
#include <LibWeb/Bindings/HTMLAnchorElementConstructor.h>
#include <LibWeb/Bindings/HTMLAnchorElementPrototype.h>
#include <LibWeb/Bindings/HTMLAreaElementConstructor.h>
#include <LibWeb/Bindings/HTMLAreaElementPrototype.h>
#include <LibWeb/Bindings/HTMLAudioElementConstructor.h>
#include <LibWeb/Bindings/HTMLAudioElementPrototype.h>
#include <LibWeb/Bindings/HTMLBRElementConstructor.h>
#include <LibWeb/Bindings/HTMLBRElementPrototype.h>
#include <LibWeb/Bindings/HTMLBaseElementConstructor.h>
#include <LibWeb/Bindings/HTMLBaseElementPrototype.h>
#include <LibWeb/Bindings/HTMLBodyElementConstructor.h>
#include <LibWeb/Bindings/HTMLBodyElementPrototype.h>
#include <LibWeb/Bindings/HTMLButtonElementConstructor.h>
#include <LibWeb/Bindings/HTMLButtonElementPrototype.h>
#include <LibWeb/Bindings/HTMLCanvasElementConstructor.h>
#include <LibWeb/Bindings/HTMLCanvasElementPrototype.h>
#include <LibWeb/Bindings/HTMLDListElementConstructor.h>
#include <LibWeb/Bindings/HTMLDListElementPrototype.h>
#include <LibWeb/Bindings/HTMLDataElementConstructor.h>
#include <LibWeb/Bindings/HTMLDataElementPrototype.h>
#include <LibWeb/Bindings/HTMLDataListElementConstructor.h>
#include <LibWeb/Bindings/HTMLDataListElementPrototype.h>
#include <LibWeb/Bindings/HTMLDetailsElementConstructor.h>
#include <LibWeb/Bindings/HTMLDetailsElementPrototype.h>
#include <LibWeb/Bindings/HTMLDialogElementConstructor.h>
#include <LibWeb/Bindings/HTMLDialogElementPrototype.h>
#include <LibWeb/Bindings/HTMLDirectoryElementConstructor.h>
#include <LibWeb/Bindings/HTMLDirectoryElementPrototype.h>
#include <LibWeb/Bindings/HTMLDivElementConstructor.h>
#include <LibWeb/Bindings/HTMLDivElementPrototype.h>
#include <LibWeb/Bindings/HTMLElementConstructor.h>
#include <LibWeb/Bindings/HTMLElementPrototype.h>
#include <LibWeb/Bindings/HTMLEmbedElementConstructor.h>
#include <LibWeb/Bindings/HTMLEmbedElementPrototype.h>
#include <LibWeb/Bindings/HTMLFieldSetElementConstructor.h>
#include <LibWeb/Bindings/HTMLFieldSetElementPrototype.h>
#include <LibWeb/Bindings/HTMLFontElementConstructor.h>
#include <LibWeb/Bindings/HTMLFontElementPrototype.h>
#include <LibWeb/Bindings/HTMLFormElementConstructor.h>
#include <LibWeb/Bindings/HTMLFormElementPrototype.h>
#include <LibWeb/Bindings/HTMLFrameElementConstructor.h>
#include <LibWeb/Bindings/HTMLFrameElementPrototype.h>
#include <LibWeb/Bindings/HTMLFrameSetElementConstructor.h>
#include <LibWeb/Bindings/HTMLFrameSetElementPrototype.h>
#include <LibWeb/Bindings/HTMLHRElementConstructor.h>
#include <LibWeb/Bindings/HTMLHRElementPrototype.h>
#include <LibWeb/Bindings/HTMLHeadElementConstructor.h>
#include <LibWeb/Bindings/HTMLHeadElementPrototype.h>
#include <LibWeb/Bindings/HTMLHeadingElementConstructor.h>
#include <LibWeb/Bindings/HTMLHeadingElementPrototype.h>
#include <LibWeb/Bindings/HTMLHtmlElementConstructor.h>
#include <LibWeb/Bindings/HTMLHtmlElementPrototype.h>
#include <LibWeb/Bindings/HTMLIFrameElementConstructor.h>
#include <LibWeb/Bindings/HTMLIFrameElementPrototype.h>
#include <LibWeb/Bindings/HTMLImageElementConstructor.h>
#include <LibWeb/Bindings/HTMLImageElementPrototype.h>
#include <LibWeb/Bindings/HTMLInputElementConstructor.h>
#include <LibWeb/Bindings/HTMLInputElementPrototype.h>
#include <LibWeb/Bindings/HTMLLIElementConstructor.h>
#include <LibWeb/Bindings/HTMLLIElementPrototype.h>
#include <LibWeb/Bindings/HTMLLabelElementConstructor.h>
#include <LibWeb/Bindings/HTMLLabelElementPrototype.h>
#include <LibWeb/Bindings/HTMLLegendElementConstructor.h>
#include <LibWeb/Bindings/HTMLLegendElementPrototype.h>
#include <LibWeb/Bindings/HTMLLinkElementConstructor.h>
#include <LibWeb/Bindings/HTMLLinkElementPrototype.h>
#include <LibWeb/Bindings/HTMLMapElementConstructor.h>
#include <LibWeb/Bindings/HTMLMapElementPrototype.h>
#include <LibWeb/Bindings/HTMLMarqueeElementConstructor.h>
#include <LibWeb/Bindings/HTMLMarqueeElementPrototype.h>
#include <LibWeb/Bindings/HTMLMediaElementConstructor.h>
#include <LibWeb/Bindings/HTMLMediaElementPrototype.h>
#include <LibWeb/Bindings/HTMLMenuElementConstructor.h>
#include <LibWeb/Bindings/HTMLMenuElementPrototype.h>
#include <LibWeb/Bindings/HTMLMetaElementConstructor.h>
#include <LibWeb/Bindings/HTMLMetaElementPrototype.h>
#include <LibWeb/Bindings/HTMLMeterElementConstructor.h>
#include <LibWeb/Bindings/HTMLMeterElementPrototype.h>
#include <LibWeb/Bindings/HTMLModElementConstructor.h>
#include <LibWeb/Bindings/HTMLModElementPrototype.h>
#include <LibWeb/Bindings/HTMLOListElementConstructor.h>
#include <LibWeb/Bindings/HTMLOListElementPrototype.h>
#include <LibWeb/Bindings/HTMLObjectElementConstructor.h>
#include <LibWeb/Bindings/HTMLObjectElementPrototype.h>
#include <LibWeb/Bindings/HTMLOptGroupElementConstructor.h>
#include <LibWeb/Bindings/HTMLOptGroupElementPrototype.h>
#include <LibWeb/Bindings/HTMLOptionElementConstructor.h>
#include <LibWeb/Bindings/HTMLOptionElementPrototype.h>
#include <LibWeb/Bindings/HTMLOutputElementConstructor.h>
#include <LibWeb/Bindings/HTMLOutputElementPrototype.h>
#include <LibWeb/Bindings/HTMLParagraphElementConstructor.h>
#include <LibWeb/Bindings/HTMLParagraphElementPrototype.h>
#include <LibWeb/Bindings/HTMLParamElementConstructor.h>
#include <LibWeb/Bindings/HTMLParamElementPrototype.h>
#include <LibWeb/Bindings/HTMLPictureElementConstructor.h>
#include <LibWeb/Bindings/HTMLPictureElementPrototype.h>
#include <LibWeb/Bindings/HTMLPreElementConstructor.h>
#include <LibWeb/Bindings/HTMLPreElementPrototype.h>
#include <LibWeb/Bindings/HTMLProgressElementConstructor.h>
#include <LibWeb/Bindings/HTMLProgressElementPrototype.h>
#include <LibWeb/Bindings/HTMLQuoteElementConstructor.h>
#include <LibWeb/Bindings/HTMLQuoteElementPrototype.h>
#include <LibWeb/Bindings/HTMLScriptElementConstructor.h>
#include <LibWeb/Bindings/HTMLScriptElementPrototype.h>
#include <LibWeb/Bindings/HTMLSelectElementConstructor.h>
#include <LibWeb/Bindings/HTMLSelectElementPrototype.h>
#include <LibWeb/Bindings/HTMLSlotElementConstructor.h>
#include <LibWeb/Bindings/HTMLSlotElementPrototype.h>
#include <LibWeb/Bindings/HTMLSourceElementConstructor.h>
#include <LibWeb/Bindings/HTMLSourceElementPrototype.h>
#include <LibWeb/Bindings/HTMLSpanElementConstructor.h>
#include <LibWeb/Bindings/HTMLSpanElementPrototype.h>
#include <LibWeb/Bindings/HTMLStyleElementConstructor.h>
#include <LibWeb/Bindings/HTMLStyleElementPrototype.h>
#include <LibWeb/Bindings/HTMLTableCaptionElementConstructor.h>
#include <LibWeb/Bindings/HTMLTableCaptionElementPrototype.h>
#include <LibWeb/Bindings/HTMLTableCellElementConstructor.h>
#include <LibWeb/Bindings/HTMLTableCellElementPrototype.h>
#include <LibWeb/Bindings/HTMLTableColElementConstructor.h>
#include <LibWeb/Bindings/HTMLTableColElementPrototype.h>
#include <LibWeb/Bindings/HTMLTableElementConstructor.h>
#include <LibWeb/Bindings/HTMLTableElementPrototype.h>
#include <LibWeb/Bindings/HTMLTableRowElementConstructor.h>
#include <LibWeb/Bindings/HTMLTableRowElementPrototype.h>
#include <LibWeb/Bindings/HTMLTableSectionElementConstructor.h>
#include <LibWeb/Bindings/HTMLTableSectionElementPrototype.h>
#include <LibWeb/Bindings/HTMLTemplateElementConstructor.h>
#include <LibWeb/Bindings/HTMLTemplateElementPrototype.h>
#include <LibWeb/Bindings/HTMLTextAreaElementConstructor.h>
#include <LibWeb/Bindings/HTMLTextAreaElementPrototype.h>
#include <LibWeb/Bindings/HTMLTimeElementConstructor.h>
#include <LibWeb/Bindings/HTMLTimeElementPrototype.h>
#include <LibWeb/Bindings/HTMLTitleElementConstructor.h>
#include <LibWeb/Bindings/HTMLTitleElementPrototype.h>
#include <LibWeb/Bindings/HTMLTrackElementConstructor.h>
#include <LibWeb/Bindings/HTMLTrackElementPrototype.h>
#include <LibWeb/Bindings/HTMLUListElementConstructor.h>
#include <LibWeb/Bindings/HTMLUListElementPrototype.h>
#include <LibWeb/Bindings/HTMLUnknownElementConstructor.h>
#include <LibWeb/Bindings/HTMLUnknownElementPrototype.h>
#include <LibWeb/Bindings/HTMLVideoElementConstructor.h>
#include <LibWeb/Bindings/HTMLVideoElementPrototype.h>
#include <LibWeb/Bindings/ImageDataConstructor.h>
#include <LibWeb/Bindings/ImageDataPrototype.h>
#include <LibWeb/Bindings/MouseEventConstructor.h>
#include <LibWeb/Bindings/MouseEventPrototype.h>
#include <LibWeb/Bindings/NodeConstructor.h>
#include <LibWeb/Bindings/NodePrototype.h>
#include <LibWeb/Bindings/PerformanceConstructor.h>
#include <LibWeb/Bindings/PerformancePrototype.h>
#include <LibWeb/Bindings/SVGElementConstructor.h>
#include <LibWeb/Bindings/SVGElementPrototype.h>
#include <LibWeb/Bindings/SVGGeometryElementConstructor.h>
#include <LibWeb/Bindings/SVGGeometryElementPrototype.h>
#include <LibWeb/Bindings/SVGGraphicsElementConstructor.h>
#include <LibWeb/Bindings/SVGGraphicsElementPrototype.h>
#include <LibWeb/Bindings/SVGPathElementConstructor.h>
#include <LibWeb/Bindings/SVGPathElementPrototype.h>
#include <LibWeb/Bindings/SVGSVGElementConstructor.h>
#include <LibWeb/Bindings/SVGSVGElementPrototype.h>
#include <LibWeb/Bindings/ShadowRootConstructor.h>
#include <LibWeb/Bindings/ShadowRootPrototype.h>
#include <LibWeb/Bindings/SubmitEventConstructor.h>
#include <LibWeb/Bindings/SubmitEventPrototype.h>
#include <LibWeb/Bindings/TextConstructor.h>
#include <LibWeb/Bindings/TextPrototype.h>
#include <LibWeb/Bindings/UIEventConstructor.h>
#include <LibWeb/Bindings/UIEventPrototype.h>

#define ADD_WINDOW_OBJECT_INTERFACE(name)                 \
    {                                                     \
        ensure_web_constructor<name##Constructor>(#name); \
    }

#define ADD_WINDOW_OBJECT_INTERFACES                      \
    ADD_WINDOW_OBJECT_INTERFACE(CanvasRenderingContext2D) \
    ADD_WINDOW_OBJECT_INTERFACE(CharacterData)            \
    ADD_WINDOW_OBJECT_INTERFACE(Comment)                  \
    ADD_WINDOW_OBJECT_INTERFACE(DocumentFragment)         \
    ADD_WINDOW_OBJECT_INTERFACE(Document)                 \
    ADD_WINDOW_OBJECT_INTERFACE(DocumentType)             \
    ADD_WINDOW_OBJECT_INTERFACE(DOMImplementation)        \
    ADD_WINDOW_OBJECT_INTERFACE(Element)                  \
    ADD_WINDOW_OBJECT_INTERFACE(Event)                    \
    ADD_WINDOW_OBJECT_INTERFACE(EventTarget)              \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLAnchorElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLAreaElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLAudioElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLBaseElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLBodyElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLBRElement)            \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLButtonElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLCanvasElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLDataElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLDataListElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLDetailsElement)       \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLDialogElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLDirectoryElement)     \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLDivElement)           \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLDListElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLElement)              \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLEmbedElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLFieldSetElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLFontElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLFormElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLFrameElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLFrameSetElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLHeadElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLHeadingElement)       \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLHRElement)            \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLHtmlElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLIFrameElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLImageElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLInputElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLLabelElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLLegendElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLLIElement)            \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLLinkElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLMapElement)           \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLMarqueeElement)       \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLMediaElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLMenuElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLMetaElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLMeterElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLModElement)           \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLObjectElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLOListElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLOptGroupElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLOptionElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLOutputElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLParagraphElement)     \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLParamElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLPictureElement)       \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLPreElement)           \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLProgressElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLQuoteElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLScriptElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLSelectElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLSlotElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLSourceElement)        \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLSpanElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLStyleElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTableCaptionElement)  \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTableCellElement)     \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTableColElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTableElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTableRowElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTableSectionElement)  \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTemplateElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTextAreaElement)      \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTimeElement)          \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTitleElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLTrackElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLUListElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLUnknownElement)       \
    ADD_WINDOW_OBJECT_INTERFACE(HTMLVideoElement)         \
    ADD_WINDOW_OBJECT_INTERFACE(ImageData)                \
    ADD_WINDOW_OBJECT_INTERFACE(MouseEvent)               \
    ADD_WINDOW_OBJECT_INTERFACE(Node)                     \
    ADD_WINDOW_OBJECT_INTERFACE(Performance)              \
    ADD_WINDOW_OBJECT_INTERFACE(ShadowRoot)               \
    ADD_WINDOW_OBJECT_INTERFACE(SubmitEvent)              \
    ADD_WINDOW_OBJECT_INTERFACE(SVGElement)               \
    ADD_WINDOW_OBJECT_INTERFACE(SVGGeometryElement)       \
    ADD_WINDOW_OBJECT_INTERFACE(SVGGraphicsElement)       \
    ADD_WINDOW_OBJECT_INTERFACE(SVGPathElement)           \
    ADD_WINDOW_OBJECT_INTERFACE(SVGSVGElement)            \
    ADD_WINDOW_OBJECT_INTERFACE(Text)                     \
    ADD_WINDOW_OBJECT_INTERFACE(UIEvent)
