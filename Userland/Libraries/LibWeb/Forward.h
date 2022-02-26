/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
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
class Angle;
class AnglePercentage;
class AngleStyleValue;
class BackgroundRepeatStyleValue;
class BackgroundSizeStyleValue;
class BackgroundStyleValue;
class BorderRadiusStyleValue;
class BorderStyleValue;
class BoxShadowStyleValue;
class CalculatedStyleValue;
class ColorStyleValue;
class ContentStyleValue;
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
class Frequency;
class FrequencyPercentage;
class FrequencyStyleValue;
class IdentifierStyleValue;
class ImageStyleValue;
class InheritStyleValue;
class InitialStyleValue;
class Length;
class LengthPercentage;
class LengthStyleValue;
class ListStyleStyleValue;
class MediaList;
class MediaQuery;
class MediaQueryList;
class MediaQueryListEvent;
class NumericStyleValue;
class OverflowStyleValue;
class Percentage;
class PercentageStyleValue;
class PositionStyleValue;
class PropertyOwningCSSStyleDeclaration;
class Resolution;
class ResolutionStyleValue;
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
class Time;
class TimePercentage;
class TimeStyleValue;
class TransformationStyleValue;
class UnresolvedStyleValue;
class UnsetStyleValue;
}

namespace Web::DOM {
class AbstractRange;
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
class DOMEventListener;
class DOMException;
class DOMImplementation;
class DOMTokenList;
class Element;
class Event;
class EventHandler;
class EventTarget;
class HTMLCollection;
class IDLEventListener;
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
class StaticRange;
class Text;
class Timer;
class Window;
enum class QuirksMode;
struct EventListenerOptions;
struct AddEventListenerOptions;

template<typename ValueType>
class ExceptionOr;
}

namespace Web::Encoding {
class TextEncoder;
}

namespace Web::Geometry {
class DOMRect;
class DOMRectList;
class DOMRectReadOnly;
}

namespace Web::HTML {
class BrowsingContext;
class BrowsingContextContainer;
class CanvasRenderingContext2D;
class ClassicScript;
class CloseEvent;
class DOMParser;
class DOMStringMap;
struct Environment;
struct EnvironmentSettingsObject;
class ErrorEvent;
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
class WorkerDebugConsoleClient;
class Storage;
class SubmitEvent;
class TextMetrics;
class WindowEnvironmentSettingsObject;
class Worker;
class WorkerEnvironmentSettingsObject;
class WorkerGlobalScope;
class WorkerLocation;
class WorkerNavigator;
}

namespace Web::HighResolutionTime {
class Performance;
}

namespace Web::IntersectionObserver {
class IntersectionObserver;
}

namespace Web::MimeSniff {
class MimeType;
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
class SVGCircleElement;
class SVGElement;
class SVGEllipseElement;
class SVGGeometryElement;
class SVGGraphicsElement;
class SVGLineElement;
class SVGPathElement;
class SVGPolygonElement;
class SVGPolylineElement;
class SVGRectElement;
class SVGSVGElement;
}

namespace Web::Selection {
class Selection;
}

namespace Web::WebSockets {
class WebSocket;
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
class ListItemBox;
class ListItemMarkerBox;
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
class AbstractRangeWrapper;
class AbortControllerWrapper;
class AbortSignalWrapper;
class AttributeWrapper;
struct CallbackType;
class CanvasGradientWrapper;
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
class DOMRectListWrapper;
class DOMRectReadOnlyWrapper;
class DOMRectWrapper;
class DOMStringMapWrapper;
class DOMTokenListWrapper;
class ElementWrapper;
class ErrorEventWrapper;
class EventListenerWrapper;
class EventTargetWrapper;
class EventWrapper;
class FocusEventWrapper;
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
class SelectionWrapper;
class StaticRangeWrapper;
class StorageWrapper;
class StyleSheetListWrapper;
class StyleSheetWrapper;
class SubmitEventWrapper;
class SubtleCryptoWrapper;
class SVGCircleElementWrapper;
class SVGElementWrapper;
class SVGEllipseElementWrapper;
class SVGGeometryElementWrapper;
class SVGGraphicsElementWrapper;
class SVGLineElementWrapper;
class SVGPathElementWrapper;
class SVGPolygonElementWrapper;
class SVGPolylineElementWrapper;
class SVGRectElementWrapper;
class SVGSVGElementWrapper;
class TextDecoderWrapper;
class TextEncoderWrapper;
class TextMetricsWrapper;
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
class WorkerWrapper;
class WorkerGlobalScopeWrapper;
class WorkerLocationWrapper;
class WorkerNavigatorWrapper;
class Wrappable;
class Wrapper;
class XMLHttpRequestConstructor;
class XMLHttpRequestEventTargetWrapper;
class XMLHttpRequestPrototype;
class XMLHttpRequestWrapper;
enum class DOMParserSupportedType;
enum class XMLHttpRequestResponseType;
}
