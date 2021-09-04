/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/SubmitEvent.h>

namespace Web::HTML {

NonnullRefPtr<SubmitEvent> SubmitEvent::create(FlyString const& event_name, RefPtr<HTMLElement> submitter)
{
    return adopt_ref(*new SubmitEvent(event_name, move(submitter)));
}

SubmitEvent::SubmitEvent(FlyString const& event_name, RefPtr<HTMLElement> submitter)
    : DOM::Event(event_name)
    , m_submitter(submitter)
{
}

SubmitEvent::~SubmitEvent()
{
}

RefPtr<HTMLElement> SubmitEvent::submitter() const
{
    return m_submitter;
}

}
