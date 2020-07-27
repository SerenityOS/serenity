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
class Document;
class DocumentType;
class Element;
class Event;
class EventHandler;
class EventListener;
class EventTarget;
class MouseEvent;
class Node;
class ParentNode;
class Text;
class Timer;
class Window;
enum class QuirksMode;
}

namespace Web {
class CanvasRenderingContext2D;
class Frame;
class HTMLAnchorElement;
class HTMLBodyElement;
class HTMLCanvasElement;
class HTMLDocumentParser;
class HTMLElement;
class HTMLFormElement;
class HTMLHeadElement;
class HTMLHtmlElement;
class HTMLImageElement;
class HTMLScriptElement;
class ImageData;
class LayoutBlock;
class LayoutDocument;
class LayoutNode;
class LayoutNodeWithStyle;
class LayoutReplaced;
class LineBox;
class LineBoxFragment;
class LoadRequest;
class Origin;
class Page;
class PageClient;
class PageView;
class PaintContext;
class Resource;
class ResourceLoader;
class StackingContext;
class XMLHttpRequest;
}

namespace Web::Bindings {

class CanvasRenderingContext2DWrapper;
class DocumentWrapper;
class DocumentTypeWrapper;
class ElementWrapper;
class EventWrapper;
class EventListenerWrapper;
class EventTargetWrapper;
class HTMLAnchorElementWrapper;
class HTMLBodyElementWrapper;
class HTMLBRElementWrapper;
class HTMLCanvasElementWrapper;
class HTMLElementWrapper;
class HTMLFormElementWrapper;
class HTMLHeadElementWrapper;
class HTMLHeadingElementWrapper;
class HTMLHRElementWrapper;
class HTMLHtmlElementWrapper;
class HTMLIFrameElementWrapper;
class HTMLImageElementWrapper;
class HTMLInputElementWrapper;
class HTMLLinkElementWrapper;
class HTMLObjectElementWrapper;
class HTMLScriptElementWrapper;
class HTMLStyleElementWrapper;
class HTMLTableCellElementWrapper;
class HTMLTableElementWrapper;
class HTMLTableRowElementWrapper;
class HTMLTitleElementWrapper;
class ImageDataWrapper;
class LocationObject;
class MouseEventWrapper;
class NodeWrapper;
class WindowObject;
class Wrappable;
class Wrapper;
class XMLHttpRequestConstructor;
class XMLHttpRequestPrototype;
class XMLHttpRequestWrapper;

}
