/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Now.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>

namespace JS::Temporal {

// 2 The Temporal.now Object, https://tc39.es/proposal-temporal/#sec-temporal-now-object
Now::Now(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Now::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.timeZone, time_zone, 0, attr);
}

// 2.1.1 Temporal.now.timeZone ( ), https://tc39.es/proposal-temporal/#sec-temporal.now.timezone
JS_DEFINE_NATIVE_FUNCTION(Now::time_zone)
{
    // 1. Return ? SystemTimeZone().
    return system_time_zone(global_object);
}

// 2.2.1 SystemTimeZone ( ), https://tc39.es/proposal-temporal/#sec-temporal-systemtimezone
Object* system_time_zone(GlobalObject& global_object)
{
    // 1. Let identifier be ! DefaultTimeZone().
    auto identifier = default_time_zone();

    // 2. Return ? CreateTemporalTimeZone(identifier).
    return create_temporal_time_zone(global_object, identifier);
}

}
