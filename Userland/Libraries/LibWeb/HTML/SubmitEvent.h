/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

struct SubmitEventInit : public DOM::EventInit {
    JS::GCPtr<HTMLElement> submitter;
};

class SubmitEvent final : public DOM::Event {
    WEB_PLATFORM_OBJECT(SubmitEvent, DOM::Event);

public:
    static SubmitEvent* create(HTML::Window&, FlyString const& event_name, SubmitEventInit const& event_init);
    static SubmitEvent* create_with_global_object(HTML::Window&, FlyString const& event_name, SubmitEventInit const& event_init);

    virtual ~SubmitEvent() override;

    SubmitEvent(HTML::Window&, FlyString const& event_name, SubmitEventInit const& event_init);

    JS::GCPtr<HTMLElement> submitter() const { return m_submitter; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    JS::GCPtr<HTMLElement> m_submitter;
};

}
