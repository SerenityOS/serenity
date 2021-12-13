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

namespace Web::Crypto {
class Crypto;
class SubtleCrypto;
}

namespace Web::CSS {
class BackgroundRepeatStyleValue;
class BackgroundSizeStyleValue;
class BackgroundStyleValue;
class BorderRadiusStyleValue;
class BorderStyleValue;
class BoxShadowStyleValue;
class CalculatedStyleValue;
class CalculatedStyleValue;
class ColorStyleValue;
class CSSImportRule;
class CSSMediaRule;
class CSSRule;
class CSSRuleList;
class CSSStyleDeclaration;
class CSSStyleRule;
class CSSStyleSheet;
class CSSSupportsRule;
class Display;
class ElementInlineCSSStyleDeclaration;
class FlexFlowStyleValue;
class FlexStyleValue;
class FontStyleValue;
class IdentifierStyleValue;
class ImageStyleValue;
class InheritStyleValue;
class InitialStyleValue;
class Length;
class LengthStyleValue;
class ListStyleStyleValue;
class MediaList;
class MediaQuery;
class MediaQueryList;
class MediaQueryListEvent;
class NumericStyleValue;
class OverflowStyleValue;
class PositionStyleValue;
class PropertyOwningCSSStyleDeclaration;
class Screen;
class Selector;
class StringStyleValue;
class StyleComputer;
class StyleProperties;
class StyleSheet;
class StyleValue;
class StyleValueList;
class Supports;
class TextDecorationStyleValue;
class TransformationStyleValue;
class UnresolvedStyleValue;
class UnsetStyleValue;
}

namespace Web::DOM {
class AbortController;
class AbortSignal;
class Attribute;
class CharacterData;
class Comment;
class CustomEvent;
class Document;
class DocumentFragment;
class DocumentLoadEventDelayer;
class DocumentType;
class DOMException;
class DOMImplementation;
class DOMTokenList;
class Element;
class Event;
class EventHandler;
class EventListener;
class EventTarget;
class HTMLCollection;
class LiveNodeList;
class NamedNodeMap;
class Node;
class NodeList;
class ParentNode;
class Position;
class ProcessingInstruction;
class Range;
class ShadowRoot;
class StaticNodeList;
class Text;
class Timer;
class Window;
enum class QuirksMode;

template<typename ValueType>
class ExceptionOr;
}

namespace Web::Encoding {
class TextEncoder;
}

namespace Web::Geometry {
class DOMRect;
class DOMRectReadOnly;
}

namespace Web::HTML {
class BrowsingContext;
class BrowsingContextContainer;
class CanvasRenderingContext2D;
class CloseEvent;
class DOMParser;
class DOMStringMap;
struct EventHandler;
class EventLoop;
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
class HTMLParser;
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
class MessageChannel;
class MessageEvent;
class MessagePort;
class PageTransitionEvent;
class PromiseRejectionEvent;
class SubmitEvent;
class WebSocket;
}

namespace Web::HighResolutionTime {
class Performance;
}

namespace Web::IntersectionObserver {
class IntersectionObserver;
}

namespace Web::NavigationTiming {
class PerformanceTiming;
}

namespace Web::RequestIdleCallback {
class IdleDeadline;
}

namespace Web::ResizeObserver {
class ResizeObserver;
}

namespace Web::SVG {
class SVGElement;
class SVGGeometryElement;
class SVGGraphicsElement;
class SVGPathElement;
class SVGSVGElement;
}

namespace Web::Selection {
class Selection;
}

namespace Web::Layout {
enum class LayoutMode;
enum class PaintPhase;
class BlockContainer;
class BlockFormattingContext;
class Box;
class ButtonBox;
class CheckBox;
class FlexFormattingContext;
class FormattingContext;
class InitialContainingBlock;
class InlineFormattingContext;
class Label;
class LabelableNode;
class LineBox;
class LineBoxFragment;
class Node;
class NodeWithStyle;
class NodeWithStyleAndBoxModelMetrics;
class RadioButton;
class ReplacedBox;
class TextNode;
}

namespace Web {
class EditEventHandler;
class EventHandler;
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
}

namespace Web::XHR {
class ProgressEvent;
class XMLHttpRequest;
class XMLHttpRequestEventTarget;
}

namespace Web::UIEvents {
class MouseEvent;
class KeyboardEvent;
class UIEvents;
}

namespace Web::URL {
class URL;
class URLSearchParams;
class URLSearchParamsIterator;
}

namespace Web::Bindings {
class AbortControllerWrapper;
class AbortSignalWrapper;
class AttributeWrapper;
class CanvasRenderingContext2DWrapper;
class CharacterDataWrapper;
class CloseEventWrapper;
class CommentWrapper;
class CryptoWrapper;
class CSSRuleListWrapper;
class CSSRuleWrapper;
class CSSStyleDeclarationWrapper;
class CSSStyleRuleWrapper;
class CSSStyleSheetWrapper;
class CustomEventWrapper;
class DocumentFragmentWrapper;
class DocumentTypeWrapper;
class DocumentWrapper;
class DOMExceptionWrapper;
class DOMImplementationWrapper;
class DOMParserWrapper;
class DOMRectReadOnlyWrapper;
class DOMRectWrapper;
class DOMStringMapWrapper;
class DOMTokenListWrapper;
class ElementWrapper;
class EventListenerWrapper;
class EventTargetWrapper;
class EventWrapper;
class HistoryWrapper;
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
class HTMLHeadElementWrapper;
class HTMLHeadingElementWrapper;
class HTMLHRElementWrapper;
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
class IdleDeadlineWrapper;
class ImageDataWrapper;
class IntersectionObserverWrapper;
class KeyboardEventWrapper;
class LocationObject;
class MediaQueryListEventWrapper;
class MediaQueryListWrapper;
class MessageChannelWrapper;
class MessageEventWrapper;
class MessagePortWrapper;
class MouseEventWrapper;
class NamedNodeMapWrapper;
class NodeListWrapper;
class NodeWrapper;
class PageTransitionEventWrapper;
class PerformanceTimingWrapper;
class PerformanceWrapper;
class ProcessingInstructionWrapper;
class ProgressEventWrapper;
class PromiseRejectionEventWrapper;
class RangeConstructor;
class RangePrototype;
class RangeWrapper;
class ResizeObserverWrapper;
class ScreenWrapper;
class ScriptExecutionContext;
class SelectionWrapper;
class StyleSheetListWrapper;
class StyleSheetWrapper;
class SubmitEventWrapper;
class SubtleCryptoWrapper;
class SVGElementWrapper;
class SVGGeometryElementWrapper;
class SVGGraphicsElementWrapper;
class SVGPathElementWrapper;
class SVGSVGElementWrapper;
class TextEncoderWrapper;
class TextWrapper;
class UIEventWrapper;
class URLConstructor;
class URLPrototype;
class URLSearchParamsConstructor;
class URLSearchParamsIteratorPrototype;
class URLSearchParamsIteratorWrapper;
class URLSearchParamsPrototype;
class URLSearchParamsWrapper;
class URLWrapper;
class WebSocketWrapper;
class WindowObject;
class Wrappable;
class Wrapper;
class XMLHttpRequestConstructor;
class XMLHttpRequestEventTargetWrapper;
class XMLHttpRequestPrototype;
class XMLHttpRequestWrapper;
}
