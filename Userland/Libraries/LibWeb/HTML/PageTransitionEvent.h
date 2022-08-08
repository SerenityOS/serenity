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
    JS_OBJECT(PageTransitionEvent, DOM::Event);

public:
    static PageTransitionEvent* create(Bindings::WindowObject&, FlyString const& event_name, PageTransitionEventInit const& event_init);
    static PageTransitionEvent* create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, PageTransitionEventInit const& event_init);

    PageTransitionEvent(Bindings::WindowObject&, FlyString const& event_name, PageTransitionEventInit const& event_init);

    virtual ~PageTransitionEvent() override;

    PageTransitionEvent& impl() { return *this; }

    bool persisted() const { return m_persisted; }

private:
    bool m_persisted { false };
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::HTML::PageTransitionEvent& object) { return &object; }
using PageTransitionEventWrapper = Web::HTML::PageTransitionEvent;
}
