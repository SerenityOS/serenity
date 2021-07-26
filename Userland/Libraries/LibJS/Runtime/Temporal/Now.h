/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Now final : public Object {
    JS_OBJECT(Now, Object);

public:
    explicit Now(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~Now() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(time_zone);
    JS_DECLARE_NATIVE_FUNCTION(instant);
    JS_DECLARE_NATIVE_FUNCTION(plain_date);
};

TimeZone* system_time_zone(GlobalObject&);
BigInt* system_utc_epoch_nanoseconds(GlobalObject&);
Instant* system_instant(GlobalObject&);
PlainDateTime* system_date_time(GlobalObject&, Value temporal_time_zone_like, Value calendar_like);

}
