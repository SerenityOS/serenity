/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::Cookie {
struct Cookie;
struct ParsedCookie;
enum class Source;
}

namespace Web::CSS {
class CSSRule;
class CSSImportRule;
class CSSStyleDeclaration;
class CSSStyleRule;
class CSSStyleSheet;
class ElementInlineCSSStyleDeclaration;
class Length;
class Screen;
class Selector;
class StyleProperties;
class StyleResolver;
class StyleSheet;
enum class Display;
}

namespace Web::DOM {
class CharacterData;
class Comment;
class Document;
class DocumentFragment;
class DocumentType;
class DOMException;
class DOMImplementation;
class Element;
class Event;
class EventHandler;
class EventListener;
class EventTarget;
class HTMLCollection;
class MouseEvent;
class Node;
class ParentNode;
class Position;
class ProcessingInstruction;
class ShadowRoot;
class Text;
class Timer;
class Window;
class Range;
enum class QuirksMode;
}

namespace Web::HTML {
class CanvasRenderingContext2D;
class CloseEvent;
class EventHandler;
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
class HTMLDirectoryElement;
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
class MessageEvent;
class WebSocket;
}

namespace Web::HighResolutionTime {
class Performance;
}

namespace Web::NavigationTiming {
class PerformanceTiming;
}

namespace Web::SVG {
class SVGElement;
class SVGGeometryElement;
class SVGGraphicsElement;
class SVGPathElement;
class SVGSVGElement;
}

namespace Web::Layout {
enum class LayoutMode;
enum class PaintPhase;
class BlockBox;
class BlockFormattingContext;
class Box;
class ButtonBox;
class CheckBox;
class FormattingContext;
class InitialContainingBlockBox;
class InlineFormattingContext;
class Label;
class LabelableNode;
class LineBox;
class LineBoxFragment;
class Node;
class NodeWithStyle;
class RadioButton;
class ReplacedBox;
class TextNode;
}

namespace Web {
class EventHandler;
class EditEventHandler;
class BrowsingContext;
class FrameLoader;
class InProcessWebView;
class LoadRequest;
class Origin;
class OutOfProcessWebView;
class Page;
class PageClient;
class PaintContext;
class Resource;
class ResourceLoader;
class StackingContext;
}

namespace Web::XHR {
class ProgressEvent;
class XMLHttpRequest;
class XMLHttpRequestEventTarget;
}

namespace Web::Bindings {
class CSSStyleDeclarationWrapper;
class CSSStyleSheetWrapper;
class CanvasRenderingContext2DWrapper;
class CharacterDataWrapper;
class CloseEventWrapper;
class CommentWrapper;
class DocumentFragmentWrapper;
class DocumentTypeWrapper;
class DocumentWrapper;
class DOMExceptionWrapper;
class DOMImplementationWrapper;
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
class HTMLCollectionWrapper;
class HTMLDataElementWrapper;
class HTMLDataListElementWrapper;
class HTMLDetailsElementWrapper;
class HTMLDialogElementWrapper;
class HTMLDirectoryElementWrapper;
class HTMLDivElementWrapper;
class HTMLDListElementWrapper;
class HTMLElementWrapper;
class HTMLEmbedElementWrapper;
class HTMLFieldSetElementWrapper;
class HTMLFontElementWrapper;
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
class MessageEventWrapper;
class MouseEventWrapper;
class NodeWrapper;
class PerformanceTimingWrapper;
class PerformanceWrapper;
class ProcessingInstructionWrapper;
class ProgressEventWrapper;
class ScreenWrapper;
class ScriptExecutionContext;
class SubmitEventWrapper;
class SVGElementWrapper;
class SVGGeometryElementWrapper;
class SVGGraphicsElementWrapper;
class SVGPathElementWrapper;
class SVGSVGElementWrapper;
class StyleSheetWrapper;
class StyleSheetListWrapper;
class TextWrapper;
class UIEventWrapper;
class WebSocketWrapper;
class WindowObject;
class Wrappable;
class Wrapper;
class XMLHttpRequestConstructor;
class XMLHttpRequestPrototype;
class XMLHttpRequestWrapper;
class XMLHttpRequestEventTargetWrapper;
class RangeConstructor;
class RangePrototype;
class RangeWrapper;

}
