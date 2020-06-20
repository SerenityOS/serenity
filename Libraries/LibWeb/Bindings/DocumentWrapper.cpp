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
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web {
namespace Bindings {

DocumentWrapper::DocumentWrapper(JS::GlobalObject& global_object, Document& document)
    : NodeWrapper(global_object, document)
{
}

void DocumentWrapper::initialize(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    NodeWrapper::initialize(interpreter, global_object);
    define_native_function("getElementById", get_element_by_id, 1);
    define_native_function("querySelector", query_selector, 1);
    define_native_function("querySelectorAll", query_selector_all, 1);
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

static Document* impl_from(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    auto* this_object = interpreter.this_value(global_object).to_object(interpreter, global_object);
    if (!this_object)
        return {};
    if (StringView("DocumentWrapper") != this_object->class_name()) {
        interpreter.throw_exception<JS::TypeError>(JS::ErrorType::NotA, "DocumentWrapper");
        return {};
    }
    return &static_cast<DocumentWrapper*>(this_object)->node();
}

JS_DEFINE_NATIVE_FUNCTION(DocumentWrapper::get_element_by_id)
{
    auto* document = impl_from(interpreter, global_object);
    if (!document)
        return {};
    if (!interpreter.argument_count())
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::BadArgCountOne, "getElementById");
    auto id = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};
    auto* element = document->get_element_by_id(id);
    if (!element)
        return JS::js_null();
    return wrap(interpreter.heap(), const_cast<Element&>(*element));
}

JS_DEFINE_NATIVE_FUNCTION(DocumentWrapper::query_selector)
{
    auto* document = impl_from(interpreter, global_object);
    if (!document)
        return {};
    if (!interpreter.argument_count())
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::BadArgCountOne, "querySelector");
    auto selector = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};
    // FIXME: Throw if selector is invalid
    auto element = document->query_selector(selector);
    if (!element)
        return JS::js_null();
    return wrap(interpreter.heap(), *element);
}

JS_DEFINE_NATIVE_FUNCTION(DocumentWrapper::query_selector_all)
{
    auto* document = impl_from(interpreter, global_object);
    if (!document)
        return {};
    if (!interpreter.argument_count())
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::BadArgCountOne, "querySelectorAll");
    auto selector = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};
    // FIXME: Throw if selector is invalid
    auto elements = document->query_selector_all(selector);
    // FIXME: This should be a static NodeList, not a plain JS::Array.
    auto* node_list = JS::Array::create(global_object);
    for (auto& element : elements) {
        node_list->indexed_properties().append(wrap(interpreter.heap(), element));
    }
    return node_list;
}

}
}
