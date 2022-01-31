/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Intl/DateTimeFormatFunction.h>

namespace JS::Intl {

// 11.1.6 DateTime Format Functions, https://tc39.es/ecma402/#sec-datetime-format-functions
DateTimeFormatFunction* DateTimeFormatFunction::create(GlobalObject& global_object, DateTimeFormat& date_time_format)
{
    return global_object.heap().allocate<DateTimeFormatFunction>(global_object, date_time_format, *global_object.function_prototype());
}

DateTimeFormatFunction::DateTimeFormatFunction(DateTimeFormat& date_time_format, Object& prototype)
    : NativeFunction(prototype)
    , m_date_time_format(date_time_format)
{
}

void DateTimeFormatFunction::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();

    Base::initialize(global_object);
    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
    define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);
}

ThrowCompletionOr<Value> DateTimeFormatFunction::call()
{
    auto& global_object = this->global_object();
    auto& vm = global_object.vm();

    auto date = vm.argument(0);

    // 1. Let dtf be F.[[DateTimeFormat]].
    // 2. Assert: Type(dtf) is Object and dtf has an [[InitializedDateTimeFormat]] internal slot.

    // 3. If date is not provided or is undefined, then
    if (date.is_undefined()) {
        // a. Let x be Call(%Date.now%, undefined).
        date = MUST(JS::call(global_object, global_object.date_constructor_now_function(), js_undefined()));
    }
    // 4. Else,
    else {
        // a. Let x be ? ToNumber(date).
        date = TRY(date.to_number(global_object));
    }

    // 5. Return ? FormatDateTime(dtf, x).
    auto formatted = TRY(format_date_time(global_object, m_date_time_format, date));
    return js_string(vm, move(formatted));
}

void DateTimeFormatFunction::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_date_time_format);
}

}
