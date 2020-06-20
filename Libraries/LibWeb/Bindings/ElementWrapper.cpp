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
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/ElementWrapper.h>
#include <LibWeb/DOM/AttributeNames.h>
#include <LibWeb/DOM/Element.h>

namespace Web {
namespace Bindings {

ElementWrapper::ElementWrapper(JS::GlobalObject& global_object, Element& element)
    : NodeWrapper(global_object, element)
{
}

void ElementWrapper::initialize(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    NodeWrapper::initialize(interpreter, global_object);

    define_native_property("innerHTML", inner_html_getter, inner_html_setter);
    define_native_property("id", id_getter, id_setter);

    u8 attributes = JS::Attribute::Configurable | JS::Attribute::Enumerable | JS::Attribute::Writable;
    define_native_function("getAttribute", get_attribute, 1, attributes);
    define_native_function("setAttribute", set_attribute, 2, attributes);
}

ElementWrapper::~ElementWrapper()
{
}

Element& ElementWrapper::node()
{
    return static_cast<Element&>(NodeWrapper::impl());
}

const Element& ElementWrapper::node() const
{
    return static_cast<const Element&>(NodeWrapper::impl());
}

static Element* impl_from(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    auto* this_object = interpreter.this_value(global_object).to_object(interpreter, global_object);
    if (!this_object)
        return nullptr;
    // FIXME: Verify that it's an ElementWrapper somehow!
    return &static_cast<ElementWrapper*>(this_object)->node();
}

JS_DEFINE_NATIVE_FUNCTION(ElementWrapper::get_attribute)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};

    if (interpreter.argument_count() < 1)
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::BadArgCountOne, "getAttribute");

    auto attribute_name = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};

    auto attribute_value = impl->attribute(attribute_name);
    if (attribute_value.is_null())
        return JS::js_null();

    return JS::js_string(interpreter, attribute_value);
}

JS_DEFINE_NATIVE_FUNCTION(ElementWrapper::set_attribute)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};

    if (interpreter.argument_count() < 2)
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::BadArgCountMany, "setAttribute", "two");

    auto attribute_name = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};

    auto attribute_value = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};

    impl->set_attribute(attribute_name, attribute_value);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_GETTER(ElementWrapper::inner_html_getter)
{
    if (auto* impl = impl_from(interpreter, global_object))
        return JS::js_string(interpreter, impl->inner_html());
    return {};
}

JS_DEFINE_NATIVE_SETTER(ElementWrapper::inner_html_setter)
{
    if (auto* impl = impl_from(interpreter, global_object)) {
        auto string = value.to_string(interpreter);
        if (interpreter.exception())
            return;
        impl->set_inner_html(string);
    }
}

JS_DEFINE_NATIVE_GETTER(ElementWrapper::id_getter)
{
    if (auto* impl = impl_from(interpreter, global_object))
        return JS::js_string(interpreter, impl->attribute(HTML::AttributeNames::id));
    return {};
}

JS_DEFINE_NATIVE_SETTER(ElementWrapper::id_setter)
{
    if (auto* impl = impl_from(interpreter, global_object)) {
        auto string = value.to_string(interpreter);
        if (interpreter.exception())
            return;
        impl->set_attribute(HTML::AttributeNames::id, string);
    }
}

}
}
