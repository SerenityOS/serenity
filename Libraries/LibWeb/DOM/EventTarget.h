/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/Forward.h>

namespace Web {

class EventTarget {
    AK_MAKE_NONCOPYABLE(EventTarget);
    AK_MAKE_NONMOVABLE(EventTarget);

public:
    virtual ~EventTarget();

    void ref() { ref_event_target(); }
    void unref() { unref_event_target(); }

    void add_event_listener(String event_name, NonnullRefPtr<EventListener>);

    virtual void dispatch_event(String event_name) = 0;

    struct EventListenerRegistration {
        String event_name;
        NonnullRefPtr<EventListener> listener;
    };

    const Vector<EventListenerRegistration>& listeners() const { return m_listeners; }

protected:
    EventTarget();

    virtual void ref_event_target() = 0;
    virtual void unref_event_target() = 0;

private:
    Vector<EventListenerRegistration> m_listeners;
};

}
