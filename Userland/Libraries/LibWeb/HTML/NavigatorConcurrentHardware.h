/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/WebIDL/Types.h>

namespace Web::HTML {

class NavigatorConcurrentHardwareMixin {
public:
    // https://html.spec.whatwg.org/multipage/workers.html#dom-navigator-hardwareconcurrency
    WebIDL::UnsignedLongLong hardware_concurrency() { return 1; }
};

}
