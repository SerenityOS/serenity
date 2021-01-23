/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <LibWeb/DOM/Event.h>

namespace Web::XHR {

// FIXME: All the "u32"s should be "u64"s, however LibJS doesn't currently support constructing values with u64,
//        and the IDL parser doesn't properly parse "unsigned long long".

class ProgressEvent : public DOM::Event {
public:
    using WrapperType = Bindings::ProgressEventWrapper;

    static NonnullRefPtr<ProgressEvent> create(const FlyString& event_name, u32 transmitted, u32 length)
    {
        return adopt(*new ProgressEvent(event_name, transmitted, length));
    }

    virtual ~ProgressEvent() override { }

    bool length_computable() const { return m_length_computable; }
    u32 loaded() const { return m_loaded; }
    u32 total() const { return m_total; }

protected:
    ProgressEvent(const FlyString& event_name, u32 transmitted, u32 length)
        : Event(event_name)
        , m_length_computable(length != 0)
        , m_loaded(transmitted)
        , m_total(length)
    {
    }

    bool m_length_computable { false };
    u32 m_loaded { 0 };
    u32 m_total { 0 };
};

}
