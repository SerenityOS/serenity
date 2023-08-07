/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/DOM/Event.h>

namespace Web::XHR {

// FIXME: All the "u32"s should be "u64"s, however LibJS doesn't currently support constructing values with u64,
//        and the IDL parser doesn't properly parse "unsigned long long".

struct ProgressEventInit : public DOM::EventInit {
    bool length_computable { false };
    u32 loaded { 0 };
    u32 total { 0 };
};

class ProgressEvent final : public DOM::Event {
    WEB_PLATFORM_OBJECT(ProgressEvent, DOM::Event);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<ProgressEvent>> create(JS::Realm&, FlyString const& event_name, ProgressEventInit const& event_init);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<ProgressEvent>> construct_impl(JS::Realm&, FlyString const& event_name, ProgressEventInit const& event_init);

    virtual ~ProgressEvent() override;

    bool length_computable() const { return m_length_computable; }
    u64 loaded() const { return m_loaded; }
    u64 total() const { return m_total; }

private:
    ProgressEvent(JS::Realm&, FlyString const& event_name, ProgressEventInit const& event_init);

    virtual void initialize(JS::Realm&) override;

    bool m_length_computable { false };
    u64 m_loaded { 0 };
    u64 m_total { 0 };
};

}
