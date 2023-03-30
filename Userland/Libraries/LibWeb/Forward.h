/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/Forward.h>

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
class AngleOrCalculated;
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
class ConicGradientStyleValue;
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
class ExplicitGridTrack;
class FilterValueListStyleValue;
class FlexFlowStyleValue;
class FlexStyleValue;
class FontFace;
class FontStyleValue;
class Frequency;
class FrequencyPercentage;
class FrequencyOrCalculated;
class FrequencyStyleValue;
class GridAreaShorthandStyleValue;
class GridMinMax;
class GridRepeat;
class GridSize;
class GridTemplateAreaStyleValue;
class GridTrackPlacement;
class GridTrackPlacementShorthandStyleValue;
class GridTrackPlacementStyleValue;
class GridTrackSizeList;
class GridTrackSizeStyleValue;
class IdentifierStyleValue;
class ImageStyleValue;
class InheritStyleValue;
class InitialStyleValue;
class Length;
class LengthBox;
class LengthPercentage;
class LengthOrCalculated;
class LengthStyleValue;
class LinearGradientStyleValue;
class ListStyleStyleValue;
class MediaFeatureValue;
class MediaList;
class MediaQuery;
class MediaQueryList;
class MediaQueryListEvent;
class Number;
class NumericStyleValue;
class OverflowStyleValue;
class Percentage;
class PercentageOrCalculated;
class PercentageStyleValue;
class PositionStyleValue;
class PropertyOwningCSSStyleDeclaration;
class RadialGradientStyleValue;
class RectStyleValue;
class Resolution;
class ResolutionStyleValue;
class Screen;
class Selector;
class ShadowStyleValue;
class Size;
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
class TimeOrCalculated;
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
class AccessibilityTreeNode;
class Attr;
class CDATASection;
class CharacterData;
class Comment;
class CustomEvent;
class Document;
class DocumentFragment;
class DocumentLoadEventDelayer;
class DocumentType;
class DOMEventListener;
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
class RegisteredObserver;
class ShadowRoot;
class StaticNodeList;
class StaticRange;
class Text;
class TreeWalker;
enum class QuirksMode;
struct EventListenerOptions;
struct AddEventListenerOptions;
}

namespace Web::DOMParsing {
class XMLSerializer;
}

namespace Web::Encoding {
class TextEncoder;
}

namespace Web::Fetch {
class BodyMixin;
class Headers;
class HeadersIterator;
class Request;
class Response;
}

namespace Web::Fetch::Fetching {
class PendingResponse;
class RefCountedFlag;
}

namespace Web::Fetch::Infrastructure {
class Body;
struct BodyWithType;
class ConnectionTimingInfo;
class FetchAlgorithms;
class FetchController;
class FetchParams;
class FetchTimingInfo;
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
class DOMMatrix;
class DOMMatrixReadOnly;
class DOMPoint;
class DOMPointReadOnly;
class DOMRect;
class DOMRectList;
class DOMRectReadOnly;
}

namespace Web::HTML {
class BrowsingContext;
class BrowsingContextContainer;
class BrowsingContextGroup;
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
class EventHandler;
class EventLoop;
class FormDataEvent;
class History;
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
class Location;
class MessageChannel;
class MessageEvent;
class MessagePort;
class MimeType;
class MimeTypeArray;
struct NavigationParams;
class Navigator;
class Origin;
class PageTransitionEvent;
class Path2D;
class Plugin;
class PluginArray;
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
class WindowProxy;
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

namespace Web::PerformanceTimeline {
class PerformanceEntry;
}

namespace Web::Platform {
class Timer;
}

namespace Web::ReferrerPolicy {
enum class ReferrerPolicy;
}

namespace Web::RequestIdleCallback {
class IdleDeadline;
}

namespace Web::ResizeObserver {
class ResizeObserver;
}

namespace Web::Selection {
class Selection;
}

namespace Web::Streams {
class ReadableStream;
}

namespace Web::SVG {
class SVGAnimatedLength;
class SVGCircleElement;
class SVGClipPathElement;
class SVGDefsElement;
class SVGElement;
class SVGEllipseElement;
class SVGForeignObjectElement;
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

namespace Web::WebIDL {
class CallbackType;
class DOMException;

template<typename ValueType>
class ExceptionOr;
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
class Viewport;
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
class TableWrapper;
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

namespace Web::WebAssembly {
class Instance;
class Memory;
class Module;
class Table;
}

namespace Web::WebGL {
class WebGLContextEvent;
class WebGLRenderingContext;
class WebGLRenderingContextBase;
}

namespace Web::XHR {
class FormData;
class FormDataIterator;
class ProgressEvent;
class XMLHttpRequest;
class XMLHttpRequestEventTarget;
class XMLHttpRequestUpload;
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

namespace Web::UserTiming {
class PerformanceMark;
}

namespace Web::Bindings {
class Intrinsics;
class OptionConstructor;
enum class CanPlayTypeResult;
enum class CanvasFillRule;
enum class EndingType;
enum class DOMParserSupportedType;
enum class ImageSmoothingQuality;
enum class ReferrerPolicy;
enum class RequestDestination;
enum class RequestMode;
enum class RequestCredentials;
enum class RequestCache;
enum class RequestRedirect;
enum class RequestDuplex;
enum class ResponseType;
enum class ResizeObserverBoxOptions;
enum class XMLHttpRequestResponseType;
}
