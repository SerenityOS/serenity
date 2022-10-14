/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginOpenerPolicy.h>
#include <LibWeb/HTML/Origin.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/origin.html#coop-enforcement-result
struct CrossOriginOpenerPolicyEnforcementResult {
    // A boolean needs a browsing context group switch, initially false.
    bool needs_a_browsing_context_group_switch { false };

    // A boolean would need a browsing context group switch due to report-only, initially false.
    bool would_need_a_browsing_context_group_switch_due_to_report_only { false };

    // A URL url.
    AK::URL url;

    // An origin origin.
    Origin origin;

    // A cross-origin opener policy cross-origin opener policy.
    CrossOriginOpenerPolicy cross_origin_opener_policy;

    // A boolean current context is navigation source.
    bool current_context_is_navigation_source { false };
};

}
