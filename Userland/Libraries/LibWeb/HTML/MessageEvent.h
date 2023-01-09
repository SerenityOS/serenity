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
    DeprecatedString origin { "" };
    DeprecatedString last_event_id { "" };
};

class MessageEvent : public DOM::Event {
    WEB_PLATFORM_OBJECT(MessageEvent, DOM::Event);

public:
    static MessageEvent* create(JS::Realm&, DeprecatedFlyString const& event_name, MessageEventInit const& event_init = {});
    static MessageEvent* construct_impl(JS::Realm&, DeprecatedFlyString const& event_name, MessageEventInit const& event_init);

    MessageEvent(JS::Realm&, DeprecatedFlyString const& event_name, MessageEventInit const& event_init);
    virtual ~MessageEvent() override;

    JS::Value data() const { return m_data; }
    DeprecatedString const& origin() const { return m_origin; }
    DeprecatedString const& last_event_id() const { return m_last_event_id; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    JS::Value m_data;
    DeprecatedString m_origin;
    DeprecatedString m_last_event_id;
};

}
