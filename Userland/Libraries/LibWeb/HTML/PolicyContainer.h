/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/CSP/Policy.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/origin.html#policy-container
// NOTE: This is pretty bare, but the spec plans to move more stuff into this struct in the future.
struct PolicyContainer {
    Vector<CSP::Policy> csp_list;

    PolicyContainer() = default;

    // https://html.spec.whatwg.org/multipage/origin.html#clone-a-policy-container
    PolicyContainer(PolicyContainer const& other)
    {
        for (auto& policy : other.csp_list)
            csp_list.append(policy);
    }
};

}
