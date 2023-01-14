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
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::Bindings {

CSSNamespace::CSSNamespace(JS::Realm& realm)
    : JS::Object(ConstructWithPrototypeTag::Tag, *realm.intrinsics().object_prototype())
{
}

void CSSNamespace::initialize(JS::Realm& realm)
{
    Object::initialize(realm);
    u8 attr = JS::Attribute::Enumerable;
    define_native_function(realm, "escape", escape, 1, attr);
    define_native_function(realm, "supports", supports, 2, attr);
}

// https://www.w3.org/TR/cssom-1/#dom-css-escape
JS_DEFINE_NATIVE_FUNCTION(CSSNamespace::escape)
{
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountAtLeastOne, "CSS.escape");

    auto identifier = TRY(vm.argument(0).to_deprecated_string(vm));
    return JS::PrimitiveString::create(vm, Web::CSS::serialize_an_identifier(identifier));
}

// https://www.w3.org/TR/css-conditional-3/#dom-css-supports
JS_DEFINE_NATIVE_FUNCTION(CSSNamespace::supports)
{
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountAtLeastOne, "CSS.supports");

    if (vm.argument_count() >= 2) {
        // When the supports(property, value) method is invoked with two arguments property and value:
        auto property_name = TRY(vm.argument(0).to_deprecated_string(vm));

        // If property is an ASCII case-insensitive match for any defined CSS property that the UA supports,
        // and value successfully parses according to that property’s grammar, return true.
        auto property = CSS::property_id_from_string(property_name);
        if (property != CSS::PropertyID::Invalid) {
            auto value_string = TRY(vm.argument(1).to_deprecated_string(vm));
            if (parse_css_value({}, value_string, property))
                return JS::Value(true);
        }
        // Otherwise, if property is a custom property name string, return true.
        // FIXME: This check is not enough to make sure this is a valid custom property name, but it's close enough.
        else if (property_name.starts_with("--"sv) && property_name.length() >= 3) {
            return JS::Value(true);
        }

        // Otherwise, return false.
        return JS::Value(false);
    } else {
        // When the supports(conditionText) method is invoked with a single conditionText argument:
        auto supports_text = TRY(vm.argument(0).to_deprecated_string(vm));

        // If conditionText, parsed and evaluated as a <supports-condition>, would return true, return true.
        if (auto supports = parse_css_supports({}, supports_text); supports && supports->matches())
            return JS::Value(true);

        // Otherwise, If conditionText, wrapped in parentheses and then parsed and evaluated as a <supports-condition>, would return true, return true.
        if (auto supports = parse_css_supports({}, DeprecatedString::formatted("({})", supports_text)); supports && supports->matches())
            return JS::Value(true);

        // Otherwise, return false.
        return JS::Value(false);
    }
}

}
