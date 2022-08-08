/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web {
class XMLDocumentBuilder;
}

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
class AbstractImageStyleValue;
class Angle;
class AnglePercentage;
class AngleStyleValue;
class BackgroundRepeatStyleValue;
class BackgroundSizeStyleValue;
class BackgroundStyleValue;
class BorderRadiusStyleValue;
class BorderRadiusShorthandStyleValue;
class BorderStyleValue;
class Clip;
class CalculatedStyleValue;
class ColorStyleValue;
class ContentStyleValue;
class CSSConditionRule;
class CSSGroupingRule;
class CSSImportRule;
class CSSFontFaceRule;
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
class FontFace;
class FontStyleValue;
class Frequency;
class FrequencyPercentage;
class FrequencyStyleValue;
class GridTrackPlacement;
class GridTrackPlacementShorthandStyleValue;
class GridTrackPlacementStyleValue;
class GridTrackSize;
class GridTrackSizeStyleValue;
class IdentifierStyleValue;
class ImageStyleValue;
class InheritStyleValue;
class InitialStyleValue;
class Length;
class LengthPercentage;
class LengthStyleValue;
class LinearGradientStyleValue;
class ListStyleStyleValue;
class MediaList;
class MediaQuery;
class MediaQueryList;
class MediaQueryListEvent;
class Number;
class NumericStyleValue;
class OverflowStyleValue;
class Percentage;
class PercentageStyleValue;
class PositionStyleValue;
class PropertyOwningCSSStyleDeclaration;
class RectStyleValue;
class Resolution;
class ResolutionStyleValue;
class Screen;
class Selector;
class ShadowStyleValue;
class StringStyleValue;
class StyleComputer;
class StyleProperties;
class StyleSheet;
class StyleSheetList;
class StyleValue;
class StyleValueList;
class Supports;
class TextDecorationStyleValue;
class Time;
class TimePercentage;
class TimeStyleValue;
class TransformationStyleValue;
class UnicodeRange;
class UnresolvedStyleValue;
class UnsetStyleValue;

enum class MediaFeatureID;
enum class PropertyID;
enum class ValueID;
}

namespace Web::CSS::Parser {
class Block;
class ComponentValue;
class Declaration;
class DeclarationOrAtRule;
class Function;
class Parser;
class Rule;
class Token;
class Tokenizer;
}

namespace Web::DOM {
class AbstractRange;
class AbortController;
class AbortSignal;
class Attribute;
class CDATASection;
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
class MutationObserver;
class MutationRecord;
class NamedNodeMap;
class Node;
class NodeFilter;
class NodeIterator;
class NodeList;
class ParentNode;
class Position;
class ProcessingInstruction;
class Range;
class ShadowRoot;
class StaticNodeList;
class StaticRange;
class Text;
class TreeWalker;
enum class QuirksMode;
struct EventListenerOptions;
struct AddEventListenerOptions;

template<typename ValueType>
class ExceptionOr;
}

namespace Web::DOMParsing {
class XMLSerializer;
}

namespace Web::Encoding {
class TextEncoder;
}

namespace Web::Fetch {
class Headers;
class HeadersIterator;
}

namespace Web::Fetch::Infrastructure {
class Body;
struct Header;
class HeaderList;
class Request;
class Response;
}

namespace Web::FileAPI {
class Blob;
class File;
}

