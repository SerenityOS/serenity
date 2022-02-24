/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
    Optional<NonnullRefPtr<AbortSignal>> signal;
};

class IDLEventListener
    : public RefCounted<IDLEventListener>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventListenerWrapper;

    explicit IDLEventListener(Bindings::CallbackType callback)
        : m_callback(move(callback))
    {
    }

    virtual ~IDLEventListener() = default;

    Bindings::CallbackType& callback() { return m_callback; }

private:
    Bindings::CallbackType m_callback;
};

}
