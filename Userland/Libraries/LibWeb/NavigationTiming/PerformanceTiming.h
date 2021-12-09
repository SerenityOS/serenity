/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCountForwarder.h>
#include <AK/StdLibExtras.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/Window.h>

namespace Web::NavigationTiming {

class PerformanceTiming final
    : public RefCountForwarder<DOM::Window>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::PerformanceTimingWrapper;
    using AllowOwnPtr = TrueType;

    explicit PerformanceTiming(DOM::Window&);
    ~PerformanceTiming();

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
};

}
