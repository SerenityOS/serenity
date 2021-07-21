/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDatePrototype.h>

namespace JS::Temporal {

// 3.3 Properties of the Temporal.PlainDate Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaindate-prototype-object
PlainDatePrototype::PlainDatePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void PlainDatePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 3.3.2 Temporal.PlainDate.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.PlainDate"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

static PlainDate* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<PlainDate>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.PlainDate");
        return {};
    }
    return static_cast<PlainDate*>(this_object);
}

// 3.3.3 get Temporal.PlainDate.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::calendar_getter)
{
    // 1. Let temporalDate be the this value.
    // Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return temporalDate.[[Calendar]].
    return Value(&temporal_date->calendar());
}

// 3.3.25 Temporal.PlainDate.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.equals
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::equals)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Set other to ? ToTemporalDate(other).
    auto* other = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. If temporalDate.[[ISOYear]] ≠ other.[[ISOYear]], return false.
    if (temporal_date->iso_year() != other->iso_year())
        return Value(false);
    // 5. If temporalDate.[[ISOMonth]] ≠ other.[[ISOMonth]], return false.
    if (temporal_date->iso_month() != other->iso_month())
        return Value(false);
    // 6. If temporalDate.[[ISODay]] ≠ other.[[ISODay]], return false.
    if (temporal_date->iso_day() != other->iso_day())
        return Value(false);
    // 7. Return ? CalendarEquals(temporalDate.[[Calendar]], other.[[Calendar]]).
    return Value(calendar_equals(global_object, temporal_date->calendar(), other->calendar()));
}

// 3.3.31 Temporal.PlainDate.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainDate", "a primitive value");
    return {};
}

}
