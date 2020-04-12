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

#include <AK/FlyString.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web {
namespace Bindings {

DocumentWrapper::DocumentWrapper(Document& document)
    : NodeWrapper(document)
{
    put_native_function("getElementById", get_element_by_id, 1);
    put_native_function("querySelectorAll", query_selector_all, 1);
}

DocumentWrapper::~DocumentWrapper()
{
}

Document& DocumentWrapper::node()
{
    return static_cast<Document&>(NodeWrapper::node());
}

const Document& DocumentWrapper::node() const
{
    return static_cast<const Document&>(NodeWrapper::node());
}

static Document* document_from(JS::Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (StringView("DocumentWrapper") != this_object->class_name()) {
        interpreter.throw_exception<JS::TypeError>("That's not a DocumentWrapper, bro.");
        return {};
    }
    return &static_cast<DocumentWrapper*>(this_object)->node();
}

JS::Value DocumentWrapper::get_element_by_id(JS::Interpreter& interpreter)
{
    auto* document = document_from(interpreter);
    if (!document)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.is_empty())
        return JS::js_null();
    auto id = arguments[0].to_string();
    auto* element = document->get_element_by_id(id);
    if (!element)
        return JS::js_null();
    return wrap(interpreter.heap(), const_cast<Element&>(*element));
}

JS::Value DocumentWrapper::query_selector_all(JS::Interpreter& interpreter)
{
    auto* document = document_from(interpreter);
    if (!document)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.is_empty())
        return JS::js_null();
    auto selector = arguments[0].to_string();
    auto elements = document->query_selector_all(selector);
    // FIXME: This should be a static NodeList, not a plain JS::Array.
    auto* node_list = interpreter.heap().allocate<JS::Array>();
    for (auto& element : elements) {
        node_list->push(wrap(interpreter.heap(), element));
    }
    return node_list;
}

}
}
