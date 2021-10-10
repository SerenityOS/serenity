/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::ResizeObserver {

struct ResizeObserverOptions {
    String box;
};

// https://drafts.csswg.org/resize-observer/#resize-observer-interface
class ResizeObserver
    : public RefCounted<ResizeObserver>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::ResizeObserverWrapper;

    static NonnullRefPtr<ResizeObserver> create_with_global_object(JS::GlobalObject&, JS::Value callback);

    void observe(DOM::Element& target, ResizeObserverOptions);
    void unobserve(DOM::Element& target);
    void disconnect();
};

}
