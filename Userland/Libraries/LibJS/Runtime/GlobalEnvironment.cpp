/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ObjectEnvironment.h>

namespace JS {

// 9.1.2.5 NewGlobalEnvironment ( G, thisValue ), https://tc39.es/ecma262/#sec-newglobalenvironment
GlobalEnvironment::GlobalEnvironment(GlobalObject& global_object, Object& this_value)
    : Environment(nullptr)
    , m_global_this_value(&this_value)
{
    m_object_record = global_object.heap().allocate<ObjectEnvironment>(global_object, global_object, ObjectEnvironment::IsWithEnvironment::No, nullptr);
    m_declarative_record = global_object.heap().allocate<DeclarativeEnvironment>(global_object);
}

void GlobalEnvironment::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_object_record);
    visitor.visit(m_global_this_value);
    visitor.visit(m_declarative_record);
}

// 9.1.1.4.11 GetThisBinding ( ), https://tc39.es/ecma262/#sec-global-environment-records-getthisbinding
ThrowCompletionOr<Value> GlobalEnvironment::get_this_binding(GlobalObject&) const
{
    // 1. Return envRec.[[GlobalThisValue]].
    return m_global_this_value;
}

// 9.1.1.4.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-global-environment-records-hasbinding-n
ThrowCompletionOr<bool> GlobalEnvironment::has_binding(FlyString const& name, Optional<size_t>*) const
{
    // 1. Let DclRec be envRec.[[DeclarativeRecord]].
    // 2. If DclRec.HasBinding(N) is true, return true.
    if (MUST(m_declarative_record->has_binding(name)))
        return true;

    // 3. Let ObjRec be envRec.[[ObjectRecord]].
    // 4. Return ? ObjRec.HasBinding(N).
    return m_object_record->has_binding(name);
}

// 9.1.1.4.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-global-environment-records-createmutablebinding-n-d
ThrowCompletionOr<void> GlobalEnvironment::create_mutable_binding(GlobalObject& global_object, FlyString const& name, bool can_be_deleted)
{
    // 1. Let DclRec be envRec.[[DeclarativeRecord]].
    // 2. If DclRec.HasBinding(N) is true, throw a TypeError exception.
    if (MUST(m_declarative_record->has_binding(name)))
        return vm().throw_completion<TypeError>(global_object, ErrorType::GlobalEnvironmentAlreadyHasBinding, name);

    // 3. Return DclRec.CreateMutableBinding(N, D).
    return m_declarative_record->create_mutable_binding(global_object, name, can_be_deleted);
}

// 9.1.1.4.3 CreateImmutableBinding ( N, S ), https://tc39.es/ecma262/#sec-global-environment-records-createimmutablebinding-n-s
ThrowCompletionOr<void> GlobalEnvironment::create_immutable_binding(GlobalObject& global_object, FlyString const& name, bool strict)
{
    // 1. Let DclRec be envRec.[[DeclarativeRecord]].
    // 2. If DclRec.HasBinding(N) is true, throw a TypeError exception.
    if (MUST(m_declarative_record->has_binding(name)))
        return vm().throw_completion<TypeError>(global_object, ErrorType::GlobalEnvironmentAlreadyHasBinding, name);

    // 3. Return DclRec.CreateImmutableBinding(N, S).
    return m_declarative_record->create_immutable_binding(global_object, name, strict);
}

// 9.1.1.4.4 InitializeBinding ( N, V ), https://tc39.es/ecma262/#sec-global-environment-records-initializebinding-n-v
ThrowCompletionOr<void> GlobalEnvironment::initialize_binding(GlobalObject& global_object, FlyString const& name, Value value)
{
    // 1. Let DclRec be envRec.[[DeclarativeRecord]].
    // 2. If DclRec.HasBinding(N) is true, then
    if (MUST(m_declarative_record->has_binding(name))) {
        // a. Return DclRec.InitializeBinding(N, V).
        return m_declarative_record->initialize_binding(global_object, name, value);
    }

    // 3. Assert: If the binding exists, it must be in the object Environment Record.
    // 4. Let ObjRec be envRec.[[ObjectRecord]].
    // 5. Return ? ObjRec.InitializeBinding(N, V).
    return m_object_record->initialize_binding(global_object, name, value);
}

