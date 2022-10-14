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
    static PageTransitionEvent* create(JS::Realm&, FlyString const& event_name, PageTransitionEventInit const& event_init);
    static PageTransitionEvent* construct_impl(JS::Realm&, FlyString const& event_name, PageTransitionEventInit const& event_init);

    PageTransitionEvent(JS::Realm&, FlyString const& event_name, PageTransitionEventInit const& event_init);

    virtual ~PageTransitionEvent() override;

    bool persisted() const { return m_persisted; }

private:
    bool m_persisted { false };
};

}
