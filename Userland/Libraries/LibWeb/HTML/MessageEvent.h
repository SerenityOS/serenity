/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
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
    WEB_PLATFORM_OBJECT(MessageEvent, DOM::Event);

public:
    static MessageEvent* create(HTML::Window&, FlyString const& event_name, MessageEventInit const& event_init = {});
    static MessageEvent* create_with_global_object(HTML::Window&, FlyString const& event_name, MessageEventInit const& event_init);

    MessageEvent(HTML::Window&, FlyString const& event_name, MessageEventInit const& event_init);
    virtual ~MessageEvent() override;

    JS::Value data() const { return m_data; }
    String const& origin() const { return m_origin; }
    String const& last_event_id() const { return m_last_event_id; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    JS::Value m_data;
    String m_origin;
    String m_last_event_id;
};

}