// 9.1.1.4.5 SetMutableBinding ( N, V, S ), https://tc39.es/ecma262/#sec-global-environment-records-setmutablebinding-n-v-s
ThrowCompletionOr<void> GlobalEnvironment::set_mutable_binding(GlobalObject& global_object, FlyString const& name, Value value, bool strict)
{
    // 1. Let DclRec be envRec.[[DeclarativeRecord]].
    // 2. If DclRec.HasBinding(N) is true, then
    if (MUST(m_declarative_record->has_binding(name))) {
        // a. Return DclRec.SetMutableBinding(N, V, S).
        return m_declarative_record->set_mutable_binding(global_object, name, value, strict);
    }

    // 3. Let ObjRec be envRec.[[ObjectRecord]].
    // 4. Return ? ObjRec.SetMutableBinding(N, V, S).
    return m_object_record->set_mutable_binding(global_object, name, value, strict);
}

// 9.1.1.4.6 GetBindingValue ( N, S ), https://tc39.es/ecma262/#sec-global-environment-records-getbindingvalue-n-s
ThrowCompletionOr<Value> GlobalEnvironment::get_binding_value(GlobalObject& global_object, FlyString const& name, bool strict)
{
    // 1. Let DclRec be envRec.[[DeclarativeRecord]].
    // 2. If DclRec.HasBinding(N) is true, then
    if (MUST(m_declarative_record->has_binding(name))) {
        // a. Return DclRec.GetBindingValue(N, S).
        return m_declarative_record->get_binding_value(global_object, name, strict);
    }

    // 3. Let ObjRec be envRec.[[ObjectRecord]].
    // 4. Return ? ObjRec.GetBindingValue(N, S).
    return m_object_record->get_binding_value(global_object, name, strict);
}

// 9.1.1.4.7 DeleteBinding ( N ), https://tc39.es/ecma262/#sec-global-environment-records-deletebinding-n
ThrowCompletionOr<bool> GlobalEnvironment::delete_binding(GlobalObject& global_object, FlyString const& name)
{
    // 1. Let DclRec be envRec.[[DeclarativeRecord]].
    // 2. If DclRec.HasBinding(N) is true, then
    if (MUST(m_declarative_record->has_binding(name))) {
        // a. Return DclRec.DeleteBinding(N).
        return m_declarative_record->delete_binding(global_object, name);
    }

    // 3. Let ObjRec be envRec.[[ObjectRecord]].
    // 4. Let globalObject be ObjRec.[[BindingObject]].

    // 5. Let existingProp be ? HasOwnProperty(globalObject, N).
    bool existing_prop = TRY(m_object_record->binding_object().has_own_property(name));

    // 6. If existingProp is true, then
    if (existing_prop) {
        // a. Let status be ? ObjRec.DeleteBinding(N).
        bool status = TRY(m_object_record->delete_binding(global_object, name));

        // b. If status is true, then
        if (status) {
            // i. Let varNames be envRec.[[VarNames]].
            // ii. If N is an element of varNames, remove that element from the varNames.
            m_var_names.remove_all_matching([&](auto& entry) { return entry == name; });
        }

        // c. Return status.
        return status;
    }

    // 7. Return true.
    return true;
}

// 9.1.1.4.12 HasVarDeclaration ( N ), https://tc39.es/ecma262/#sec-hasvardeclaration
bool GlobalEnvironment::has_var_declaration(FlyString const& name) const
{
    return m_var_names.contains_slow(name);
}

