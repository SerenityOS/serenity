/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Origin.h>

namespace Web::CSP {

// https://w3c.github.io/webappsec-csp/#content-security-policy-object
class Policy {
public:
    // https://w3c.github.io/webappsec-csp/#policy-disposition
    enum class Disposition {
        Enforce,
        Report,
    };

    // https://w3c.github.io/webappsec-csp/#policy-source
    enum class Source {
        Header,
        Meta,
    };

private:
    // FIXME: Each policy has an associated directive set, which is an ordered set of directives that define the policy’s implications when applied.
    Disposition m_disposition { Disposition::Enforce }; // NOTE: The spec doesn't define a default value. This just initialises it to the first enum possible.
    Source m_source { Source::Header };                 // NOTE: The spec doesn't define a default value. This just initialises it to the first enum possible.
    Origin m_self_origin;
};

}
