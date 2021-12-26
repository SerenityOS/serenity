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

#include <LibWeb/Bindings/ScriptExecutionContext.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::DOM {

EventTarget::EventTarget(Bindings::ScriptExecutionContext& script_execution_context)
    : m_script_execution_context(&script_execution_context)
{
}

EventTarget::~EventTarget()
{
}

void EventTarget::add_event_listener(const FlyString& event_name, NonnullRefPtr<EventListener> listener)
{
    auto existing_listener = m_listeners.first_matching([&](auto& entry) {
        return entry.listener->type() == event_name && &entry.listener->function() == &listener->function() && entry.listener->capture() == listener->capture();
    });
    if (existing_listener.has_value())
        return;
    listener->set_type(event_name);
    m_listeners.append({ event_name, move(listener) });
}

void EventTarget::remove_event_listener(const FlyString& event_name, NonnullRefPtr<EventListener> listener)
{
    m_listeners.remove_first_matching([&](auto& entry) {
        auto matches = entry.event_name == event_name && &entry.listener->function() == &listener->function() && entry.listener->capture() == listener->capture();
        if (matches)
            entry.listener->set_removed(true);
        return matches;
    });
}

void EventTarget::remove_from_event_listener_list(NonnullRefPtr<EventListener> listener)
{
    m_listeners.remove_first_matching([&](auto& entry) {
        return entry.listener->type() == listener->type() && &entry.listener->function() == &listener->function() && entry.listener->capture() == listener->capture();
    });
}

}
