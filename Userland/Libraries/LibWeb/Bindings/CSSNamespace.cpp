/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/CSSNamespace.h>

namespace Web::Bindings {

CSSNamespace::CSSNamespace(JS::GlobalObject& global_object)
    : JS::Object(*global_object.object_prototype())
{
}

CSSNamespace::~CSSNamespace()
{
}

void CSSNamespace::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);
    u8 attr = JS::Attribute::Enumerable;
    define_native_function("escape", escape, 1, attr);
    define_native_function("supports", supports, 2, attr);
}

// https://www.w3.org/TR/cssom-1/#dom-css-escape
JS_DEFINE_NATIVE_FUNCTION(CSSNamespace::escape)
{
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "CSS.escape");
        return {};
    }

    String result = Web::CSS::serialize_an_identifier(vm.argument(0).to_string(global_object));
    if (vm.exception())
        return {};

    return JS::Value(JS::js_string(vm, result));
}

// https://www.w3.org/TR/css-conditional-3/#dom-css-supports
JS_DEFINE_NATIVE_FUNCTION(CSSNamespace::supports)
{
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "CSS.supports");
        return {};
    }

    if (vm.argument_count() >= 2) {
        // When the supports(property, value) method is invoked with two arguments property and value:
        //     If property is an ASCII case-insensitive match for any defined CSS property that the UA supports, and value successfully parses according to that property’s grammar, return true.
        //
        //     Otherwise, if property is a custom property name string, return true.
        //
        //     Otherwise, return false.
        return JS::Value(false);
    } else {
        // When the supports(conditionText) method is invoked with a single conditionText argument:
        //
        //    If conditionText, parsed and evaluated as a <supports-condition>, would return true, return true.
        //
        //    Otherwise, If conditionText, wrapped in parentheses and then parsed and evaluated as a <supports-condition>, would return true, return true.
        //
        //    Otherwise, return false.
        return JS::Value(false);
    }
}

}
