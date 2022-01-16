/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Module.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/Environment.h>

namespace JS {

// 9.1.1.5 Module Environment Records, https://tc39.es/ecma262/#sec-module-environment-records
class ModuleEnvironment final : public DeclarativeEnvironment {
    JS_ENVIRONMENT(ModuleEnvironment, DeclarativeEnvironment);

public:
    ModuleEnvironment(Environment* outer_environment);

    // Note: Module Environment Records support all of the declarative Environment Record methods listed
    //       in Table 18 and share the same specifications for all of those methods except for
    //       GetBindingValue, DeleteBinding, HasThisBinding and GetThisBinding.
    //       In addition, module Environment Records support the methods listed in Table 24.
    virtual ThrowCompletionOr<Value> get_binding_value(GlobalObject&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<bool> delete_binding(GlobalObject&, FlyString const& name) override;
    virtual bool has_this_binding() const final { return true; }
    virtual ThrowCompletionOr<Value> get_this_binding(GlobalObject&) const final;
    ThrowCompletionOr<void> create_import_binding(FlyString name, Module* module, FlyString binding_name);

    // Note: Although the spec does not explicitly say this we also have to implement HasBinding as
    //       the HasBinding method of Declarative Environment records states:
    //       "It determines if the argument identifier is one of the identifiers bound by the record"
    //       And this means that we have to include the indirect bindings of a Module Environment.
    virtual ThrowCompletionOr<bool> has_binding(FlyString const& name, Optional<size_t>* = nullptr) const override;

private:
    struct IndirectBinding {
        FlyString name;
        Module* module;
        FlyString binding_name;
    };
    IndirectBinding const* get_indirect_binding(FlyString const& name) const;

    // FIXME: Since we always access this via the name this could be a map.
    Vector<IndirectBinding> m_indirect_bindings;
};

}
