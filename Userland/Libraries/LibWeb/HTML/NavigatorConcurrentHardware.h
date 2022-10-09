/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::HTML {

class NavigatorConcurrentHardwareMixin {
public:
    // https://html.spec.whatwg.org/multipage/workers.html#dom-navigator-hardwareconcurrency
    unsigned long long hardware_concurrency() { return 1; }
};

}
