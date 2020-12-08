/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/RangePrototype.h>
#include <LibWeb/Bindings/RangeWrapper.h>
#include <LibWeb/DOM/Range.h>

namespace Web::Bindings {

RangePrototype::RangePrototype(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void RangePrototype::initialize(JS::GlobalObject& global_object)
{
    auto default_attributes = JS::Attribute::Enumerable | JS::Attribute::Configurable;

    Object::initialize(global_object);

    define_native_function("setStart", set_start, 2);
    define_native_function("setEnd", set_end, 2);
    define_native_function("cloneRange", clone_range, 0);

    define_native_property("startContainer", start_container_getter, nullptr, default_attributes);
    define_native_property("endContainer", end_container_getter, nullptr, default_attributes);
    define_native_property("startOffset", start_offset_getter, nullptr, default_attributes);
    define_native_property("endOffset", end_offset_getter, nullptr, default_attributes);
}

static DOM::Range* impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (StringView("RangeWrapper") != this_object->class_name()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "Range");
        return nullptr;
    }
    return &static_cast<RangeWrapper*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(RangePrototype::set_start)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    auto arg0 = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto arg1 = vm.argument(1).to_number(global_object);
    if (vm.exception())
        return {};

    if (!arg0->is_node_wrapper()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "Node");
        return {};
    }

    impl->set_start(static_cast<NodeWrapper*>(arg0)->impl(), arg1.as_i32());

    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(RangePrototype::set_end)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    auto arg0 = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto arg1 = vm.argument(1).to_number(global_object);
    if (vm.exception())
        return {};

    if (!arg0->is_node_wrapper()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "Node");
        return {};
    }

    impl->set_end(static_cast<NodeWrapper*>(arg0)->impl(), arg1.as_i32());

    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(RangePrototype::clone_range)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    return wrap(global_object, *impl->clone_range());
}

JS_DEFINE_NATIVE_GETTER(RangePrototype::start_container_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    return wrap(global_object, *impl->start_container());
}

JS_DEFINE_NATIVE_GETTER(RangePrototype::end_container_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    return wrap(global_object, *impl->end_container());
}

JS_DEFINE_NATIVE_GETTER(RangePrototype::start_offset_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    return JS::Value(impl->start_offset());
}

JS_DEFINE_NATIVE_GETTER(RangePrototype::end_offset_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    return JS::Value(impl->end_offset());
}

}
