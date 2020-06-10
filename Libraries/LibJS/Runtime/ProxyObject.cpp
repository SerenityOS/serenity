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

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ProxyObject.h>

namespace JS {

bool static is_compatible_property_descriptor(Interpreter& interpreter, bool is_extensible, PropertyDescriptor new_descriptor, Optional<PropertyDescriptor> current_descriptor_optional)
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
        return new_descriptor.value.is_empty() && same_value(interpreter, new_descriptor.value, current_descriptor.value);
    }
    return true;
}

ProxyObject* ProxyObject::create(GlobalObject& global_object, Object& target, Object& handler)
{
    return global_object.heap().allocate<ProxyObject>(target, handler, *global_object.proxy_prototype());
}

ProxyObject::ProxyObject(Object& target, Object& handler, Object& prototype)
    : Object(&prototype)
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
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return nullptr;
    }
    auto trap = m_handler.get("getPrototypeOf");
    if (interpreter().exception())
        return nullptr;
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.prototype();
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "getPrototypeOf");
        return nullptr;
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments));
    if (interpreter().exception())
        return nullptr;
    if (!trap_result.is_object() && !trap_result.is_null()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyGetPrototypeOfReturn);
        return nullptr;
    }
    if (m_target.is_extensible()) {
        if (interpreter().exception())
            return nullptr;
        if (trap_result.is_null())
            return nullptr;
        return &trap_result.as_object();
    }
    auto target_proto = m_target.prototype();
    if (interpreter().exception())
        return nullptr;
    if (!same_value(interpreter(), trap_result, Value(target_proto))) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyGetPrototypeOfNonExtensible);
        return nullptr;
    }
    return &trap_result.as_object();
}

const Object* ProxyObject::prototype() const
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return nullptr;
    }
    return const_cast<const Object*>(const_cast<ProxyObject*>(this)->prototype());
}

bool ProxyObject::set_prototype(Object* object)
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("setPrototypeOf");
    if (interpreter().exception())
        return false;
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.set_prototype(object);
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "setPrototypeOf");
        return false;
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    arguments.values().append(Value(object));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments)).to_boolean();
    if (interpreter().exception() || !trap_result)
        return false;
    if (m_target.is_extensible())
        return true;
    auto* target_proto = m_target.prototype();
    if (interpreter().exception())
        return false;
    if (!same_value(interpreter(), Value(object), Value(target_proto))) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxySetPrototypeOfNonExtensible);
        return false;
    }
    return true;
}

bool ProxyObject::is_extensible() const
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("isExtensible");
    if (interpreter().exception())
        return false;
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.is_extensible();
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "isExtensible");
        return {};
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments)).to_boolean();
    if (interpreter().exception())
        return false;
    if (trap_result != m_target.is_extensible()) {
        if (!interpreter().exception())
            interpreter().throw_exception<TypeError>(ErrorType::ProxyIsExtensibleReturn);
        return false;
    }
    return trap_result;
}

bool ProxyObject::prevent_extensions()
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("preventExtensions");
    if (interpreter().exception())
        return false;
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.prevent_extensions();
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "preventExtensions");
        return {};
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments)).to_boolean();
    if (interpreter().exception())
        return false;
    if (trap_result && m_target.is_extensible()) {
        if (!interpreter().exception())
            interpreter().throw_exception<TypeError>(ErrorType::ProxyPreventExtensionsReturn);
        return false;
    }
    return trap_result;
}

Optional<PropertyDescriptor> ProxyObject::get_own_property_descriptor(PropertyName name) const
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("getOwnPropertyDescriptor");
    if (interpreter().exception())
        return {};
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.get_own_property_descriptor(name);
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "getOwnPropertyDescriptor");
        return {};
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    arguments.values().append(js_string(interpreter(), name.to_string()));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments));
    if (interpreter().exception())
        return {};
    if (!trap_result.is_object() && !trap_result.is_undefined()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyGetOwnDescriptorReturn);
        return {};
    }
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (interpreter().exception())
        return {};
    if (trap_result.is_undefined()) {
        if (!target_desc.has_value())
            return {};
        if (!target_desc.value().attributes.is_configurable()) {
            interpreter().throw_exception<TypeError>(ErrorType::ProxyGetOwnDescriptorNonConfigurable);
            return {};
        }
        if (!m_target.is_extensible()) {
            if (!interpreter().exception())
                interpreter().throw_exception<TypeError>(ErrorType::ProxyGetOwnDescriptorUndefReturn);
            return {};
        }
        return {};
    }
    auto result_desc = PropertyDescriptor::from_dictionary(interpreter(), trap_result.as_object());
    if (interpreter().exception())
        return {};
    if (!is_compatible_property_descriptor(interpreter(), m_target.is_extensible(), result_desc, target_desc)) {
        if (!interpreter().exception())
            interpreter().throw_exception<TypeError>(ErrorType::ProxyGetOwnDescriptorInvalidDescriptor);
        return {};
    }
    if (!result_desc.attributes.is_configurable() && (!target_desc.has_value() || target_desc.value().attributes.is_configurable())) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyGetOwnDescriptorInvalidNonConfig);
        return {};
    }
    return result_desc;
}

