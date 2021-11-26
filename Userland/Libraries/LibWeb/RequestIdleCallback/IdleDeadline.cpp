/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/RequestIdleCallback/IdleDeadline.h>

namespace Web::RequestIdleCallback {

NonnullRefPtr<IdleDeadline> IdleDeadline::create(double time_remaining, bool did_timeout)
{
    return adopt_ref(*new IdleDeadline(time_remaining, did_timeout));
}

IdleDeadline::IdleDeadline(double time_remaining, bool did_timeout)
    : m_time_remaining(time_remaining)
    , m_did_timeout(did_timeout)
{
}

IdleDeadline::~IdleDeadline()
{
}

}
