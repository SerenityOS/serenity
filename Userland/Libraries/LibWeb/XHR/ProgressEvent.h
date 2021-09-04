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

class ProgressEvent : public DOM::Event {
public:
    using WrapperType = Bindings::ProgressEventWrapper;

    static NonnullRefPtr<ProgressEvent> create(FlyString const& event_name, u32 transmitted, u32 length)
    {
        return adopt_ref(*new ProgressEvent(event_name, transmitted, length));
    }

    virtual ~ProgressEvent() override { }

    bool length_computable() const { return m_length_computable; }
    u32 loaded() const { return m_loaded; }
    u32 total() const { return m_total; }

protected:
    ProgressEvent(FlyString const& event_name, u32 transmitted, u32 length)
        : Event(event_name)
        , m_length_computable(length != 0)
        , m_loaded(transmitted)
        , m_total(length)
    {
    }

    bool m_length_computable { false };
    u32 m_loaded { 0 };
    u32 m_total { 0 };
};

}
