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

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

struct PropertyMetadata {
    size_t offset { 0 };
    u8 attributes { 0 };
};

class Shape final : public Cell {
public:
    virtual ~Shape() override;

    Shape();
    Shape(Shape* previous_shape, const FlyString& property_name, u8 property_attributes);
    Shape(Shape* previous_shape, Object* new_prototype);

    Shape* create_put_transition(const FlyString& name, u8 attributes);
    Shape* create_prototype_transition(Object* new_prototype);

    Object* prototype() { return m_prototype; }
    const Object* prototype() const { return m_prototype; }

    Optional<PropertyMetadata> lookup(const FlyString&) const;
    const HashMap<FlyString, PropertyMetadata>& property_table() const;
    size_t property_count() const;

    void set_prototype_without_transition(Object* new_prototype) { m_prototype = new_prototype; }

private:
    virtual const char* class_name() const override { return "Shape"; }
    virtual void visit_children(Visitor&) override;

    void ensure_property_table() const;

    mutable OwnPtr<HashMap<FlyString, PropertyMetadata>> m_property_table;

    HashMap<FlyString, Shape*> m_forward_transitions;
    Shape* m_previous { nullptr };
    FlyString m_property_name;
    u8 m_property_attributes { 0 };
    Object* m_prototype { nullptr };
};

}
