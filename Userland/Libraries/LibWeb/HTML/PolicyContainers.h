/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/origin.html#policy-container
// A policy container is a struct containing policies that apply to a Document, a WorkerGlobalScope, or a WorkletGlobalScope. It has the following items:
struct PolicyContainer {
    // https://html.spec.whatwg.org/multipage/origin.html#policy-container-csp-list
    // FIXME: A CSP list, which is a CSP list. It is initially empty.

    // https://html.spec.whatwg.org/multipage/origin.html#policy-container-embedder-policy
    // FIXME: An embedder policy, which is an embedder policy. It is initially a new embedder policy.

    // https://html.spec.whatwg.org/multipage/origin.html#policy-container-referrer-policy
    // FIXME: A referrer policy, which is a referrer policy. It is initially the default referrer policy.
};

}
