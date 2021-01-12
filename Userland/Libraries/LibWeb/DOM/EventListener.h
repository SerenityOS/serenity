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

#include <AK/RefCounted.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::DOM {

class EventListener
    : public RefCounted<EventListener>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventListenerWrapper;

    explicit EventListener(JS::Handle<JS::Function> function)
        : m_function(move(function))
    {
    }

    JS::Function& function();

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
    JS::Handle<JS::Function> m_function;
    bool m_capture { false };
    bool m_passive { false };
    bool m_once { false };
    bool m_removed { false };
};

}
