/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/SourceRange.h>

namespace JS {

Error* Error::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<Error>(global_object, *global_object.error_prototype());
}

Error* Error::create(GlobalObject& global_object, String const& message)
{
    auto& vm = global_object.vm();
    auto* error = Error::create(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    error->define_direct_property(vm.names.message, js_string(vm, message), attr);
    return error;
}

Error::Error(Object& prototype)
    : Object(prototype)
{
    populate_stack();
}

// 20.5.8.1 InstallErrorCause ( O, options ), https://tc39.es/ecma262/#sec-installerrorcause
ThrowCompletionOr<void> Error::install_error_cause(Value options)
{
    auto& vm = this->vm();

    // 1. If Type(options) is Object and ? HasProperty(options, "cause") is true, then
    if (options.is_object() && TRY(options.as_object().has_property(vm.names.cause))) {
        // a. Let cause be ? Get(options, "cause").
        auto cause = TRY(options.as_object().get(vm.names.cause));

        // b. Perform ! CreateNonEnumerableDataPropertyOrThrow(O, "cause", cause).
        MUST(create_non_enumerable_data_property_or_throw(vm.names.cause, cause));
    }

    // 2. Return NormalCompletion(undefined).
    return {};
}

void Error::populate_stack()
{
    auto& vm = this->vm();
    m_traceback.ensure_capacity(vm.execution_context_stack().size());
    for (ssize_t i = vm.execution_context_stack().size() - 1; i >= 0; i--) {
        auto* context = vm.execution_context_stack()[i];
        auto function_name = context->function_name;
        if (function_name.is_empty())
            function_name = "<unknown>"sv;
        m_traceback.empend(
            move(function_name),
            // We might not have an AST node associated with the execution context, e.g. in promise
            // reaction jobs (which aren't called anywhere from the source code).
            // They're not going to generate any _unhandled_ exceptions though, so a meaningless
            // source range is fine.
            context->current_node ? context->current_node->source_range() : SourceRange {});
    }
}

String Error::stack_string() const
{
    StringBuilder stack_string_builder;
    // Note: We roughly follow V8's formatting
    // Note: The error's name and message get prepended by ErrorPrototype::stack
    // Note: We don't want to capture the global execution context, so we omit the last frame
    // FIXME: We generate a stack-frame for the Errors constructor, other engines do not
    for (size_t i = 0; i < m_traceback.size() - 1; ++i) {
        auto const& frame = m_traceback[i];
        auto function_name = frame.function_name;
        // Note: Since we don't know whether we have a valid SourceRange here we just check for some default values.
        if (!frame.source_range.filename.is_null() || frame.source_range.start.offset != 0 || frame.source_range.end.offset != 0) {

            if (function_name == "<unknown>"sv)
                stack_string_builder.appendff("    at {}:{}:{}\n", frame.source_range.filename, frame.source_range.start.line, frame.source_range.start.column);
            else
                stack_string_builder.appendff("    at {} ({}:{}:{})\n", function_name, frame.source_range.filename, frame.source_range.start.line, frame.source_range.start.column);
        } else {
            stack_string_builder.appendff("    at {}\n", function_name.is_empty() ? "<unknown>"sv : function_name.view());
        }
    }

    return stack_string_builder.build();
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType)                         \
    ClassName* ClassName::create(GlobalObject& global_object)                                                    \
    {                                                                                                            \
        return global_object.heap().allocate<ClassName>(global_object, *global_object.snake_name##_prototype()); \
    }                                                                                                            \
                                                                                                                 \
    ClassName* ClassName::create(GlobalObject& global_object, String const& message)                             \
    {                                                                                                            \
        auto& vm = global_object.vm();                                                                           \
        auto* error = ClassName::create(global_object);                                                          \
        u8 attr = Attribute::Writable | Attribute::Configurable;                                                 \
        error->define_direct_property(vm.names.message, js_string(vm, message), attr);                           \
        return error;                                                                                            \
    }                                                                                                            \
                                                                                                                 \
    ClassName::ClassName(Object& prototype)                                                                      \
        : Error(prototype)                                                                                       \
    {                                                                                                            \
    }

JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
