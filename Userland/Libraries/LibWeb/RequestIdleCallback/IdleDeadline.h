/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::RequestIdleCallback {

class IdleDeadline final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(IdleDeadline, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<IdleDeadline> create(HTML::Window&, bool did_timeout = false);
    virtual ~IdleDeadline() override;

    double time_remaining() const;
    bool did_timeout() const { return m_did_timeout; }

private:
    IdleDeadline(HTML::Window&, bool did_timeout);

    bool m_did_timeout { false };
};

}

WRAPPER_HACK(IdleDeadline, Web::RequestIdleCallback)
