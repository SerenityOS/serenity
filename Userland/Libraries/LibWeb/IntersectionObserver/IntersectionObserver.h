/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::IntersectionObserver {

struct IntersectionObserverInit {
    DOM::Node* root { nullptr };
    String root_margin { "0px"sv };
    JS::Value threshold { 0 };
};

// https://w3c.github.io/IntersectionObserver/#intersection-observer-interface
class IntersectionObserver
    : public RefCounted<IntersectionObserver>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::IntersectionObserverWrapper;

    static NonnullRefPtr<IntersectionObserver> create_with_global_object(JS::GlobalObject&, JS::Value callback, IntersectionObserverInit const& options = {});

    void observe(DOM::Element& target);
    void unobserve(DOM::Element& target);
    void disconnect();
};

}
