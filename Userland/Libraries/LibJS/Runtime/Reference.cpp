/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Reference.h>

namespace JS {

// 6.2.4.6 PutValue ( V, W ), https://tc39.es/ecma262/#sec-putvalue
ThrowCompletionOr<void> Reference::put_value(GlobalObject& global_object, Value value)
{
    auto& vm = global_object.vm();

    // 1. ReturnIfAbrupt(V).
    // 2. ReturnIfAbrupt(W).

    // 3. If V is not a Reference Record, throw a ReferenceError exception.
    if (!is_valid_reference())
        return vm.throw_completion<ReferenceError>(global_object, ErrorType::InvalidLeftHandAssignment);

    // 4. If IsUnresolvableReference(V) is true, then
    if (is_unresolvable()) {
        // a. If V.[[Strict]] is true, throw a ReferenceError exception.
        if (m_strict)
            return throw_reference_error(global_object);

        // b. Let globalObj be GetGlobalObject().
        // c. Return ? Set(globalObj, V.[[ReferencedName]], W, false).
        TRY(global_object.set(m_name, value, Object::ShouldThrowExceptions::No));
        return {};
    }

    // 5. If IsPropertyReference(V) is true, then
    if (is_property_reference()) {
        // a. Let baseObj be ? ToObject(V.[[Base]]).
        auto* base_obj = TRY(m_base_value.to_object(global_object));

        // b. If IsPrivateReference(V) is true, then
        if (is_private_reference()) {
            // i. Return ? PrivateSet(baseObj, V.[[ReferencedName]], W).
            return base_obj->private_set(m_private_name, value);
        }

        // c. Let succeeded be ? baseObj.[[Set]](V.[[ReferencedName]], W, GetThisValue(V)).
        auto succeeded = TRY(base_obj->internal_set(m_name, value, get_this_value()));

        // d. If succeeded is false and V.[[Strict]] is true, throw a TypeError exception.
        if (!succeeded && m_strict)
            return vm.throw_completion<TypeError>(global_object, ErrorType::ReferenceNullishSetProperty, m_name, m_base_value.to_string_without_side_effects());

        // e. Return.
        return {};
    }

    // 6. Else,
    // a. Let base be V.[[Base]].

    // b. Assert: base is an Environment Record.
    VERIFY(m_base_type == BaseType::Environment);
    VERIFY(m_base_environment);

    // c. Return ? base.SetMutableBinding(V.[[ReferencedName]], W, V.[[Strict]]) (see 9.1).
    if (m_environment_coordinate.has_value())
        return static_cast<DeclarativeEnvironment*>(m_base_environment)->set_mutable_binding_direct(global_object, m_environment_coordinate->index, value, m_strict);
    else
        return m_base_environment->set_mutable_binding(global_object, m_name.as_string(), value, m_strict);
}

Completion Reference::throw_reference_error(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();
    if (!m_name.is_valid())
        return vm.throw_completion<ReferenceError>(global_object, ErrorType::ReferenceUnresolvable);
    else
        return vm.throw_completion<ReferenceError>(global_object, ErrorType::UnknownIdentifier, m_name.to_string_or_symbol().to_display_string());
}

// 6.2.4.5 GetValue ( V ), https://tc39.es/ecma262/#sec-getvalue
ThrowCompletionOr<Value> Reference::get_value(GlobalObject& global_object) const
{
    // 1. ReturnIfAbrupt(V).
    // 2. If V is not a Reference Record, return V.

    // 3. If IsUnresolvableReference(V) is true, throw a ReferenceError exception.
    if (!is_valid_reference() || is_unresolvable())
        return throw_reference_error(global_object);

    // 4. If IsPropertyReference(V) is true, then
    if (is_property_reference()) {
        // a. Let baseObj be ? ToObject(V.[[Base]]).
        // NOTE: Deferred as an optimization; we might not actually need to create an object.

        // b. If IsPrivateReference(V) is true, then
        if (is_private_reference()) {
            // FIXME: We need to be able to specify the receiver for this
            // if we want to use it in error messages in future
            // as things currently stand this does the "wrong thing" but
            // the error is unobservable

            auto* base_obj = TRY(m_base_value.to_object(global_object));

            // i. Return ? PrivateGet(baseObj, V.[[ReferencedName]]).
            return base_obj->private_get(m_private_name);
        }

        // OPTIMIZATION: For various primitives we can avoid actually creating a new object for them.
        Object* base_obj = nullptr;
        if (m_base_value.is_string()) {
            auto string_value = m_base_value.as_string().get(global_object, m_name);
            if (string_value.has_value())
                return *string_value;
            base_obj = global_object.string_prototype();
        } else if (m_base_value.is_number())
            base_obj = global_object.number_prototype();
        else if (m_base_value.is_boolean())
            base_obj = global_object.boolean_prototype();
        else
            base_obj = TRY(m_base_value.to_object(global_object));

        // c. Return ? baseObj.[[Get]](V.[[ReferencedName]], GetThisValue(V)).
        return base_obj->internal_get(m_name, get_this_value());
    }

    // 5. Else,
    // a. Let base be V.[[Base]].

    // b. Assert: base is an Environment Record.
    VERIFY(m_base_type == BaseType::Environment);
    VERIFY(m_base_environment);

    // c. Return ? base.GetBindingValue(V.[[ReferencedName]], V.[[Strict]]) (see 9.1).
    if (m_environment_coordinate.has_value())
        return static_cast<DeclarativeEnvironment*>(m_base_environment)->get_binding_value_direct(global_object, m_environment_coordinate->index, m_strict);
    return m_base_environment->get_binding_value(global_object, m_name.as_string(), m_strict);
}

// 13.5.1.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-delete-operator-runtime-semantics-evaluation
ThrowCompletionOr<bool> Reference::delete_(GlobalObject& global_object)
{
    // 13.5.1.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-delete-operator-runtime-semantics-evaluation
    // UnaryExpression : delete UnaryExpression

    // NOTE: The following steps have already been evaluated by the time we get here:
    // 1. Let ref be the result of evaluating UnaryExpression.
    // 2. ReturnIfAbrupt(ref).
    // 3. If ref is not a Reference Record, return true.

    // 4. If IsUnresolvableReference(ref) is true, then
    if (is_unresolvable()) {
        // a. Assert: ref.[[Strict]] is false.
        VERIFY(!m_strict);
        // b. Return true.
        return true;
    }

    auto& vm = global_object.vm();

    // 5. If IsPropertyReference(ref) is true, then
    if (is_property_reference()) {
        // a. Assert: ! IsPrivateReference(ref) is false.
        VERIFY(!is_private_reference());

        // b. If IsSuperReference(ref) is true, throw a ReferenceError exception.
        if (is_super_reference())
            return vm.throw_completion<ReferenceError>(global_object, ErrorType::UnsupportedDeleteSuperProperty);

        // c. Let baseObj be ! ToObject(ref.[[Base]]).
        auto* base_obj = MUST(m_base_value.to_object(global_object));

        // d. Let deleteStatus be ? baseObj.[[Delete]](ref.[[ReferencedName]]).
        bool delete_status = TRY(base_obj->internal_delete(m_name));

        // e. If deleteStatus is false and ref.[[Strict]] is true, throw a TypeError exception.
        if (!delete_status && m_strict)
            return vm.throw_completion<TypeError>(global_object, ErrorType::ReferenceNullishDeleteProperty, m_name, m_base_value.to_string_without_side_effects());

        // f. Return deleteStatus.
        return delete_status;
    }

    // 6. Else,
    //    a. Let base be ref.[[Base]].
    //    b. Assert: base is an Environment Record.

    VERIFY(m_base_type == BaseType::Environment);

    //    c. Return ? base.DeleteBinding(ref.[[ReferencedName]]).
    return m_base_environment->delete_binding(global_object, m_name.as_string());
}

String Reference::to_string() const
{
    StringBuilder builder;
    builder.append("Reference { Base=");
    switch (m_base_type) {
    case BaseType::Unresolvable:
        builder.append("Unresolvable");
        break;
    case BaseType::Environment:
        builder.appendff("{}", base_environment().class_name());
        break;
    case BaseType::Value:
        if (m_base_value.is_empty())
            builder.append("<empty>");
        else
            builder.appendff("{}", m_base_value.to_string_without_side_effects());
        break;
    }
    builder.append(", ReferencedName=");
    if (!m_name.is_valid())
        builder.append("<invalid>");
    else if (m_name.is_symbol())
        builder.appendff("{}", m_name.as_symbol()->to_string());
    else
        builder.appendff("{}", m_name.to_string());
    builder.appendff(", Strict={}", m_strict);
    builder.appendff(", ThisValue=");
    if (m_this_value.is_empty())
        builder.append("<empty>");
    else
        builder.appendff("{}", m_this_value.to_string_without_side_effects());

    builder.append(" }");
    return builder.to_string();
}

// 6.2.4.9 MakePrivateReference ( baseValue, privateIdentifier ), https://tc39.es/ecma262/#sec-makeprivatereference
Reference make_private_reference(VM& vm, Value base_value, FlyString const& private_identifier)
{
    // 1. Let privEnv be the running execution context's PrivateEnvironment.
    auto* private_environment = vm.running_execution_context().private_environment;

    // 2. Assert: privEnv is not null.
    VERIFY(private_environment);

    // 3. Let privateName be ! ResolvePrivateIdentifier(privEnv, privateIdentifier).
    auto private_name = private_environment->resolve_private_identifier(private_identifier);

    // 4. Return the Reference Record { [[Base]]: baseValue, [[ReferencedName]]: privateName, [[Strict]]: true, [[ThisValue]]: empty }.
    return Reference { base_value, private_name };
}

}
