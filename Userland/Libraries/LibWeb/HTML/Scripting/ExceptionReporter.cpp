/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/#report-the-exception
void report_exception(JS::Completion const& throw_completion)
{
    // FIXME: This is just old code, and does not strictly follow the spec of report an exception.
    // FIXME: We should probably also report these exceptions to the JS console.
    VERIFY(throw_completion.type() == JS::Completion::Type::Throw);
    VERIFY(throw_completion.value().has_value());
    auto thrown_value = *throw_completion.value();
    if (thrown_value.is_object()) {
        auto& object = thrown_value.as_object();
        auto& vm = object.vm();
        auto name = object.get_without_side_effects(vm.names.name).value_or(JS::js_undefined());
        auto message = object.get_without_side_effects(vm.names.message).value_or(JS::js_undefined());
        if (name.is_accessor() || message.is_accessor()) {
            // The result is not going to be useful, let's just print the value. This affects DOMExceptions, for example.
            dbgln("\033[31;1mUnhandled JavaScript exception:\033[0m {}", thrown_value);
        } else {
            dbgln("\033[31;1mUnhandled JavaScript exception:\033[0m [{}] {}", name, message);
        }
        if (is<JS::Error>(object)) {
            auto const& error_value = static_cast<JS::Error const&>(object);
            for (auto const& traceback_frame : error_value.traceback()) {
                auto const& function_name = traceback_frame.function_name;
                auto const& source_range = traceback_frame.source_range;
                dbgln("  {} at {}:{}:{}", function_name, source_range.filename, source_range.start.line, source_range.start.column);
            }
        }
    } else {
        dbgln("\033[31;1mUnhandled JavaScript exception:\033[0m {}", thrown_value);
    }
}

}
