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
    static JS::NonnullGCPtr<ResizeObserver> create_with_global_object(HTML::Window&, Bindings::CallbackType* callback);

    virtual ~ResizeObserver() override;

    void observe(DOM::Element& target, ResizeObserverOptions);
    void unobserve(DOM::Element& target);
    void disconnect();

private:
    explicit ResizeObserver(HTML::Window&);
};

}

WRAPPER_HACK(ResizeObserver, Web::ResizeObserver)
