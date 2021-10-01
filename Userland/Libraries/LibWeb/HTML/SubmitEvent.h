/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/HTMLElementWrapper.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

struct SubmitEventInit : public DOM::EventInit {
    RefPtr<HTMLElement> submitter { nullptr };
};

class SubmitEvent final : public DOM::Event {
public:
    using WrapperType = Bindings::SubmitEventWrapper;

    static NonnullRefPtr<SubmitEvent> create(FlyString const& event_name, SubmitEventInit const& event_init)
    {
        return adopt_ref(*new SubmitEvent(event_name, event_init));
    }
    static NonnullRefPtr<SubmitEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, SubmitEventInit const& event_init)
    {
        return SubmitEvent::create(event_name, event_init);
    }

    virtual ~SubmitEvent() override = default;

    RefPtr<HTMLElement> submitter() const { return m_submitter; }

private:
    SubmitEvent(FlyString const& event_name, SubmitEventInit const& event_init)
        : DOM::Event(event_name, event_init)
        , m_submitter(event_init.submitter)
    {
    }

    RefPtr<HTMLElement> m_submitter;
};

}
