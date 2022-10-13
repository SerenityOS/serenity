/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::ReferrerPolicy {

// https://w3c.github.io/webappsec-referrer-policy/#enumdef-referrerpolicy
enum class ReferrerPolicy {
    NoReferrer,
    NoReferrerWhenDowngrade,
    SameOrigin,
    Origin,
    StrictOrigin,
    OriginWhenCrossOrigin,
    StrictOriginWhenCrossOrigin,
    UnsafeURL,
};

// https://w3c.github.io/webappsec-referrer-policy/#default-referrer-policy
// The default referrer policy is "strict-origin-when-cross-origin".
constexpr auto DEFAULT_REFERRER_POLICY = ReferrerPolicy::StrictOriginWhenCrossOrigin;

}
