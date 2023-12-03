/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::Internals {

class Internals final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Internals, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(Internals);

public:
    virtual ~Internals() override;

    void signal_text_test_is_done();

    void gc();
    JS::Object* hit_test(double x, double y);

    void send_text(HTML::HTMLElement&, String const&);
    void commit_text();

    void click(double x, double y);

    WebIDL::ExceptionOr<bool> dispatch_user_activated_event(DOM::EventTarget&, DOM::Event& event);

private:
    explicit Internals(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
};

}
