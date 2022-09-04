/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::IntersectionObserver {

struct IntersectionObserverInit {
    Optional<Variant<JS::Handle<DOM::Element>, JS::Handle<DOM::Document>>> root;
    String root_margin { "0px"sv };
    Variant<double, Vector<double>> threshold { 0 };
};

// https://w3c.github.io/IntersectionObserver/#intersection-observer-interface
class IntersectionObserver : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(IntersectionObserver, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<IntersectionObserver> create_with_global_object(HTML::Window&, Bindings::CallbackType* callback, IntersectionObserverInit const& options = {});

    virtual ~IntersectionObserver() override;

    void observe(DOM::Element& target);
    void unobserve(DOM::Element& target);
    void disconnect();

private:
    explicit IntersectionObserver(HTML::Window&);
};

}

WRAPPER_HACK(IntersectionObserver, Web::IntersectionObserver)
