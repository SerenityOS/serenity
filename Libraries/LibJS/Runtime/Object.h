/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Object : public Cell {
public:
    Object();
    virtual ~Object();

    Value get(const FlyString& property_name) const;
    void put(const FlyString& property_name, Value);

    virtual Optional<Value> get_own_property(const FlyString& property_name) const;
    virtual bool put_own_property(const FlyString& property_name, Value);

    void put_native_function(const FlyString& property_name, AK::Function<Value(Object*, Vector<Value>)>);
    void put_native_property(const FlyString& property_name, AK::Function<Value(Object*)> getter, AK::Function<void(Object*, Value)> setter);

    virtual bool is_array() const { return false; }
    virtual bool is_function() const { return false; }
    virtual bool is_native_function() const { return false; }
    virtual bool is_string_object() const { return false; }
    virtual bool is_native_property() const { return false; }

    virtual const char* class_name() const override { return "Object"; }
    virtual void visit_children(Cell::Visitor&) override;

    Object* prototype() { return m_prototype; }
    const Object* prototype() const { return m_prototype; }
    void set_prototype(Object* prototype) { m_prototype = prototype; }

    bool has_own_property(const FlyString& property_name) const;
    enum class PreferredType {
        Default,
        String,
        Number,
    };

    virtual Value value_of() const { return Value(const_cast<Object*>(this)); }
    virtual Value to_primitive(PreferredType preferred_type = PreferredType::Default) const;
    virtual Value to_string() const;

private:
    HashMap<FlyString, Value> m_properties;
    Object* m_prototype { nullptr };
};

}
