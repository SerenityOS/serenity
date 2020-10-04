/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ProxyObject.h>

namespace JS {

bool static is_compatible_property_descriptor(bool is_extensible, PropertyDescriptor new_descriptor, Optional<PropertyDescriptor> current_descriptor_optional)
{
    if (!current_descriptor_optional.has_value())
        return is_extensible;
    auto current_descriptor = current_descriptor_optional.value();
    if (new_descriptor.attributes.is_empty() && new_descriptor.value.is_empty() && !new_descriptor.getter && !new_descriptor.setter)
        return true;
    if (!current_descriptor.attributes.is_configurable()) {
        if (new_descriptor.attributes.is_configurable())
            return false;
        if (new_descriptor.attributes.has_enumerable() && new_descriptor.attributes.is_enumerable() != current_descriptor.attributes.is_enumerable())
            return false;
    }
    if (new_descriptor.is_generic_descriptor())
        return true;
    if (current_descriptor.is_data_descriptor() != new_descriptor.is_data_descriptor() && !current_descriptor.attributes.is_configurable())
        return false;
    if (current_descriptor.is_data_descriptor() && new_descriptor.is_data_descriptor() && !current_descriptor.attributes.is_configurable() && !current_descriptor.attributes.is_writable()) {
        if (new_descriptor.attributes.is_writable())
            return false;
        return new_descriptor.value.is_empty() && same_value(new_descriptor.value, current_descriptor.value);
    }
    return true;
}

ProxyObject* ProxyObject::create(GlobalObject& global_object, Object& target, Object& handler)
{
    return global_object.heap().allocate<ProxyObject>(global_object, target, handler, *global_object.proxy_prototype());
}

ProxyObject::ProxyObject(Object& target, Object& handler, Object& prototype)
    : Function(prototype)
    , m_target(target)
    , m_handler(handler)
{
}

ProxyObject::~ProxyObject()
{
}

Object* ProxyObject::prototype()
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return nullptr;
    }
    auto trap = m_handler.get("getPrototypeOf");
    if (vm().exception())
        return nullptr;
    if (trap.is_empty() || trap.is_nullish())
        return m_target.prototype();
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "getPrototypeOf");
        return nullptr;
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target));
    if (vm().exception())
        return nullptr;
    if (!trap_result.is_object() && !trap_result.is_null()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetPrototypeOfReturn);
        return nullptr;
    }
    if (m_target.is_extensible()) {
        if (vm().exception())
            return nullptr;
        if (trap_result.is_null())
            return nullptr;
        return &trap_result.as_object();
    }
    auto target_proto = m_target.prototype();
    if (vm().exception())
        return nullptr;
    if (!same_value(trap_result, Value(target_proto))) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetPrototypeOfNonExtensible);
        return nullptr;
    }
    return &trap_result.as_object();
}

const Object* ProxyObject::prototype() const
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return nullptr;
    }
    return const_cast<const Object*>(const_cast<ProxyObject*>(this)->prototype());
}

bool ProxyObject::set_prototype(Object* object)
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("setPrototypeOf");
    if (vm().exception())
        return false;
    if (trap.is_empty() || trap.is_nullish())
        return m_target.set_prototype(object);
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "setPrototypeOf");
        return false;
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target), Value(object)).to_boolean();
    if (vm().exception() || !trap_result)
        return false;
    if (m_target.is_extensible())
        return true;
    auto* target_proto = m_target.prototype();
    if (vm().exception())
        return false;
    if (!same_value(Value(object), Value(target_proto))) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxySetPrototypeOfNonExtensible);
        return false;
    }
    return true;
}

bool ProxyObject::is_extensible() const
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("isExtensible");
    if (vm().exception())
        return false;
    if (trap.is_empty() || trap.is_nullish())
        return m_target.is_extensible();
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "isExtensible");
        return {};
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target)).to_boolean();
    if (vm().exception())
        return false;
    if (trap_result != m_target.is_extensible()) {
        if (!vm().exception())
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyIsExtensibleReturn);
        return false;
    }
    return trap_result;
}

bool ProxyObject::prevent_extensions()
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("preventExtensions");
    if (vm().exception())
        return false;
    if (trap.is_empty() || trap.is_nullish())
        return m_target.prevent_extensions();
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "preventExtensions");
        return {};
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target)).to_boolean();
    if (vm().exception())
        return false;
    if (trap_result && m_target.is_extensible()) {
        if (!vm().exception())
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyPreventExtensionsReturn);
        return false;
    }
    return trap_result;
}

