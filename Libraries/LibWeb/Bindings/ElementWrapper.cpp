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
#include <AK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/ElementWrapper.h>
#include <LibWeb/DOM/Element.h>

namespace Web {
namespace Bindings {

ElementWrapper::ElementWrapper(Element& element)
    : NodeWrapper(element)
{
    put_native_property("innerHTML", inner_html_getter, inner_html_setter);
    put_native_property("id", id_getter, id_setter);

    u8 attributes = JS::Attribute::Configurable | JS::Attribute::Enumerable | JS::Attribute::Writable;
    put_native_function("getAttribute", get_attribute, 1, attributes);
    put_native_function("setAttribute", set_attribute, 2, attributes);
}

ElementWrapper::~ElementWrapper()
{
}

Element& ElementWrapper::node()
{
    return static_cast<Element&>(NodeWrapper::node());
}

const Element& ElementWrapper::node() const
{
    return static_cast<const Element&>(NodeWrapper::node());
}

static Element* impl_from(JS::Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return nullptr;
    // FIXME: Verify that it's an ElementWrapper somehow!
    return &static_cast<ElementWrapper*>(this_object)->node();
}

JS::Value ElementWrapper::get_attribute(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};

    if (interpreter.argument_count() < 1)
        return interpreter.throw_exception<JS::TypeError>("getAttribute() needs one argument");

    auto attribute_name = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};

    auto attribute_value = impl->attribute(attribute_name);
    if (attribute_value.is_null())
        return JS::js_null();

    return JS::js_string(interpreter, attribute_value);
}

JS::Value ElementWrapper::set_attribute(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};

    if (interpreter.argument_count() < 2)
        return interpreter.throw_exception<JS::TypeError>("setAttribute() needs two arguments");

    auto attribute_name = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};

    auto attribute_value = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};

    impl->set_attribute(attribute_name, attribute_value);
    return JS::js_undefined();
}

JS::Value ElementWrapper::inner_html_getter(JS::Interpreter& interpreter)
{
    if (auto* impl = impl_from(interpreter))
        return JS::js_string(interpreter, impl->inner_html());
    return {};
}

void ElementWrapper::inner_html_setter(JS::Interpreter& interpreter, JS::Value value)
{
    if (auto* impl = impl_from(interpreter)) {
        auto string = value.to_string(interpreter);
        if (interpreter.exception())
            return;
        impl->set_inner_html(string);
    }
}

JS::Value ElementWrapper::id_getter(JS::Interpreter& interpreter)
{
    if (auto* impl = impl_from(interpreter))
        return JS::js_string(interpreter, impl->attribute("id"));
    return {};
}

void ElementWrapper::id_setter(JS::Interpreter& interpreter, JS::Value value)
{
    if (auto* impl = impl_from(interpreter)) {
        auto string = value.to_string(interpreter);
        if (interpreter.exception())
            return;
        impl->set_attribute("id", string);
    }
}

}
}
