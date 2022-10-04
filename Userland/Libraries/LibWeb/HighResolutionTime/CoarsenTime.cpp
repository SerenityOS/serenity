/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HighResolutionTime/CoarsenTime.h>

namespace Web::HighResolutionTime {

// https://w3c.github.io/hr-time/#dfn-coarsen-time
DOMHighResTimeStamp coarsen_time(DOMHighResTimeStamp timestamp, bool cross_origin_isolated_capability)
{
    // FIXME: Implement this.
    (void)cross_origin_isolated_capability;
    return timestamp;
}

}
