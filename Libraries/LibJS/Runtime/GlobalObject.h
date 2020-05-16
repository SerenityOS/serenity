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

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class GlobalObject : public Object {
public:
    explicit GlobalObject();
    virtual void initialize();

    virtual ~GlobalObject() override;

    Shape* empty_object_shape() { return m_empty_object_shape; }

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)            \
    ConstructorName* snake_name##_constructor() { return m_##snake_name##_constructor; } \
    Object* snake_name##_prototype() { return m_##snake_name##_prototype; }
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

protected:
    virtual void visit_children(Visitor&) override;

    template<typename ConstructorType>
    void add_constructor(const FlyString& property_name, ConstructorType*&, Object& prototype);

private:
    virtual const char* class_name() const override { return "GlobalObject"; }

    static Value gc(Interpreter&);
    static Value is_nan(Interpreter&);
    static Value is_finite(Interpreter&);

    Shape* m_empty_object_shape { nullptr };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    ConstructorName* m_##snake_name##_constructor { nullptr };                \
    Object* m_##snake_name##_prototype { nullptr };
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE
};

template<typename ConstructorType>
inline void GlobalObject::add_constructor(const FlyString& property_name, ConstructorType*& constructor, Object& prototype)
{
    constructor = heap().allocate<ConstructorType>();
    constructor->put("name", js_string(heap(), property_name), Attribute::Configurable);
    prototype.put("constructor", constructor, Attribute::Writable | Attribute::Configurable);
    put(property_name, constructor, Attribute::Writable | Attribute::Configurable);
}

}
