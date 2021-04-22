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
        vm = JS::VM::create();
        vm->set_should_log_exceptions(true);
    }
    return *vm;
}

}