// 9.1.1.4.13 HasLexicalDeclaration ( N ), https://tc39.es/ecma262/#sec-haslexicaldeclaration
bool GlobalEnvironment::has_lexical_declaration(FlyString const& name) const
{
    return MUST(m_declarative_record->has_binding(name));
}

// 9.1.1.4.14 HasRestrictedGlobalProperty ( N ), https://tc39.es/ecma262/#sec-hasrestrictedglobalproperty
bool GlobalEnvironment::has_restricted_global_property(FlyString const& name) const
{
    auto& global_object = m_object_record->binding_object();
    auto existing_prop = TRY_OR_DISCARD(global_object.internal_get_own_property(name));
    if (!existing_prop.has_value())
        return false;
    if (*existing_prop->configurable)
        return false;
    return true;
}

// 9.1.1.4.15 CanDeclareGlobalVar ( N ), https://tc39.es/ecma262/#sec-candeclareglobalvar
bool GlobalEnvironment::can_declare_global_var(FlyString const& name) const
{
    auto& global_object = m_object_record->binding_object();
    bool has_property = TRY_OR_DISCARD(global_object.has_own_property(name));
    if (has_property)
        return true;
    return TRY_OR_DISCARD(global_object.is_extensible());
}

// 9.1.1.4.16 CanDeclareGlobalFunction ( N ), https://tc39.es/ecma262/#sec-candeclareglobalfunction
bool GlobalEnvironment::can_declare_global_function(FlyString const& name) const
{
    auto& global_object = m_object_record->binding_object();
    auto existing_prop = TRY_OR_DISCARD(global_object.internal_get_own_property(name));
    if (!existing_prop.has_value())
        return TRY_OR_DISCARD(global_object.is_extensible());
    if (*existing_prop->configurable)
        return true;
    if (existing_prop->is_data_descriptor() && *existing_prop->writable && *existing_prop->enumerable)
        return true;
    return false;
}

// 9.1.1.4.17 CreateGlobalVarBinding ( N, D ), https://tc39.es/ecma262/#sec-createglobalvarbinding
void GlobalEnvironment::create_global_var_binding(FlyString const& name, bool can_be_deleted)
{
    auto& global_object = m_object_record->binding_object();
    auto has_property_or_error = global_object.has_own_property(name);
    if (has_property_or_error.is_error())
        return;
    auto has_property = has_property_or_error.release_value();
    auto extensible_or_error = global_object.is_extensible();
    if (extensible_or_error.is_error())
        return;
    auto extensible = extensible_or_error.release_value();
    if (!has_property && extensible) {
        auto result = m_object_record->create_mutable_binding(m_object_record->global_object(), name, can_be_deleted);
        if (result.is_error())
            return;
        result = m_object_record->initialize_binding(m_object_record->global_object(), name, js_undefined());
        if (result.is_error())
            return;
    }
    if (!m_var_names.contains_slow(name))
        m_var_names.append(name);
}

// 9.1.1.4.18 CreateGlobalFunctionBinding ( N, V, D ), https://tc39.es/ecma262/#sec-createglobalfunctionbinding
void GlobalEnvironment::create_global_function_binding(FlyString const& name, Value value, bool can_be_deleted)
{
    auto& global_object = m_object_record->binding_object();
    auto existing_prop_or_error = global_object.internal_get_own_property(name);
    if (existing_prop_or_error.is_error())
        return;
    auto existing_prop = existing_prop_or_error.release_value();
    PropertyDescriptor desc;
    if (!existing_prop.has_value() || *existing_prop->configurable)
        desc = { .value = value, .writable = true, .enumerable = true, .configurable = can_be_deleted };
    else
        desc = { .value = value };
    auto result_or_error = global_object.define_property_or_throw(name, desc);
    if (result_or_error.is_error())
        return;
    result_or_error = global_object.set(name, value, Object::ShouldThrowExceptions::Yes);
    if (result_or_error.is_error())
        return;
    if (!m_var_names.contains_slow(name))
        m_var_names.append(name);
}

}
