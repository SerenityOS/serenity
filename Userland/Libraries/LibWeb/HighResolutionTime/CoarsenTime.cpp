/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HighResolutionTime/CoarsenTime.h>

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
    return coarsen_time(HTML::main_thread_event_loop().unsafe_shared_current_time(), cross_origin_isolated_capability);
}

}
