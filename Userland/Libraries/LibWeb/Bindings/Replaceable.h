/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define REPLACEABLE_PROPERTY_SETTER(ObjectType, property)                                                        \
    auto this_value = vm.this_value(global_object);                                                              \
    if (!this_value.is_object() || !is<ObjectType>(this_value.as_object()))                                      \
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, #ObjectType); \
    TRY(this_value.as_object().internal_define_own_property(                                                     \
        #property, JS::PropertyDescriptor { .value = vm.argument(0), .writable = true }));                       \
    return JS::js_undefined();
