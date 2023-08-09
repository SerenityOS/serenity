/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Console.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>

namespace Web::HTML {

void report_exception_to_console(JS::Value value, JS::Realm& realm, ErrorInPromise error_in_promise)
{
    auto& console = realm.intrinsics().console_object()->console();

    if (value.is_object()) {
        auto& object = value.as_object();
        auto& vm = object.vm();
        auto name = object.get_without_side_effects(vm.names.name).value_or(JS::js_undefined());
        auto message = object.get_without_side_effects(vm.names.message).value_or(JS::js_undefined());
        if (name.is_accessor() || message.is_accessor()) {
            // The result is not going to be useful, let's just print the value. This affects DOMExceptions, for example.
            dbgln("\033[31;1mUnhandled JavaScript exception{}:\033[0m {}", error_in_promise == ErrorInPromise::Yes ? " (in promise)" : "", JS::Value(&object));
        } else {
            dbgln("\033[31;1mUnhandled JavaScript exception{}:\033[0m [{}] {}", error_in_promise == ErrorInPromise::Yes ? " (in promise)" : "", name, message);
        }
        if (is<JS::Error>(object)) {
            auto const& error_value = static_cast<JS::Error const&>(object);
            for (auto& traceback_frame : error_value.traceback()) {
                auto& function_name = traceback_frame.function_name;
                auto& source_range = traceback_frame.source_range();
                dbgln("  {} at {}:{}:{}", function_name, source_range.filename(), source_range.start.line, source_range.start.column);
            }
            console.report_exception(error_value, error_in_promise == ErrorInPromise::Yes);

            return;
        }
    } else {
        dbgln("\033[31;1mUnhandled JavaScript exception{}:\033[0m {}", error_in_promise == ErrorInPromise::Yes ? " (in promise)" : "", value);
    }

    console.report_exception(*JS::Error::create(realm, value.to_string_without_side_effects()), error_in_promise == ErrorInPromise::Yes);
}

// https://html.spec.whatwg.org/#report-the-exception
void report_exception(JS::Completion const& throw_completion, JS::Realm& realm)
{
    VERIFY(throw_completion.type() == JS::Completion::Type::Throw);
    VERIFY(throw_completion.value().has_value());
    report_exception_to_console(*throw_completion.value(), realm, ErrorInPromise::No);
}

}
