/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ThrowableStringBuilder.h>
#include <LibJS/SourceRange.h>

namespace JS {

SourceRange const& TracebackFrame::source_range() const
{
    if (auto* unrealized = source_range_storage.get_pointer<UnrealizedSourceRange>()) {
        auto source_range = [&] {
            if (!unrealized->source_code) {
                static auto dummy_source_code = SourceCode::create(String {}, String {});
                return SourceRange { dummy_source_code, {}, {} };
            }
            return unrealized->realize();
        }();
        source_range_storage = move(source_range);
    }
    return source_range_storage.get<SourceRange>();
}

NonnullGCPtr<Error> Error::create(Realm& realm)
{
    return realm.heap().allocate<Error>(realm, realm.intrinsics().error_prototype()).release_allocated_value_but_fixme_should_propagate_errors();
}

NonnullGCPtr<Error> Error::create(Realm& realm, String message)
{
    auto& vm = realm.vm();
    auto error = Error::create(realm);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    error->define_direct_property(vm.names.message, PrimitiveString::create(vm, move(message)), attr);
    return error;
}

ThrowCompletionOr<NonnullGCPtr<Error>> Error::create(Realm& realm, StringView message)
{
    return create(realm, TRY_OR_THROW_OOM(realm.vm(), String::from_utf8(message)));
}

Error::Error(Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
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

        // b. Perform CreateNonEnumerableDataPropertyOrThrow(O, "cause", cause).
        create_non_enumerable_data_property_or_throw(vm.names.cause, cause);
    }

    // 2. Return unused.
    return {};
}

void Error::populate_stack()
{
    auto& vm = this->vm();
    m_traceback.ensure_capacity(vm.execution_context_stack().size());
    for (ssize_t i = vm.execution_context_stack().size() - 1; i >= 0; i--) {
        auto context = vm.execution_context_stack()[i];
        auto function_name = context->function_name;
        if (function_name.is_empty())
            function_name = "<unknown>"sv;

        TracebackFrame frame {
            .function_name = move(function_name),
            .source_range_storage = context->source_range,
        };

        m_traceback.append(move(frame));
    }
}

ThrowCompletionOr<String> Error::stack_string(VM& vm) const
{
    ThrowableStringBuilder stack_string_builder(vm);

    // Note: We roughly follow V8's formatting
    // Note: The error's name and message get prepended by ErrorPrototype::stack
    // Note: We don't want to capture the global execution context, so we omit the last frame
    // FIXME: We generate a stack-frame for the Errors constructor, other engines do not
    for (size_t i = 0; i < m_traceback.size() - 1; ++i) {
        auto const& frame = m_traceback[i];
        auto function_name = frame.function_name;
        auto source_range = frame.source_range();
        // Note: Since we don't know whether we have a valid SourceRange here we just check for some default values.
        if (!source_range.filename().is_empty() || source_range.start.offset != 0 || source_range.end.offset != 0) {

            if (function_name == "<unknown>"sv)
                MUST_OR_THROW_OOM(stack_string_builder.appendff("    at {}:{}:{}\n", source_range.filename(), source_range.start.line, source_range.start.column));
            else
                MUST_OR_THROW_OOM(stack_string_builder.appendff("    at {} ({}:{}:{})\n", function_name, source_range.filename(), source_range.start.line, source_range.start.column));
        } else {
            MUST_OR_THROW_OOM(stack_string_builder.appendff("    at {}\n", function_name.is_empty() ? "<unknown>"sv : function_name.view()));
        }
    }

    return stack_string_builder.to_string();
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType)                                                                         \
    NonnullGCPtr<ClassName> ClassName::create(Realm& realm)                                                                                                      \
    {                                                                                                                                                            \
        return realm.heap().allocate<ClassName>(realm, realm.intrinsics().snake_name##_prototype()).release_allocated_value_but_fixme_should_propagate_errors(); \
    }                                                                                                                                                            \
                                                                                                                                                                 \
    NonnullGCPtr<ClassName> ClassName::create(Realm& realm, String message)                                                                                      \
    {                                                                                                                                                            \
        auto& vm = realm.vm();                                                                                                                                   \
        auto error = ClassName::create(realm);                                                                                                                   \
        u8 attr = Attribute::Writable | Attribute::Configurable;                                                                                                 \
        error->define_direct_property(vm.names.message, PrimitiveString::create(vm, move(message)), attr);                                                       \
        return error;                                                                                                                                            \
    }                                                                                                                                                            \
                                                                                                                                                                 \
    ThrowCompletionOr<NonnullGCPtr<ClassName>> ClassName::create(Realm& realm, StringView message)                                                               \
    {                                                                                                                                                            \
        return create(realm, TRY_OR_THROW_OOM(realm.vm(), String::from_utf8(message)));                                                                          \
    }                                                                                                                                                            \
                                                                                                                                                                 \
    ClassName::ClassName(Object& prototype)                                                                                                                      \
        : Error(prototype)                                                                                                                                       \
    {                                                                                                                                                            \
    }

JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
