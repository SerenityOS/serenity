/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Window.h>

namespace Web::UIEvents {

class UIEvent : public DOM::Event {
public:
    using WrapperType = Bindings::UIEventWrapper;

    virtual ~UIEvent() override { }

    DOM::Window const* view() const { return m_window; }
    int detail() const { return m_detail; }

protected:
    explicit UIEvent(const FlyString& event_name)
        : Event(event_name)
    {
    }

    RefPtr<DOM::Window> m_window;
    int m_detail { 0 };
};

}