Optional<PropertyDescriptor> ProxyObject::get_own_property_descriptor(const PropertyName& name) const
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("getOwnPropertyDescriptor");
    if (vm().exception())
        return {};
    if (trap.is_empty() || trap.is_nullish())
        return m_target.get_own_property_descriptor(name);
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "getOwnPropertyDescriptor");
        return {};
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target), js_string(vm(), name.to_string()));
    if (vm().exception())
        return {};
    if (!trap_result.is_object() && !trap_result.is_undefined()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetOwnDescriptorReturn);
        return {};
    }
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (vm().exception())
        return {};
    if (trap_result.is_undefined()) {
        if (!target_desc.has_value())
            return {};
        if (!target_desc.value().attributes.is_configurable()) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetOwnDescriptorNonConfigurable);
            return {};
        }
        if (!m_target.is_extensible()) {
            if (!vm().exception())
                vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetOwnDescriptorUndefReturn);
            return {};
        }
        return {};
    }
    auto result_desc = PropertyDescriptor::from_dictionary(vm(), trap_result.as_object());
    if (vm().exception())
        return {};
    if (!is_compatible_property_descriptor(m_target.is_extensible(), result_desc, target_desc)) {
        if (!vm().exception())
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetOwnDescriptorInvalidDescriptor);
        return {};
    }
    if (!result_desc.attributes.is_configurable() && (!target_desc.has_value() || target_desc.value().attributes.is_configurable())) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetOwnDescriptorInvalidNonConfig);
        return {};
    }
    return result_desc;
}

bool ProxyObject::define_property(const StringOrSymbol& property_name, const Object& descriptor, bool throw_exceptions)
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("defineProperty");
    if (vm().exception())
        return false;
    if (trap.is_empty() || trap.is_nullish())
        return m_target.define_property(property_name, descriptor, throw_exceptions);
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "defineProperty");
        return false;
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target), property_name.to_value(vm()), Value(const_cast<Object*>(&descriptor))).to_boolean();
    if (vm().exception() || !trap_result)
        return false;
    auto target_desc = m_target.get_own_property_descriptor(property_name);
    if (vm().exception())
        return false;
    bool setting_config_false = false;
    if (descriptor.has_property("configurable") && !descriptor.get("configurable").to_boolean())
        setting_config_false = true;
    if (vm().exception())
        return false;
    if (!target_desc.has_value()) {
        if (!m_target.is_extensible()) {
            if (!vm().exception())
                vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyDefinePropNonExtensible);
            return false;
        }
        if (setting_config_false) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyDefinePropNonConfigurableNonExisting);
            return false;
        }
    } else {
        if (!is_compatible_property_descriptor(m_target.is_extensible(), PropertyDescriptor::from_dictionary(vm(), descriptor), target_desc)) {
            if (!vm().exception())
                vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyDefinePropIncompatibleDescriptor);
            return false;
        }
        if (setting_config_false && target_desc.value().attributes.is_configurable()) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyDefinePropExistingConfigurable);
            return false;
        }
    }
    return true;
}

bool ProxyObject::has_property(const PropertyName& name) const
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("has");
    if (vm().exception())
        return false;
    if (trap.is_empty() || trap.is_nullish())
        return m_target.has_property(name);
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "has");
        return false;
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target), js_string(vm(), name.to_string())).to_boolean();
    if (vm().exception())
        return false;
    if (!trap_result) {
        auto target_desc = m_target.get_own_property_descriptor(name);
        if (vm().exception())
            return false;
        if (target_desc.has_value()) {
            if (!target_desc.value().attributes.is_configurable()) {
                vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyHasExistingNonConfigurable);
                return false;
            }
            if (!m_target.is_extensible()) {
                if (!vm().exception())
                    vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyHasExistingNonExtensible);
                return false;
            }
        }
    }
    return trap_result;
}

