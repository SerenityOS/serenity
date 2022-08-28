/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/AbortSignal.h>

namespace Web::DOM {

// NOTE: Even though these dictionaries are defined in EventTarget.idl, they are here to prevent a circular include between EventTarget.h and AbortSignal.h.
struct EventListenerOptions {
    bool capture { false };
};

struct AddEventListenerOptions : public EventListenerOptions {
    bool passive { false };
    bool once { false };
    Optional<JS::NonnullGCPtr<AbortSignal>> signal;
};

class IDLEventListener final : public JS::Object {
    JS_OBJECT(IDLEventListener, JS::Object);

public:
    static JS::NonnullGCPtr<IDLEventListener> create(JS::Realm&, JS::NonnullGCPtr<Bindings::CallbackType>);
    IDLEventListener(JS::Realm&, JS::NonnullGCPtr<Bindings::CallbackType>);

    virtual ~IDLEventListener() = default;

    Bindings::CallbackType& callback() { return *m_callback; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<Bindings::CallbackType> m_callback;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::DOM::IDLEventListener& object) { return &object; }
using EventListenerWrapper = Web::DOM::IDLEventListener;
}
