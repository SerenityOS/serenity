/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>

namespace Web::Bindings {

struct WebEngineCustomData final : public JS::VM::CustomData {
    virtual ~WebEngineCustomData() override { }

    HTML::EventLoop event_loop;
};

JS::VM& main_thread_vm();

}
