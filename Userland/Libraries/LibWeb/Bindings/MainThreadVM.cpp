/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Module.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>

namespace Web::Bindings {

JS::VM& main_thread_vm()
{
    static RefPtr<JS::VM> vm;
    if (!vm) {
        vm = JS::VM::create(make<WebEngineCustomData>());
        static_cast<WebEngineCustomData*>(vm->custom_data())->event_loop.set_vm(*vm);

        vm->host_resolve_imported_module = [&](JS::ScriptOrModule, JS::ModuleRequest const&) -> JS::ThrowCompletionOr<NonnullRefPtr<JS::Module>> {
            return vm->throw_completion<JS::InternalError>(vm->current_realm()->global_object(), JS::ErrorType::NotImplemented, "Modules in the browser");
        };
    }
    return *vm;
}

}
