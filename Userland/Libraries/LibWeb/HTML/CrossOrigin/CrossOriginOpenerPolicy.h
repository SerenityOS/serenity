/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/origin.html#cross-origin-opener-policy-value
enum class CrossOriginOpenerPolicyValue {
    UnsafeNone,
    SameOriginAllowPopups,
    SameOrigin,
    SameOriginPlusCOEP,
};

// https://html.spec.whatwg.org/multipage/origin.html#cross-origin-opener-policy
struct CrossOriginOpenerPolicy {
    // A value, which is a cross-origin opener policy value, initially "unsafe-none".
    CrossOriginOpenerPolicyValue value { CrossOriginOpenerPolicyValue::UnsafeNone };

    // A reporting endpoint, which is string or null, initially null.
    Optional<String> reporting_endpoint;

    // A report-only value, which is a cross-origin opener policy value, initially "unsafe-none".
    CrossOriginOpenerPolicyValue report_only_value { CrossOriginOpenerPolicyValue::UnsafeNone };

    // A report-only reporting endpoint, which is a string or null, initially null.
    Optional<String> report_only_reporting_endpoint;
};

}
