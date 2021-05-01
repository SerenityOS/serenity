/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

class MessageEvent : public DOM::Event {
public:
    using WrapperType = Bindings::MessageEventWrapper;

    static NonnullRefPtr<MessageEvent> create(const FlyString& event_name, const String& data, const String& origin)
    {
        return adopt_ref(*new MessageEvent(event_name, data, origin));
    }

    virtual ~MessageEvent() override = default;

    const String& data() const { return m_data; }
    const String& origin() const { return m_origin; }

protected:
    MessageEvent(const FlyString& event_name, const String& data, const String& origin)
        : DOM::Event(event_name)
        , m_data(data)
        , m_origin(origin)
    {
    }

    String m_data;
    String m_origin;
};

}
