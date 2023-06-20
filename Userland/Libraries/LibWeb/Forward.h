/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibWeb/Bindings/Forward.h>

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
class XMLDocumentBuilder;
}

namespace Web::Bindings {
class Intrinsics;
class OptionConstructor;

enum class CanPlayTypeResult;
enum class CanvasFillRule;
enum class DOMParserSupportedType;
enum class EndingType;
enum class ImageSmoothingQuality;
enum class ReferrerPolicy;
enum class RequestCache;
enum class RequestCredentials;
enum class RequestDestination;
enum class RequestDuplex;
enum class RequestMode;
enum class RequestRedirect;
enum class ResizeObserverBoxOptions;
enum class ResponseType;
enum class XMLHttpRequestResponseType;
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
class AngleOrCalculated;
class AnglePercentage;
class AngleStyleValue;
class BackgroundRepeatStyleValue;
class BackgroundSizeStyleValue;
class BackgroundStyleValue;
class BorderRadiusShorthandStyleValue;
class BorderRadiusStyleValue;
class BorderStyleValue;
class CSSConditionRule;
class CSSFontFaceRule;
class CSSGroupingRule;
class CSSImportRule;
class CSSKeyframeRule;
class CSSKeyframesRule;
class CSSMediaRule;
class CSSRule;
class CSSRuleList;
class CSSStyleDeclaration;
class CSSStyleRule;
class CSSStyleSheet;
class CSSSupportsRule;
class CalculatedStyleValue;
class Clip;
class ColorStyleValue;
class CompositeStyleValue;
class ConicGradientStyleValue;
class ContentStyleValue;
class CustomIdentStyleValue;
class Display;
class DisplayStyleValue;
class EdgeStyleValue;
class ElementInlineCSSStyleDeclaration;
class ExplicitGridTrack;
class FilterValueListStyleValue;
class FlexFlowStyleValue;
class FlexStyleValue;
class FontFace;
class FontStyleValue;
class Frequency;
class FrequencyOrCalculated;
class FrequencyPercentage;
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
class GridTrackSizeListShorthandStyleValue;
class GridTrackSizeListStyleValue;
class IdentifierStyleValue;
class ImageStyleValue;
class InheritStyleValue;
class InitialStyleValue;
class IntegerStyleValue;
class Length;
class LengthBox;
class LengthOrCalculated;
class LengthPercentage;
class LengthStyleValue;
class LinearGradientStyleValue;
class ListStyleStyleValue;
class MediaFeatureValue;
class MediaList;
class MediaQuery;
class MediaQueryList;
class MediaQueryListEvent;
class Number;
class NumberStyleValue;
class OverflowStyleValue;
class Percentage;
class PercentageOrCalculated;
class PercentageStyleValue;
class PlaceContentStyleValue;
class PositionStyleValue;
class PropertyOwningCSSStyleDeclaration;
class RadialGradientStyleValue;
class Ratio;
class RatioStyleValue;
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
class SVGPaint;
class TextDecorationStyleValue;
class Time;
class TimeOrCalculated;
class TimePercentage;
class TimeStyleValue;
class TransformationStyleValue;
class URLStyleValue;
class UnicodeRange;
class UnresolvedStyleValue;
class UnsetStyleValue;

enum class MediaFeatureID;
enum class PropertyID;
enum class ValueID;

struct BackgroundLayerData;
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
class AbortController;
class AbortSignal;
class AbstractRange;
class AccessibilityTreeNode;
class Attr;
class CDATASection;
class CharacterData;
class Comment;
class CustomEvent;
class Document;
class DocumentFragment;
class DocumentLoadEventDelayer;
class DocumentObserver;
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
class XMLDocument;

enum class QuirksMode;

struct AddEventListenerOptions;
struct EventListenerOptions;
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
class ConnectionTimingInfo;
class FetchAlgorithms;
class FetchController;
class FetchParams;
class FetchTimingInfo;
class HeaderList;
class Request;
class Response;

struct BodyWithType;
struct Header;
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
class AudioTrack;
class AudioTrackList;
class BrowsingContext;
class BrowsingContextGroup;
class CanvasRenderingContext2D;
class ClassicScript;
class CloseEvent;
class CustomElementDefinition;
class CustomElementRegistry;
class DecodedImageData;
class DocumentState;
class DOMParser;
class DOMStringMap;
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
class HTMLSummaryElement;
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
class ImageRequest;
class ListOfAvailableImages;
class Location;
class MediaError;
class MessageChannel;
class MessageEvent;
class MessagePort;
class MimeType;
class MimeTypeArray;
class Navigable;
class NavigableContainer;
class Navigator;
struct NavigationParams;
class Origin;
class PageTransitionEvent;
class Path2D;
class Plugin;
class PluginArray;
class PromiseRejectionEvent;
class SharedImageRequest;
class Storage;
class SubmitEvent;
class TextMetrics;
class Timer;
class TimeRanges;
class TrackEvent;
class TraversableNavigable;
class VideoTrack;
class VideoTrackList;
class Window;
class WindowEnvironmentSettingsObject;
class WindowProxy;
class Worker;
class WorkerDebugConsoleClient;
class WorkerEnvironmentSettingsObject;
class WorkerGlobalScope;
class WorkerLocation;
class WorkerNavigator;

enum class MediaSeekMode;

struct CrossOriginOpenerPolicy;
struct CrossOriginOpenerPolicyEnforcementResult;
struct Environment;
struct EnvironmentSettingsObject;
struct NavigationParams;
struct POSTResource;
struct PolicyContainer;
struct SandboxingFlagSet;
struct SerializedFormData;
struct SessionHistoryEntry;
}

