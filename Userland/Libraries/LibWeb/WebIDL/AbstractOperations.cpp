/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/NumericLimits.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/WebIDL/AbstractOperations.h>

namespace Web::WebIDL {

// https://webidl.spec.whatwg.org/#dfn-get-buffer-source-copy
ErrorOr<ByteBuffer> get_buffer_source_copy(JS::Object const& buffer_source)
{
    // 1. Let esBufferSource be the result of converting bufferSource to an ECMAScript value.

    // 2. Let esArrayBuffer be esBufferSource.
    JS::ArrayBuffer* es_array_buffer;

    // 3. Let offset be 0.
    u32 offset = 0;

    // 4. Let length be 0.
    u32 length = 0;

    // 5. If esBufferSource has a [[ViewedArrayBuffer]] internal slot, then:
    if (is<JS::TypedArrayBase>(buffer_source)) {
        auto const& es_buffer_source = static_cast<JS::TypedArrayBase const&>(buffer_source);

        // 1. Set esArrayBuffer to esBufferSource.[[ViewedArrayBuffer]].
        es_array_buffer = es_buffer_source.viewed_array_buffer();

        // 2. Set offset to esBufferSource.[[ByteOffset]].
        offset = es_buffer_source.byte_offset();

        // 3. Set length to esBufferSource.[[ByteLength]].
        length = es_buffer_source.byte_length();
    } else if (is<JS::DataView>(buffer_source)) {
        auto const& es_buffer_source = static_cast<JS::DataView const&>(buffer_source);

        // 1. Set esArrayBuffer to esBufferSource.[[ViewedArrayBuffer]].
        es_array_buffer = es_buffer_source.viewed_array_buffer();

        // 2. Set offset to esBufferSource.[[ByteOffset]].
        offset = es_buffer_source.byte_offset();

        // 3. Set length to esBufferSource.[[ByteLength]].
        length = es_buffer_source.byte_length();
    }
    // 6. Otherwise:
    else {
        // 1. Assert: esBufferSource is an ArrayBuffer or SharedArrayBuffer object.
        auto const& es_buffer_source = static_cast<JS::ArrayBuffer const&>(buffer_source);
        es_array_buffer = &const_cast<JS ::ArrayBuffer&>(es_buffer_source);

        // 2. Set length to esBufferSource.[[ArrayBufferByteLength]].
        length = es_buffer_source.byte_length();
    }

    // 7. If ! IsDetachedBuffer(esArrayBuffer) is true, then return the empty byte sequence.
    if (es_array_buffer->is_detached())
        return ByteBuffer {};

    // 8. Let bytes be a new byte sequence of length equal to length.
    auto bytes = TRY(ByteBuffer::create_zeroed(length));

    // 9. For i in the range offset to offset + length − 1, inclusive, set bytes[i − offset] to ! GetValueFromBuffer(esArrayBuffer, i, Uint8, true, Unordered).
    for (u64 i = offset; i < offset + length; ++i) {
        auto value = es_array_buffer->get_value<u8>(i, true, JS::ArrayBuffer::Unordered);
        bytes[i - offset] = static_cast<u8>(value.as_double());
    }

    // 10. Return bytes.
    return bytes;
}

// https://webidl.spec.whatwg.org/#call-user-object-operation-return
inline JS::Completion clean_up_on_return(HTML::EnvironmentSettingsObject& stored_settings, HTML::EnvironmentSettingsObject& relevant_settings, JS::Completion& completion)
{
    // Return: at this point completion will be set to an ECMAScript completion value.

    // 1. Clean up after running a callback with stored settings.
    stored_settings.clean_up_after_running_callback();

    // 2. Clean up after running script with relevant settings.
    relevant_settings.clean_up_after_running_script();

    // 3. If completion is a normal completion, return completion.
    if (completion.type() == JS::Completion::Type::Normal)
        return completion;

    // 4. If completion is an abrupt completion and the operation has a return type that is not a promise type, return completion.
    // FIXME: This does not handle promises and thus always returns completion at this point.
    return completion;

    // FIXME: 5. Let rejectedPromise be ! Call(%Promise.reject%, %Promise%, «completion.[[Value]]»).

    // FIXME: 6. Return the result of converting rejectedPromise to the operation’s return type.
}

JS::Completion call_user_object_operation(WebIDL::CallbackType& callback, DeprecatedString const& operation_name, Optional<JS::Value> this_argument, JS::MarkedVector<JS::Value> args)
{
    // 1. Let completion be an uninitialized variable.
    JS::Completion completion;

    // 2. If thisArg was not given, let thisArg be undefined.
    if (!this_argument.has_value())
        this_argument = JS::js_undefined();

    // 3. Let O be the ECMAScript object corresponding to value.
    auto& object = callback.callback;

    // 4. Let realm be O’s associated Realm.
    auto& realm = object->shape().realm();

    // 5. Let relevant settings be realm’s settings object.
    auto& relevant_settings = Bindings::host_defined_environment_settings_object(realm);

    // 6. Let stored settings be value’s callback context.
    auto& stored_settings = callback.callback_context;

    // 7. Prepare to run script with relevant settings.
    relevant_settings.prepare_to_run_script();

    // 8. Prepare to run a callback with stored settings.
    stored_settings->prepare_to_run_callback();

    // 9. Let X be O.
    auto actual_function_object = object;

    // 10. If ! IsCallable(O) is false, then:
    if (!object->is_function()) {
        // 1. Let getResult be Get(O, opName).
        auto get_result = object->get(operation_name);

        // 2. If getResult is an abrupt completion, set completion to getResult and jump to the step labeled return.
        if (get_result.is_throw_completion()) {
            completion = get_result.throw_completion();
            return clean_up_on_return(stored_settings, relevant_settings, completion);
        }

        // 4. If ! IsCallable(X) is false, then set completion to a new Completion{[[Type]]: throw, [[Value]]: a newly created TypeError object, [[Target]]: empty}, and jump to the step labeled return.
        if (!get_result.value().is_function()) {
            completion = realm.vm().template throw_completion<JS::TypeError>(JS::ErrorType::NotAFunction, TRY_OR_THROW_OOM(realm.vm(), get_result.value().to_string_without_side_effects()));
            return clean_up_on_return(stored_settings, relevant_settings, completion);
        }

        // 3. Set X to getResult.[[Value]].
        // NOTE: This is done out of order because `actual_function_object` is of type JS::Object and we cannot assign to it until we know for sure getResult.[[Value]] is a JS::Object.
        actual_function_object = get_result.release_value().as_object();

        // 5. Set thisArg to O (overriding the provided value).
        this_argument = object;
    }

    // FIXME: 11. Let esArgs be the result of converting args to an ECMAScript arguments list. If this throws an exception, set completion to the completion value representing the thrown exception and jump to the step labeled return.
    //        For simplicity, we currently make the caller do this. However, this means we can't throw exceptions at this point like the spec wants us to.

    // 12. Let callResult be Call(X, thisArg, esArgs).
    VERIFY(actual_function_object);
    auto& vm = object->vm();
    auto call_result = JS::call(vm, verify_cast<JS::FunctionObject>(*actual_function_object), this_argument.value(), move(args));

    // 13. If callResult is an abrupt completion, set completion to callResult and jump to the step labeled return.
    if (call_result.is_throw_completion()) {
        completion = call_result.throw_completion();
        return clean_up_on_return(stored_settings, relevant_settings, completion);
    }

    // 14. Set completion to the result of converting callResult.[[Value]] to an IDL value of the same type as the operation’s return type.
    // FIXME: This does no conversion.
    completion = call_result.value();

    return clean_up_on_return(stored_settings, relevant_settings, completion);
}

// https://webidl.spec.whatwg.org/#invoke-a-callback-function
JS::Completion invoke_callback(WebIDL::CallbackType& callback, Optional<JS::Value> this_argument, JS::MarkedVector<JS::Value> args)
{
    // 1. Let completion be an uninitialized variable.
    JS::Completion completion;

    // 2. If thisArg was not given, let thisArg be undefined.
    if (!this_argument.has_value())
        this_argument = JS::js_undefined();

    // 3. Let F be the ECMAScript object corresponding to callable.
    auto& function_object = callback.callback;

    // 4. If ! IsCallable(F) is false:
    if (!function_object->is_function()) {
        // 1. Note: This is only possible when the callback function came from an attribute marked with [LegacyTreatNonObjectAsNull].

        // 2. Return the result of converting undefined to the callback function’s return type.
        // FIXME: This does no conversion.
        return { JS::js_undefined() };
    }

    // 5. Let realm be F’s associated Realm.
    // See the comment about associated realm on step 4 of call_user_object_operation.
    auto& realm = function_object->shape().realm();

    // 6. Let relevant settings be realm’s settings object.
    auto& relevant_settings = Bindings::host_defined_environment_settings_object(realm);

    // 7. Let stored settings be value’s callback context.
    auto& stored_settings = callback.callback_context;

    // 8. Prepare to run script with relevant settings.
    relevant_settings.prepare_to_run_script();

    // 9. Prepare to run a callback with stored settings.
    stored_settings->prepare_to_run_callback();

    // FIXME: 10. Let esArgs be the result of converting args to an ECMAScript arguments list. If this throws an exception, set completion to the completion value representing the thrown exception and jump to the step labeled return.
    //        For simplicity, we currently make the caller do this. However, this means we can't throw exceptions at this point like the spec wants us to.

    // 11. Let callResult be Call(F, thisArg, esArgs).
    auto& vm = function_object->vm();
    auto call_result = JS::call(vm, verify_cast<JS::FunctionObject>(*function_object), this_argument.value(), move(args));

    // 12. If callResult is an abrupt completion, set completion to callResult and jump to the step labeled return.
    if (call_result.is_throw_completion()) {
        completion = call_result.throw_completion();
        return clean_up_on_return(stored_settings, relevant_settings, completion);
    }

    // 13. Set completion to the result of converting callResult.[[Value]] to an IDL value of the same type as the operation’s return type.
    // FIXME: This does no conversion.
    completion = call_result.value();

    return clean_up_on_return(stored_settings, relevant_settings, completion);
}

}
