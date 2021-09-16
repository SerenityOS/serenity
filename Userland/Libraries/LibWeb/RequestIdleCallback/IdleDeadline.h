/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::RequestIdleCallback {

class IdleDeadline final
    : public RefCounted<IdleDeadline>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::IdleDeadlineWrapper;
    using AllowOwnPtr = TrueType;

    static NonnullRefPtr<IdleDeadline> create(double time_remaining, bool did_timeout);
    virtual ~IdleDeadline() override;

    double time_remaining() const { return m_time_remaining; }
    bool did_timeout() const { return m_did_timeout; }

private:
    IdleDeadline(double time_remaining, bool did_timeout);

    double m_time_remaining { 0 };
    bool m_did_timeout { false };
};

}
