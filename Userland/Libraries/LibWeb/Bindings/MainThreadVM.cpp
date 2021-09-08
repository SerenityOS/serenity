/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>

namespace Web::Bindings {

JS::VM& main_thread_vm()
{
    static RefPtr<JS::VM> vm;
    if (!vm) {
        vm = JS::VM::create(make<WebEngineCustomData>());
        static_cast<WebEngineCustomData*>(vm->custom_data())->event_loop.set_vm(*vm);
    }
    return *vm;
}

}
