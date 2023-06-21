/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/RemoteBrowsingContext.h>

namespace Web::HTML {

JS::NonnullGCPtr<RemoteBrowsingContext> RemoteBrowsingContext::create_a_new_remote_browsing_context(String handle)
{
    auto browsing_context = Bindings::main_thread_vm().heap().allocate_without_realm<RemoteBrowsingContext>(handle);
    return browsing_context;
};

HTML::WindowProxy* RemoteBrowsingContext::window_proxy()
{
    return nullptr;
}

HTML::WindowProxy const* RemoteBrowsingContext::window_proxy() const
{
    return nullptr;
}

RemoteBrowsingContext::RemoteBrowsingContext(String handle)
    : m_window_handle(handle) {};

}
