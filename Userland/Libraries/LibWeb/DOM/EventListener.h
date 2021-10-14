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

namespace Web::DOM {

class EventListener
    : public RefCounted<EventListener>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventListenerWrapper;

    explicit EventListener(Bindings::CallbackType callback)
        : m_callback(move(callback))
    {
    }

    virtual ~EventListener() = default;

    Bindings::CallbackType& callback() { return m_callback; }

    const FlyString& type() const { return m_type; }
    void set_type(const FlyString& type) { m_type = type; }

    bool capture() const { return m_capture; }
    void set_capture(bool capture) { m_capture = capture; }

    bool passive() const { return m_passive; }
    void set_passive(bool passive) { m_capture = passive; }

    bool once() const { return m_once; }
    void set_once(bool once) { m_once = once; }

    bool removed() const { return m_removed; }
    void set_removed(bool removed) { m_removed = removed; }

private:
    FlyString m_type;
    Bindings::CallbackType m_callback;
    bool m_capture { false };
    bool m_passive { false };
    bool m_once { false };
    bool m_removed { false };
};

}