bool ProxyObject::define_property(const FlyString& property_name, const Object& descriptor, bool throw_exceptions)
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("defineProperty");
    if (interpreter().exception())
        return false;
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.define_property(property_name, descriptor, throw_exceptions);
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "defineProperty");
        return false;
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    arguments.values().append(js_string(interpreter(), property_name));
    arguments.values().append(Value(const_cast<Object*>(&descriptor)));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments)).to_boolean();
    if (interpreter().exception() || !trap_result)
        return false;
    auto target_desc = m_target.get_own_property_descriptor(property_name);
    if (interpreter().exception())
        return false;
    bool setting_config_false = false;
    if (descriptor.has_property("configurable") && !descriptor.get("configurable").to_boolean())
        setting_config_false = true;
    if (interpreter().exception())
        return false;
    if (!target_desc.has_value()) {
        if (!m_target.is_extensible()) {
            if (!interpreter().exception())
                interpreter().throw_exception<TypeError>(ErrorType::ProxyDefinePropNonExtensible);
            return false;
        }
        if (setting_config_false) {
            interpreter().throw_exception<TypeError>(ErrorType::ProxyDefinePropNonConfigurableNonExisting);
            return false;
        }
    } else {
        if (!is_compatible_property_descriptor(interpreter(), m_target.is_extensible(), PropertyDescriptor::from_dictionary(interpreter(), descriptor), target_desc)) {
            if (!interpreter().exception())
                interpreter().throw_exception<TypeError>(ErrorType::ProxyDefinePropIncompatibleDescriptor);
            return false;
        }
        if (setting_config_false && target_desc.value().attributes.is_configurable()) {
            interpreter().throw_exception<TypeError>(ErrorType::ProxyDefinePropExistingConfigurable);
            return false;
        }
    }
    return true;
}

bool ProxyObject::has_property(PropertyName name) const
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("has");
    if (interpreter().exception())
        return false;
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.has_property(name);
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "has");
        return false;
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    arguments.values().append(js_string(interpreter(), name.to_string()));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments)).to_boolean();
    if (interpreter().exception())
        return false;
    if (!trap_result) {
        auto target_desc = m_target.get_own_property_descriptor(name);
        if (interpreter().exception())
            return false;
        if (target_desc.has_value()) {
            if (!target_desc.value().attributes.is_configurable()) {
                interpreter().throw_exception<TypeError>(ErrorType::ProxyHasExistingNonConfigurable);
                return false;
            }
            if (!m_target.is_extensible()) {
                if (!interpreter().exception())
                    interpreter().throw_exception<TypeError>(ErrorType::ProxyHasExistingNonExtensible);
                return false;
            }
        }
    }
    return trap_result;
}

Value ProxyObject::get(PropertyName name) const
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("get");
    if (interpreter().exception())
        return {};
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.get(name);
    if (!trap.is_function())
        return interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "get");
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    arguments.values().append(js_string(interpreter(), name.to_string()));
    arguments.values().append(Value(const_cast<ProxyObject*>(this)));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments));
    if (interpreter().exception())
        return {};
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (target_desc.has_value()) {
        if (interpreter().exception())
            return {};
        if (target_desc.value().is_data_descriptor() && !target_desc.value().attributes.is_writable() && !same_value(interpreter(), trap_result, target_desc.value().value))
            return interpreter().throw_exception<TypeError>(ErrorType::ProxyGetImmutableDataProperty);
        if (target_desc.value().is_accessor_descriptor() && target_desc.value().getter == nullptr && !trap_result.is_undefined())
            return interpreter().throw_exception<TypeError>(ErrorType::ProxyGetNonConfigurableAccessor);
    }
    return trap_result;
}

bool ProxyObject::put(PropertyName name, Value value)
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return false;
    }
    auto trap = m_handler.get("set");
    if (interpreter().exception())
        return false;
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.put(name, value);
    if (!trap.is_function()) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "set");
        return false;
    }
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    arguments.values().append(js_string(interpreter(), name.to_string()));
    arguments.values().append(value);
    arguments.values().append(Value(const_cast<ProxyObject*>(this)));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments)).to_boolean();
    if (interpreter().exception() || !trap_result)
        return false;
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (interpreter().exception())
        return false;
    if (target_desc.has_value() && !target_desc.value().attributes.is_configurable()) {
        if (target_desc.value().is_data_descriptor() && !target_desc.value().attributes.is_writable() && !same_value(interpreter(), value, target_desc.value().value)) {
            interpreter().throw_exception<TypeError>(ErrorType::ProxySetImmutableDataProperty);
            return false;
        }
        if (target_desc.value().is_accessor_descriptor() && !target_desc.value().setter) {
            interpreter().throw_exception<TypeError>(ErrorType::ProxySetNonConfigurableAccessor);
        }
    }
    return true;
}

Value ProxyObject::delete_property(PropertyName name)
{
    if (m_is_revoked) {
        interpreter().throw_exception<TypeError>(ErrorType::ProxyRevoked);
        return {};
    }
    auto trap = m_handler.get("deleteProperty");
    if (interpreter().exception())
        return {};
    if (trap.is_empty() || trap.is_undefined() || trap.is_null())
        return m_target.delete_property(name);
    if (!trap.is_function())
        return interpreter().throw_exception<TypeError>(ErrorType::ProxyInvalidTrap, "deleteProperty");
    MarkedValueList arguments(interpreter().heap());
    arguments.values().append(Value(&m_target));
    arguments.values().append(js_string(interpreter(), name.to_string()));
    auto trap_result = interpreter().call(trap.as_function(), Value(&m_handler), move(arguments)).to_boolean();
    if (interpreter().exception())
        return {};
    if (!trap_result)
        return Value(false);
    auto target_desc = m_target.get_own_property_descriptor(name);
    if (interpreter().exception())
        return {};
    if (!target_desc.has_value())
        return Value(true);
    if (!target_desc.value().attributes.is_configurable())
        return interpreter().throw_exception<TypeError>(ErrorType::ProxyDeleteNonConfigurable);
    return Value(true);
}

void ProxyObject::visit_children(Cell::Visitor& visitor)
{
    Object::visit_children(visitor);
    visitor.visit(&m_target);
    visitor.visit(&m_handler);
}

}
