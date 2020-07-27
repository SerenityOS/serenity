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

#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/DocumentTypeWrapper.h>
#include <LibWeb/Bindings/HTMLAnchorElementWrapper.h>
#include <LibWeb/Bindings/HTMLBodyElementWrapper.h>
#include <LibWeb/Bindings/HTMLBRElementWrapper.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/Bindings/HTMLElementWrapper.h>
#include <LibWeb/Bindings/HTMLFormElementWrapper.h>
#include <LibWeb/Bindings/HTMLHeadElementWrapper.h>
#include <LibWeb/Bindings/HTMLHeadingElementWrapper.h>
#include <LibWeb/Bindings/HTMLHRElementWrapper.h>
#include <LibWeb/Bindings/HTMLHtmlElementWrapper.h>
#include <LibWeb/Bindings/HTMLIFrameElementWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/HTMLInputElementWrapper.h>
#include <LibWeb/Bindings/HTMLLinkElementWrapper.h>
#include <LibWeb/Bindings/HTMLObjectElementWrapper.h>
#include <LibWeb/Bindings/HTMLScriptElementWrapper.h>
#include <LibWeb/Bindings/HTMLStyleElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableCellElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableRowElementWrapper.h>
#include <LibWeb/Bindings/HTMLTitleElementWrapper.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLBRElement.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHeadingElement.h>
#include <LibWeb/HTML/HTMLHRElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/HTML/HTMLObjectElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLStyleElement.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>
#include <LibWeb/HTML/HTMLTableElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/DOM/Node.h>

namespace Web {
namespace Bindings {

NodeWrapper* wrap(JS::GlobalObject& global_object, DOM::Node& node)
{
    if (is<DOM::Document>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::Document>(node)));
    if (is<DOM::DocumentType>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::DocumentType>(node)));
    if (is<HTMLAnchorElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLAnchorElement>(node)));
    if (is<HTMLBodyElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLBodyElement>(node)));
    if (is<HTMLBRElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLBRElement>(node)));
    if (is<HTMLCanvasElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLCanvasElement>(node)));
    if (is<HTMLFormElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLFormElement>(node)));
    if (is<HTMLHeadElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLHeadElement>(node)));
    if (is<HTMLHeadingElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLHeadingElement>(node)));
    if (is<HTMLHRElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLHRElement>(node)));
    if (is<HTMLHtmlElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLHtmlElement>(node)));
    if (is<HTMLIFrameElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLIFrameElement>(node)));
    if (is<HTMLImageElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLImageElement>(node)));
    if (is<HTMLInputElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLInputElement>(node)));
    if (is<HTMLLinkElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLLinkElement>(node)));
    if (is<HTMLObjectElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLObjectElement>(node)));
    if (is<HTMLScriptElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLScriptElement>(node)));
    if (is<HTMLStyleElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLStyleElement>(node)));
    if (is<HTMLTableCellElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLTableCellElement>(node)));
    if (is<HTMLTableElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLTableElement>(node)));
    if (is<HTMLTableRowElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLTableRowElement>(node)));
    if (is<HTMLTitleElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLTitleElement>(node)));
    if (is<HTMLElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<HTMLElement>(node)));
    if (is<DOM::Element>(node))
        return static_cast<NodeWrapper*>(wrap_impl(global_object, downcast<DOM::Element>(node)));
    return static_cast<NodeWrapper*>(wrap_impl(global_object, node));
}

}
}
