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
    JS_OBJECT(SubmitEvent, DOM::Event);

public:
    static SubmitEvent* create(Bindings::WindowObject&, FlyString const& event_name, SubmitEventInit const& event_init);
    static SubmitEvent* create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, SubmitEventInit const& event_init);

    virtual ~SubmitEvent() override;

    SubmitEvent(Bindings::WindowObject&, FlyString const& event_name, SubmitEventInit const& event_init);

    SubmitEvent& impl() { return *this; }

    RefPtr<HTMLElement> submitter() const { return m_submitter; }

private:
    RefPtr<HTMLElement> m_submitter;
};

}
