/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

struct MessageEventInit : public DOM::EventInit {
    JS::Value data { JS::js_null() };
    String origin { "" };
    String last_event_id { "" };
};

class MessageEvent : public DOM::Event {
public:
    using WrapperType = Bindings::MessageEventWrapper;

    static NonnullRefPtr<MessageEvent> create(FlyString const& event_name, MessageEventInit const& event_init = {})
    {
        return adopt_ref(*new MessageEvent(event_name, event_init));
    }
    static NonnullRefPtr<MessageEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, MessageEventInit const& event_init)
    {
        return MessageEvent::create(event_name, event_init);
    }

    virtual ~MessageEvent() override = default;

    JS::Value data() const { return m_data; }
    String const& origin() const { return m_origin; }
    String const& last_event_id() const { return m_last_event_id; }

protected:
    MessageEvent(FlyString const& event_name, MessageEventInit const& event_init)
        : DOM::Event(event_name, event_init)
        , m_data(event_init.data)
        , m_origin(event_init.origin)
        , m_last_event_id(event_init.last_event_id)
    {
    }

    JS::Value m_data;
    String m_origin;
    String m_last_event_id;
};

}
