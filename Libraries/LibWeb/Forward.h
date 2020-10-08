/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

namespace Web::CSS {
class Selector;
class StyleProperties;
class StyleResolver;
class StyleRule;
class StyleSheet;
}

namespace Web::DOM {
class CharacterData;
class Comment;
class Document;
class DocumentFragment;
class DocumentType;
class Element;
class Event;
class EventHandler;
class EventListener;
class EventTarget;
class MouseEvent;
class Node;
class ParentNode;
class Position;
class Text;
class Timer;
class Window;
enum class QuirksMode;
}

namespace Web::HTML {
class CanvasRenderingContext2D;
class HTMLAnchorElement;
class HTMLAreaElement;
class HTMLAudioElement;
class HTMLBaseElement;
class HTMLBlinkElement;
class HTMLBodyElement;
class HTMLBRElement;
class HTMLButtonElement;
class HTMLCanvasElement;
class HTMLDataElement;
class HTMLDataListElement;
class HTMLDetailsElement;
class HTMLDialogElement;
class HTMLDivElement;
class HTMLDListElement;
class HTMLDocumentParser;
class HTMLElement;
class HTMLEmbedElement;
class HTMLFieldSetElement;
class HTMLFontElement;
class HTMLFormElement;
class HTMLFrameElement;
class HTMLFrameSetElement;
class HTMLHeadElement;
class HTMLHeadingElement;
class HTMLHRElement;
class HTMLHtmlElement;
class HTMLIFrameElement;
class HTMLImageElement;
class HTMLInputElement;
class HTMLLabelElement;
class HTMLLegendElement;
class HTMLLIElement;
class HTMLLinkElement;
class HTMLMapElement;
class HTMLMarqueeElement;
class HTMLMediaElement;
class HTMLMenuElement;
class HTMLMetaElement;
class HTMLMeterElement;
class HTMLModElement;
class HTMLObjectElement;
class HTMLOListElement;
class HTMLOptGroupElement;
class HTMLOptionElement;
class HTMLOutputElement;
class HTMLParagraphElement;
class HTMLParamElement;
class HTMLPictureElement;
class HTMLPreElement;
class HTMLProgressElement;
class HTMLQuoteElement;
class HTMLScriptElement;
class HTMLSelectElement;
class HTMLSlotElement;
class HTMLSourceElement;
class HTMLSpanElement;
class HTMLStyleElement;
class HTMLTableCaptionElement;
class HTMLTableCellElement;
class HTMLTableColElement;
class HTMLTableElement;
class HTMLTableRowElement;
class HTMLTableSectionElement;
class HTMLTemplateElement;
class HTMLTextAreaElement;
class HTMLTimeElement;
class HTMLTitleElement;
class HTMLTrackElement;
class HTMLUListElement;
class HTMLUnknownElement;
class HTMLVideoElement;
class ImageData;
}

namespace Web::HighResolutionTime {
class Performance;
}

namespace Web::SVG {
class SVGElement;
class SVGGeometryElement;
class SVGGraphicsElement;
class SVGPathElement;
class SVGSVGElement;
}

namespace Web {
class EventHandler;
class Frame;
class FrameLoader;
class InProcessWebView;
class LayoutBlock;
class LayoutButton;
class LayoutCheckBox;
class LayoutDocument;
class LayoutNode;
class LayoutNodeWithStyle;
class LayoutReplaced;
class LineBox;
class LineBoxFragment;
class LoadRequest;
class Origin;
class OutOfProcessWebView;
class Page;
class PageClient;
class PaintContext;
class Resource;
class ResourceLoader;
class StackingContext;
class XMLHttpRequest;
}

namespace Web::Bindings {

class CanvasRenderingContext2DWrapper;
class CharacterDataWrapper;
class CommentWrapper;
class DocumentFragmentWrapper;
class DocumentTypeWrapper;
class DocumentWrapper;
class ElementWrapper;
class EventListenerWrapper;
class EventTargetWrapper;
class EventWrapper;
class HTMLAnchorElementWrapper;
class HTMLAreaElementWrapper;
class HTMLAudioElementWrapper;
class HTMLBaseElementWrapper;
class HTMLBodyElementWrapper;
class HTMLBRElementWrapper;
class HTMLButtonElementWrapper;
class HTMLCanvasElementWrapper;
class HTMLDataElementWrapper;
class HTMLDataListElementWrapper;
class HTMLDetailsElementWrapper;
class HTMLDialogElementWrapper;
class HTMLDivElementWrapper;
class HTMLDListElementWrapper;
class HTMLElementWrapper;
class HTMLEmbedElementWrapper;
class HTMLFieldSetElementWrapper;
class HTMLFormElementWrapper;
class HTMLFrameElementWrapper;
class HTMLFrameSetElementWrapper;
class HTMLHRElementWrapper;
class HTMLHeadElementWrapper;
class HTMLHeadingElementWrapper;
class HTMLHtmlElementWrapper;
class HTMLIFrameElementWrapper;
class HTMLImageElementWrapper;
class HTMLInputElementWrapper;
class HTMLLabelElementWrapper;
class HTMLLegendElementWrapper;
class HTMLLIElementWrapper;
class HTMLLinkElementWrapper;
class HTMLMapElementWrapper;
class HTMLMarqueeElementWrapper;
class HTMLMediaElementWrapper;
class HTMLMenuElementWrapper;
class HTMLMetaElementWrapper;
class HTMLMeterElementWrapper;
class HTMLModElementWrapper;
class HTMLObjectElementWrapper;
class HTMLOListElementWrapper;
class HTMLOptGroupElementWrapper;
class HTMLOptionElementWrapper;
class HTMLOutputElementWrapper;
class HTMLParagraphElementWrapper;
class HTMLParamElementWrapper;
class HTMLPictureElementWrapper;
class HTMLPreElementWrapper;
class HTMLProgressElementWrapper;
class HTMLQuoteElementWrapper;
class HTMLScriptElementWrapper;
class HTMLSelectElementWrapper;
class HTMLSlotElementWrapper;
class HTMLSourceElementWrapper;
class HTMLSpanElementWrapper;
class HTMLStyleElementWrapper;
class HTMLTableCaptionElementWrapper;
class HTMLTableCellElementWrapper;
class HTMLTableColElementWrapper;
class HTMLTableElementWrapper;
class HTMLTableRowElementWrapper;
class HTMLTableSectionElementWrapper;
class HTMLTemplateElementWrapper;
class HTMLTextAreaElementWrapper;
class HTMLTimeElementWrapper;
class HTMLTitleElementWrapper;
class HTMLTrackElementWrapper;
class HTMLUListElementWrapper;
class HTMLUnknownElementWrapper;
class HTMLVideoElementWrapper;
class ImageDataWrapper;
class LocationObject;
class MouseEventWrapper;
class NodeWrapper;
class PerformanceWrapper;
class ScriptExecutionContext;
class SVGElementWrapper;
class SVGGeometryElementWrapper;
class SVGGraphicsElementWrapper;
class SVGPathElementWrapper;
class SVGSVGElementWrapper;
class TextWrapper;
class UIEventWrapper;
class WindowObject;
class Wrappable;
class Wrapper;
class XMLHttpRequestConstructor;
class XMLHttpRequestPrototype;
class XMLHttpRequestWrapper;

}
