/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>

namespace Web::HTML {

EventLoop::EventLoop()
{
}

EventLoop::~EventLoop()
{
}

EventLoop& main_thread_event_loop()
{
    return static_cast<Bindings::WebEngineCustomData*>(Bindings::main_thread_vm().custom_data())->event_loop;
}

}
