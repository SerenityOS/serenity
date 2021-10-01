/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::XHR {

// FIXME: All the "u32"s should be "u64"s, however LibJS doesn't currently support constructing values with u64,
//        and the IDL parser doesn't properly parse "unsigned long long".

struct ProgressEventInit : public DOM::EventInit {
    bool length_computable { false };
    u32 loaded { 0 };
    u32 total { 0 };
};

class ProgressEvent : public DOM::Event {
public:
    using WrapperType = Bindings::ProgressEventWrapper;

    static NonnullRefPtr<ProgressEvent> create(FlyString const& event_name, ProgressEventInit const& event_init)
    {
        return adopt_ref(*new ProgressEvent(event_name, event_init));
    }
    static NonnullRefPtr<ProgressEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, ProgressEventInit const& event_init)
    {
        return ProgressEvent::create(event_name, event_init);
    }

    virtual ~ProgressEvent() override { }

    bool length_computable() const { return m_length_computable; }
    u32 loaded() const { return m_loaded; }
    u32 total() const { return m_total; }

protected:
    ProgressEvent(FlyString const& event_name, ProgressEventInit const& event_init)
        : Event(event_name, event_init)
        , m_length_computable(event_init.length_computable)
        , m_loaded(event_init.loaded)
        , m_total(event_init.total)
    {
    }

    bool m_length_computable { false };
    u32 m_loaded { 0 };
    u32 m_total { 0 };
};

}
