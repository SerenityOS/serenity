/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Completion.h>
#include <LibWeb/Bindings/CSSStyleDeclarationWrapper.h>
#include <LibWeb/DOM/Element.h>

namespace Web::Bindings {

static CSS::PropertyID property_id_from_name(StringView name)
{
    // FIXME: Perhaps this should go in the code generator.
    if (name == "cssFloat"sv)
        return CSS::PropertyID::Float;

    if (auto property_id = CSS::property_id_from_camel_case_string(name); property_id != CSS::PropertyID::Invalid)
        return property_id;

    if (auto property_id = CSS::property_id_from_string(name); property_id != CSS::PropertyID::Invalid)
        return property_id;

    return CSS::PropertyID::Invalid;
}

JS::ThrowCompletionOr<bool> CSSStyleDeclarationWrapper::internal_has_property(JS::PropertyKey const& name) const
{
    if (!name.is_string())
        return Base::internal_has_property(name);
    return property_id_from_name(name.to_string()) != CSS::PropertyID::Invalid;
}

JS::ThrowCompletionOr<JS::Value> CSSStyleDeclarationWrapper::internal_get(JS::PropertyKey const& name, JS::Value receiver) const
{
    if (!name.is_string())
        return Base::internal_get(name, receiver);
    auto property_id = property_id_from_name(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::internal_get(name, receiver);
    if (auto maybe_property = impl().property(property_id); maybe_property.has_value())
        return { js_string(vm(), maybe_property->value->to_string()) };
    return { js_string(vm(), String::empty()) };
}

JS::ThrowCompletionOr<bool> CSSStyleDeclarationWrapper::internal_set(JS::PropertyKey const& name, JS::Value value, JS::Value receiver)
{
    if (!name.is_string())
        return Base::internal_set(name, value, receiver);
    auto property_id = property_id_from_name(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::internal_set(name, value, receiver);

    auto css_text = TRY(value.to_string(global_object()));

    impl().set_property(property_id, css_text);
    return true;
}

}
