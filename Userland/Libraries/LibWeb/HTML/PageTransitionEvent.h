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
    WEB_PLATFORM_OBJECT(PageTransitionEvent, DOM::Event);

public:
    static PageTransitionEvent* create(HTML::Window&, FlyString const& event_name, PageTransitionEventInit const& event_init);
    static PageTransitionEvent* create_with_global_object(HTML::Window&, FlyString const& event_name, PageTransitionEventInit const& event_init);

    PageTransitionEvent(HTML::Window&, FlyString const& event_name, PageTransitionEventInit const& event_init);

    virtual ~PageTransitionEvent() override;

    bool persisted() const { return m_persisted; }

private:
    bool m_persisted { false };
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::HTML::PageTransitionEvent& object) { return &object; }
using PageTransitionEventWrapper = Web::HTML::PageTransitionEvent;
}
