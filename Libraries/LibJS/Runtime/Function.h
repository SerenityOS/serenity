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

#include <AK/String.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Function : public Object {
    JS_OBJECT(Function, Object);

public:
    enum class ConstructorKind {
        Base,
        Derived,
    };

    virtual ~Function();
    virtual void initialize(GlobalObject&) override { }

    virtual Value call() = 0;
    virtual Value construct(Function& new_target) = 0;
    virtual const FlyString& name() const = 0;
    virtual LexicalEnvironment* create_environment() = 0;

    virtual void visit_children(Visitor&) override;

    virtual bool is_script_function() const { return false; }

    BoundFunction* bind(Value bound_this_value, Vector<Value> arguments);

    Value bound_this() const { return m_bound_this; }

    const Vector<Value>& bound_arguments() const { return m_bound_arguments; }

    Value home_object() const { return m_home_object; }
    void set_home_object(Value home_object) { m_home_object = home_object; }

    ConstructorKind constructor_kind() const { return m_constructor_kind; };
    void set_constructor_kind(ConstructorKind constructor_kind) { m_constructor_kind = constructor_kind; }

    virtual bool is_strict_mode() const { return false; }

protected:
    explicit Function(Object& prototype);
    Function(Object& prototype, Value bound_this, Vector<Value> bound_arguments);

private:
    virtual bool is_function() const override { return true; }
    Value m_bound_this;
    Vector<Value> m_bound_arguments;
    Value m_home_object;
    ConstructorKind m_constructor_kind = ConstructorKind::Base;
};

}