namespace Web::Geometry {
class DOMPoint;
class DOMPointReadOnly;
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
struct CrossOriginOpenerPolicy;
struct CrossOriginOpenerPolicyEnforcementResult;
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
class HTMLOptionsCollection;
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
struct NavigationParams;
class Origin;
class PageTransitionEvent;
class Path2D;
struct PolicyContainer;
class PromiseRejectionEvent;
class WorkerDebugConsoleClient;
struct SandboxingFlagSet;
class Storage;
class SubmitEvent;
class TextMetrics;
class Timer;
class Window;
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

namespace Web::Painting {
enum class PaintPhase;
class ButtonPaintable;
class CheckBoxPaintable;
class LabelablePaintable;
class Paintable;
class PaintableBox;
class PaintableWithLines;
class StackingContext;
class TextPaintable;
struct BorderRadiusData;
struct BorderRadiiData;
struct LinearGradientData;
}

namespace Web::RequestIdleCallback {
class IdleDeadline;
}

namespace Web::ResizeObserver {
class ResizeObserver;
}

namespace Web::SVG {
class SVGAnimatedLength;
class SVGCircleElement;
class SVGClipPathElement;
class SVGDefsElement;
class SVGElement;
class SVGEllipseElement;
class SVGGeometryElement;
class SVGGraphicsElement;
class SVGLength;
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
class BlockContainer;
class BlockFormattingContext;
class Box;
class ButtonBox;
class CheckBox;
class FlexFormattingContext;
class FormattingContext;
struct LayoutState;
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
class LoadRequest;
class Page;
class PageClient;
class PaintContext;
class Resource;
class ResourceLoader;
}

namespace Web::WebGL {
class WebGLContextEvent;
class WebGLRenderingContext;
class WebGLRenderingContextBase;
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
class BlobWrapper;
struct CallbackType;
class CanvasGradientWrapper;
class CanvasRenderingContext2DWrapper;
class CDATASectionWrapper;
class CharacterDataWrapper;
class CloseEventWrapper;
class CommentWrapper;
class CryptoWrapper;
class CustomEventWrapper;
class DocumentFragmentWrapper;
class DocumentTypeWrapper;
class DocumentWrapper;
class DOMExceptionWrapper;
class DOMImplementationWrapper;
class DOMParserWrapper;
class DOMPointWrapper;
class DOMPointReadOnlyWrapper;
class DOMRectListWrapper;
class DOMRectReadOnlyWrapper;
class DOMRectWrapper;
class DOMTokenListWrapper;
class ElementWrapper;
class ErrorEventWrapper;
class EventListenerWrapper;
class EventTargetWrapper;
class EventWrapper;
class FileWrapper;
class FocusEventWrapper;
class HeadersWrapper;
class HeadersIteratorWrapper;
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
class HTMLOptionsCollectionWrapper;
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
class MediaListWrapper;
class MediaQueryListEventWrapper;
class MediaQueryListWrapper;
class MessageChannelWrapper;
class MessageEventWrapper;
class MessagePortWrapper;
class MouseEventWrapper;
class MutationObserverWrapper;
class MutationRecordWrapper;
class NodeFilterWrapper;
class NodeIteratorWrapper;
class NodeListWrapper;
class NodeWrapper;
class OptionConstructor;
class PageTransitionEventWrapper;
class Path2DWrapper;
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
class SubmitEventWrapper;
class SubtleCryptoWrapper;
class SVGAnimatedLengthWrapper;
class SVGCircleElementWrapper;
class SVGDefsElementWrapper;
class SVGClipPathElementWrapper;
class SVGElementWrapper;
class SVGEllipseElementWrapper;
class SVGGeometryElementWrapper;
class SVGGraphicsElementWrapper;
class SVGLengthWrapper;
class SVGLineElementWrapper;
class SVGPathElementWrapper;
class SVGPolygonElementWrapper;
class SVGPolylineElementWrapper;
class SVGRectElementWrapper;
class SVGSVGElementWrapper;
class SVGTextContentElementWrapper;
class TextDecoderWrapper;
class TextEncoderWrapper;
class TextMetricsWrapper;
class TextWrapper;
class TreeWalkerWrapper;
class UIEventWrapper;
class URLConstructor;
class URLPrototype;
class URLSearchParamsConstructor;
class URLSearchParamsIteratorPrototype;
class URLSearchParamsIteratorWrapper;
class URLSearchParamsPrototype;
class URLSearchParamsWrapper;
class URLWrapper;
class WebGLContextEventWrapper;
class WebGLRenderingContextWrapper;
class WebSocketWrapper;
class WindowObject;
class WindowProxy;
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
class XMLSerializerWrapper;
enum class CanPlayTypeResult;
enum class CanvasFillRule;
enum class EndingType;
enum class DOMParserSupportedType;
enum class ResizeObserverBoxOptions;
enum class XMLHttpRequestResponseType;
}
