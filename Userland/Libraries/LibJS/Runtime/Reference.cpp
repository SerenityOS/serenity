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

    if (!is_valid_reference())
        return vm.throw_completion<ReferenceError>(global_object, ErrorType::InvalidLeftHandAssignment);

    if (is_unresolvable()) {
        if (m_strict)
            return throw_reference_error(global_object);
        TRY(global_object.set(m_name, value, Object::ShouldThrowExceptions::No));
        return {};
    }

    if (is_property_reference()) {
        auto* base_obj = TRY(m_base_value.to_object(global_object));

        if (is_private_reference())
            return base_obj->private_set(m_private_name, value);

        auto succeeded = TRY(base_obj->internal_set(m_name, value, get_this_value()));
        if (!succeeded && m_strict)
            return vm.throw_completion<TypeError>(global_object, ErrorType::ReferenceNullishSetProperty, m_name, m_base_value.to_string_without_side_effects());
        return {};
    }

    VERIFY(m_base_type == BaseType::Environment);
    VERIFY(m_base_environment);
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
    if (!is_valid_reference() || is_unresolvable())
        return throw_reference_error(global_object);

    if (is_property_reference()) {
        auto* base_obj = TRY(m_base_value.to_object(global_object));

        if (is_private_reference())
            return base_obj->private_get(m_private_name);

        return base_obj->get(m_name);
    }

    VERIFY(m_base_type == BaseType::Environment);

    VERIFY(m_base_environment);
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
    auto* private_environment = vm.running_execution_context().private_environment;
    VERIFY(private_environment);
    return Reference { base_value, private_environment->resolve_private_identifier(private_identifier) };
}

}