namespace Web::HighResolutionTime {
class Performance;
}

namespace Web::IntersectionObserver {
class IntersectionObserver;
}

namespace Web::Layout {
class AudioBox;
class BlockContainer;
class BlockFormattingContext;
class Box;
class ButtonBox;
class CheckBox;
class FlexFormattingContext;
class FormattingContext;
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
class TreeBuilder;
class VideoBox;
class Viewport;

enum class LayoutMode;

struct LayoutState;
}

namespace Web::MimeSniff {
class MimeType;
}

namespace Web::NavigationTiming {
class PerformanceTiming;
}

namespace Web::Painting {
class AudioPaintable;
class ButtonPaintable;
class CheckBoxPaintable;
class LabelablePaintable;
class MediaPaintable;
class Paintable;
class PaintableBox;
class PaintableWithLines;
class StackingContext;
class TextPaintable;
class VideoPaintable;

enum class PaintPhase;

struct BorderRadiiData;
struct BorderRadiusData;
struct LinearGradientData;
}

namespace Web::PerformanceTimeline {
class PerformanceEntry;
}

namespace Web::PermissionsPolicy {
class AutoplayAllowlist;
}

namespace Web::Platform {
class AudioCodecPlugin;
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
class ReadableByteStreamController;
class ReadableStream;
class ReadableStreamBYOBReader;
class ReadableStreamBYOBRequest;
class ReadableStreamDefaultController;
class ReadableStreamDefaultReader;
class ReadableStreamGenericReaderMixin;
class ReadRequest;
class WritableStream;
class WritableStreamDefaultController;
class WritableStreamDefaultWriter;

struct PullIntoDescriptor;
struct QueuingStrategyInit;
struct UnderlyingSink;
struct UnderlyingSource;
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
class SVGTitleElement;
}

namespace Web::UIEvents {
class KeyboardEvent;
class MouseEvent;
class UIEvents;
}

namespace Web::URL {
class URL;
class URLSearchParams;
class URLSearchParamsIterator;
}

namespace Web::UserTiming {
class PerformanceMark;
class PerformanceMeasure;
}

namespace Web::WebAssembly {
class Instance;
class Memory;
class Module;
class Table;
}

namespace Web::WebAudio {
class AudioContext;
class BaseAudioContext;
}

namespace Web::WebGL {
class WebGLContextEvent;
class WebGLRenderingContext;
class WebGLRenderingContextBase;
}

namespace Web::WebIDL {
class CallbackType;
class DOMException;

template<typename ValueType>
class ExceptionOr;

using Promise = JS::PromiseCapability;
}

namespace Web::WebSockets {
class WebSocket;
}

namespace Web::XHR {
class FormData;
class FormDataIterator;
class ProgressEvent;
class XMLHttpRequest;
class XMLHttpRequestEventTarget;
class XMLHttpRequestUpload;
struct FormDataEntry;
}
