/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>

namespace Web::HighResolutionTime {

// https://w3c.github.io/hr-time/#dfn-coarsen-time
DOMHighResTimeStamp coarsen_time(DOMHighResTimeStamp timestamp, bool cross_origin_isolated_capability)
{
    // FIXME: Implement this.
    (void)cross_origin_isolated_capability;
    return timestamp;
}

// https://w3c.github.io/hr-time/#dfn-coarsened-shared-current-time
DOMHighResTimeStamp coarsened_shared_current_time(bool cross_origin_isolated_capability)
{
    // The coarsened shared current time given an optional boolean crossOriginIsolatedCapability (default false), must return the result of calling coarsen time with the unsafe shared current time and crossOriginIsolatedCapability.
    return coarsen_time(unsafe_shared_current_time(), cross_origin_isolated_capability);
}

// https://w3c.github.io/hr-time/#dfn-unsafe-shared-current-time
DOMHighResTimeStamp unsafe_shared_current_time()
{
    // The unsafe shared current time must return the current value of the shared monotonic clock.
    return Time::now_monotonic().to_nanoseconds() / 1e6;
}

}
