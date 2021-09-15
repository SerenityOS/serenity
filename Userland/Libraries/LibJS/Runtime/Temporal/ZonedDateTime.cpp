/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimeConstructor.h>

namespace JS::Temporal {

// 6 Temporal.ZonedDateTime Objects, https://tc39.es/proposal-temporal/#sec-temporal-zoneddatetime-objects
ZonedDateTime::ZonedDateTime(BigInt const& nanoseconds, Object& time_zone, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_nanoseconds(nanoseconds)
    , m_time_zone(time_zone)
    , m_calendar(calendar)
{
}

void ZonedDateTime::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_nanoseconds);
    visitor.visit(&m_time_zone);
    visitor.visit(&m_calendar);
}

// 6.5.3 CreateTemporalZonedDateTime ( epochNanoseconds, timeZone, calendar [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalzoneddatetime
ZonedDateTime* create_temporal_zoned_date_time(GlobalObject& global_object, BigInt const& epoch_nanoseconds, Object& time_zone, Object& calendar, FunctionObject const* new_target)
{
    // 1. Assert: Type(epochNanoseconds) is BigInt.
    // 3. Assert: Type(timeZone) is Object.
    // 4. Assert: Type(calendar) is Object.

    // 2. Assert: ! IsValidEpochNanoseconds(epochNanoseconds) is true.
    VERIFY(is_valid_epoch_nanoseconds(epoch_nanoseconds));

    // 5. If newTarget is not present, set it to %Temporal.ZonedDateTime%.
    if (!new_target)
        new_target = global_object.temporal_zoned_date_time_constructor();

    // 6. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.ZonedDateTime.prototype%", « [[InitializedTemporalZonedDateTime]], [[Nanoseconds]], [[TimeZone]], [[Calendar]] »).
    // 7. Set object.[[Nanoseconds]] to epochNanoseconds.
    // 8. Set object.[[TimeZone]] to timeZone.
    // 9. Set object.[[Calendar]] to calendar.
    auto* object = TRY_OR_DISCARD(ordinary_create_from_constructor<ZonedDateTime>(global_object, *new_target, &GlobalObject::temporal_time_zone_prototype, epoch_nanoseconds, time_zone, calendar));

    // 10. Return object.
    return object;
}

}
