/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainMonthDayPrototype.h>

namespace JS::Temporal {

// 10.3 Properties of the Temporal.PlainMonthDay Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plainmonthday-prototype-object
PlainMonthDayPrototype::PlainMonthDayPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void PlainMonthDayPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
}

}
