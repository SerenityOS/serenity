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

#include <LibJS/Runtime/Object.h>

namespace JS {

class GlobalObject : public Object {
public:
    explicit GlobalObject();
    virtual ~GlobalObject() override;

    ArrayConstructor* array_constructor() { return m_array_constructor; }
    BooleanConstructor* boolean_constructor() { return m_boolean_constructor; }
    DateConstructor* date_constructor() { return m_date_constructor; }
    FunctionConstructor* function_constructor() { return m_function_constructor; }
    NumberConstructor* number_constructor() { return m_number_constructor; };
    ObjectConstructor* object_constructor() { return m_object_constructor; }
    ErrorConstructor* error_constructor() { return m_error_constructor; }

#define __JS_ENUMERATE_ERROR_SUBCLASS(TitleCase, snake_case) \
    TitleCase##Constructor* snake_case##_constructor() { return m_##snake_case##_constructor; }
    JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE_ERROR_SUBCLASS

protected:
    virtual void visit_children(Visitor&) override;

private:
    virtual const char* class_name() const override { return "GlobalObject"; }

    static Value gc(Interpreter&);
    static Value is_nan(Interpreter&);

    template<typename ConstructorType>
    void add_constructor(const FlyString& property_name, ConstructorType*&, Object& prototype);

    ArrayConstructor* m_array_constructor { nullptr };
    BooleanConstructor* m_boolean_constructor { nullptr };
    DateConstructor* m_date_constructor { nullptr };
    FunctionConstructor* m_function_constructor { nullptr };
    NumberConstructor* m_number_constructor { nullptr };
    ObjectConstructor* m_object_constructor { nullptr };
    ErrorConstructor* m_error_constructor { nullptr };

#define __JS_ENUMERATE_ERROR_SUBCLASS(TitleCase, snake_case) \
    TitleCase##Constructor* m_##snake_case##_constructor;
    JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE_ERROR_SUBCLASS
};

}