Value ProxyObject::get(const PropertyName& name, Value) const
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("get");
    if (vm().exception())
        return {};
    if (trap.is_empty() || trap.is_nullish())
        return m_target.get(name);
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "get");
        return {};
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target), js_string(vm(), name.to_string()), Value(const_cast<ProxyObject*>(this)));
    if (vm().exception())
        return {};
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (target_desc.has_value()) {
        if (vm().exception())
            return {};
        if (target_desc.value().is_data_descriptor() && !target_desc.value().attributes.is_writable() && !same_value(trap_result, target_desc.value().value)) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetImmutableDataProperty);
            return {};
        }
        if (target_desc.value().is_accessor_descriptor() && target_desc.value().getter == nullptr && !trap_result.is_undefined()) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyGetNonConfigurableAccessor);
            return {};
        }
    }
    return trap_result;
}

bool ProxyObject::put(const PropertyName& name, Value value, Value)
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("set");
    if (vm().exception())
        return false;
    if (trap.is_empty() || trap.is_nullish())
        return m_target.put(name, value);
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "set");
        return false;
    }
    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target), js_string(vm(), name.to_string()), value, Value(const_cast<ProxyObject*>(this))).to_boolean();
    if (vm().exception() || !trap_result)
        return false;
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (vm().exception())
        return false;
    if (target_desc.has_value() && !target_desc.value().attributes.is_configurable()) {
        if (target_desc.value().is_data_descriptor() && !target_desc.value().attributes.is_writable() && !same_value(value, target_desc.value().value)) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxySetImmutableDataProperty);
            return false;
        }
        if (target_desc.value().is_accessor_descriptor() && !target_desc.value().setter) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ProxySetNonConfigurableAccessor);
        }
    }
    return true;
}

Value ProxyObject::delete_property(const PropertyName& name)
{
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("deleteProperty");
    if (vm().exception())
        return {};
    if (trap.is_empty() || trap.is_nullish())
        return m_target.delete_property(name);
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "deleteProperty");
        return {};
    }

    auto trap_result = vm().call(trap.as_function(), Value(&m_handler), Value(&m_target), js_string(vm(), name.to_string())).to_boolean();
    if (vm().exception())
        return {};
    if (!trap_result)
        return Value(false);
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (vm().exception())
        return {};
    if (!target_desc.has_value())
        return Value(true);
    if (!target_desc.value().attributes.is_configurable()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyDeleteNonConfigurable);
        return {};
    }
    return Value(true);
}

void ProxyObject::visit_children(Cell::Visitor& visitor)
{
    Function::visit_children(visitor);
    visitor.visit(&m_target);
    visitor.visit(&m_handler);
}

Value ProxyObject::call()
{
    if (!is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::NotAFunction, Value(this).to_string_without_side_effects());
        return {};
    }
    if (m_is_revoked) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("apply");
    if (vm().exception())
        return {};
    if (trap.is_empty() || trap.is_nullish())
        return static_cast<Function&>(m_target).call();
    if (!trap.is_function()) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "apply");
        return {};
    }
    MarkedValueList arguments(heap());
    arguments.append(Value(&m_target));
    arguments.append(Value(&m_handler));
    // FIXME: Pass global object
    auto arguments_array = Array::create(global_object());
    vm().for_each_argument([&](auto& argument) {
        arguments_array->indexed_properties().append(argument);
    });
    arguments.append(arguments_array);

    return vm().call(trap.as_function(), Value(&m_handler), move(arguments));
}

Value ProxyObject::construct(Function& new_target)
{
    auto& vm = this->vm();
    if (!is_function()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, Value(this).to_string_without_side_effects());
        return {};
    }
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("construct");
    if (vm.exception())
        return {};
    if (trap.is_empty() || trap.is_nullish())
        return static_cast<Function&>(m_target).construct(new_target);
    if (!trap.is_function()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::ProxyInvalidTrap, "construct");
        return {};
    }

    MarkedValueList arguments(vm.heap());
    arguments.append(Value(&m_target));
    auto arguments_array = Array::create(global_object());
    vm.for_each_argument([&](auto& argument) {
        arguments_array->indexed_properties().append(argument);
    });
    arguments.append(arguments_array);
    arguments.append(Value(&new_target));
    auto result = vm.call(trap.as_function(), Value(&m_handler), move(arguments));
    if (!result.is_object()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::ProxyConstructBadReturnType);
        return {};
    }
    return result;
}

const FlyString& ProxyObject::name() const
{
    ASSERT(is_function());
    return static_cast<Function&>(m_target).name();
}

LexicalEnvironment* ProxyObject::create_environment()
{
    ASSERT(is_function());
    return static_cast<Function&>(m_target).create_environment();
}

}
