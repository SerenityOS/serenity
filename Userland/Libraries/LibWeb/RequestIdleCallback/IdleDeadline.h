/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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

    static NonnullRefPtr<IdleDeadline> create(bool did_timeout = false);
    virtual ~IdleDeadline() override;

    double time_remaining() const;
    bool did_timeout() const { return m_did_timeout; }

private:
    IdleDeadline(bool did_timeout);

    bool m_did_timeout { false };
};

}
