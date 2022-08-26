/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/OwnPtr.h>
#include <AK/StringView.h>
#include <AK/Weakable.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Intrinsics.h>

namespace JS {

// 9.3 Realms, https://tc39.es/ecma262/#realm-record
class Realm final
    : public Cell
    , public Weakable<Realm> {
public:
    struct HostDefined {
        virtual ~HostDefined() = default;
    };

    Realm() = default;

    static Realm* create(VM&);
    static ThrowCompletionOr<NonnullOwnPtr<ExecutionContext>> initialize_host_defined_realm(VM&, Function<GlobalObject*(Realm&)> create_global_object, Function<GlobalObject*(Realm&)> create_global_this_value);

    void set_global_object(GlobalObject* global_object, GlobalObject* this_value);

    [[nodiscard]] GlobalObject& global_object() const { return *m_global_object; }
    [[nodiscard]] GlobalEnvironment& global_environment() const { return *m_global_environment; }

    [[nodiscard]] Intrinsics const& intrinsics() const { return *m_intrinsics; }
    [[nodiscard]] Intrinsics& intrinsics() { return *m_intrinsics; }
    void set_intrinsics(Badge<Intrinsics>, Intrinsics& intrinsics)
    {
        VERIFY(!m_intrinsics);
        m_intrinsics = &intrinsics;
    }

    HostDefined* host_defined() { return m_host_defined; }
    void set_host_defined(OwnPtr<HostDefined> host_defined) { m_host_defined = move(host_defined); }

private:
    virtual StringView class_name() const override { return "Realm"sv; }
    virtual void visit_edges(Visitor&) override;

    Intrinsics* m_intrinsics { nullptr };                // [[Intrinsics]]
    GlobalObject* m_global_object { nullptr };           // [[GlobalObject]]
    GlobalEnvironment* m_global_environment { nullptr }; // [[GlobalEnv]]
    OwnPtr<HostDefined> m_host_defined;                  // [[HostDefined]]
};

}
