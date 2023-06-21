/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibWeb/Forward.h>
#include <LibWeb/PermissionsPolicy/Decision.h>

namespace Web::PermissionsPolicy {

class AutoplayAllowlist {
public:
    static AutoplayAllowlist& the();

    Decision is_allowed_for_origin(DOM::Document const&, HTML::Origin const&) const;

    void enable_globally();
    ErrorOr<void> enable_for_origins(ReadonlySpan<String>);

private:
    AutoplayAllowlist();
    ~AutoplayAllowlist();

    using Patterns = Vector<HTML::Origin>;
    struct Global { };

    Optional<Variant<Patterns, Global>> m_allowlist;
};

}
