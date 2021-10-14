/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Weakable.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

// 9.3 Realms, https://tc39.es/ecma262/#realm-record
class Realm final
    : public Cell
    , public Weakable<Realm> {
public:
    Realm() = default;

    // 9.3.1 CreateRealm ( ), https://tc39.es/ecma262/#sec-createrealm
    static Realm* create(VM& vm)
    {
        return vm.heap().allocate_without_global_object<Realm>();
    }

    void set_global_object(GlobalObject&, Object* this_value = nullptr);

    [[nodiscard]] GlobalObject& global_object() const { return *m_global_object; }
    [[nodiscard]] GlobalEnvironment& global_environment() const { return *m_global_environment; }

private:
    virtual char const* class_name() const override { return "Realm"; }
    virtual void visit_edges(Visitor&) override;

    GlobalObject* m_global_object { nullptr };           // [[GlobalObject]]
    GlobalEnvironment* m_global_environment { nullptr }; // [[GlobalEnv]]
};

}
