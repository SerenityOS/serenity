/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

struct PageTransitionEventInit : public DOM::EventInit {
    bool persisted { false };
};

class PageTransitionEvent final : public DOM::Event {
public:
    using WrapperType = Bindings::PageTransitionEventWrapper;

    static NonnullRefPtr<PageTransitionEvent> create(FlyString const& event_name, PageTransitionEventInit const& event_init)
    {
        return adopt_ref(*new PageTransitionEvent(event_name, event_init));
    }
    static NonnullRefPtr<PageTransitionEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, PageTransitionEventInit const& event_init)
    {
        return PageTransitionEvent::create(event_name, event_init);
    }

    virtual ~PageTransitionEvent() override = default;

    bool persisted() const { return m_persisted; }

protected:
    PageTransitionEvent(FlyString const& event_name, PageTransitionEventInit const& event_init)
        : DOM::Event(event_name, event_init)
        , m_persisted(event_init.persisted)
    {
    }

    bool m_persisted { false };
};

}
