/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

class PageTransitionEvent final : public DOM::Event {
public:
    using WrapperType = Bindings::PageTransitionEventWrapper;

    static NonnullRefPtr<PageTransitionEvent> create(FlyString event_name, bool persisted)
    {
        return adopt_ref(*new PageTransitionEvent(move(event_name), persisted));
    }

    virtual ~PageTransitionEvent() override = default;

    bool persisted() const { return m_persisted; }

protected:
    PageTransitionEvent(FlyString event_name, bool persisted)
        : DOM::Event(move(event_name))
        , m_persisted(persisted)
    {
    }

    bool m_persisted { false };
};

}
