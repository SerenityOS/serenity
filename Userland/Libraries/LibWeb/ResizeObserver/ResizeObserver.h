/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::ResizeObserver {

struct ResizeObserverOptions {
    Bindings::ResizeObserverBoxOptions box;
};

// https://drafts.csswg.org/resize-observer/#resize-observer-interface
class ResizeObserver : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(ResizeObserver, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<ResizeObserver>> construct_impl(JS::Realm&, WebIDL::CallbackType* callback);

    virtual ~ResizeObserver() override;

    void observe(DOM::Element& target, ResizeObserverOptions);
    void unobserve(DOM::Element& target);
    void disconnect();

private:
    explicit ResizeObserver(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
};

}
