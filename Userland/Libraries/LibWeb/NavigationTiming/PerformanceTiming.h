/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/StdLibExtras.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::NavigationTiming {

class PerformanceTiming final : public Bindings::Wrappable {
public:
    using WrapperType = Bindings::PerformanceTimingWrapper;
    using AllowOwnPtr = AK::TrueType;

    explicit PerformanceTiming(DOM::Window&);
    ~PerformanceTiming();

    void ref();
    void unref();

    u32 navigation_start() { return 0; }
    u32 unload_event_start() { return 0; }
    u32 unload_event_end() { return 0; }
    u32 redirect_start() { return 0; }
    u32 redirect_end() { return 0; }
    u32 fetch_start() { return 0; }
    u32 domain_lookup_start() { return 0; }
    u32 domain_lookup_end() { return 0; }
    u32 connect_start() { return 0; }
    u32 connect_end() { return 0; }
    u32 secure_connection_start() { return 0; }
    u32 request_start() { return 0; }
    u32 response_start() { return 0; }
    u32 response_end() { return 0; }
    u32 dom_loading() { return 0; }
    u32 dom_interactive() { return 0; }
    u32 dom_content_loaded_event_start() { return 0; }
    u32 dom_content_loaded_event_end() { return 0; }
    u32 dom_complete() { return 0; }
    u32 load_event_start() { return 0; }
    u32 load_event_end() { return 0; }

private:
    DOM::Window& m_window;
};

}
