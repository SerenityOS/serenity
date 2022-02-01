/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

struct Variable {
    Value value;
    DeclarationKind declaration_kind;
};

#define JS_ENVIRONMENT(class_, base_class) \
public:                                    \
    using Base = base_class;               \
    virtual char const* class_name() const override { return #class_; }

class Environment : public Cell {
public:
    virtual bool has_this_binding() const { return false; }
    virtual ThrowCompletionOr<Value> get_this_binding(GlobalObject&) const { return Value {}; }

    virtual Object* with_base_object() const { return nullptr; }

    virtual ThrowCompletionOr<bool> has_binding([[maybe_unused]] FlyString const& name, [[maybe_unused]] Optional<size_t>* out_index = nullptr) const { return false; }
    virtual ThrowCompletionOr<void> create_mutable_binding(GlobalObject&, [[maybe_unused]] FlyString const& name, [[maybe_unused]] bool can_be_deleted) { return {}; }
    virtual ThrowCompletionOr<void> create_immutable_binding(GlobalObject&, [[maybe_unused]] FlyString const& name, [[maybe_unused]] bool strict) { return {}; }
    virtual ThrowCompletionOr<void> initialize_binding(GlobalObject&, [[maybe_unused]] FlyString const& name, Value) { return {}; }
    virtual ThrowCompletionOr<void> set_mutable_binding(GlobalObject&, [[maybe_unused]] FlyString const& name, Value, [[maybe_unused]] bool strict) { return {}; }
    virtual ThrowCompletionOr<Value> get_binding_value(GlobalObject&, [[maybe_unused]] FlyString const& name, [[maybe_unused]] bool strict) { return Value {}; }
    virtual ThrowCompletionOr<bool> delete_binding(GlobalObject&, [[maybe_unused]] FlyString const& name) { return false; }

    // [[OuterEnv]]
    Environment* outer_environment() { return m_outer_environment; }
    Environment const* outer_environment() const { return m_outer_environment; }

    virtual bool is_global_environment() const { return false; }
    virtual bool is_declarative_environment() const { return false; }
    virtual bool is_function_environment() const { return false; }

    template<typename T>
    bool fast_is() const = delete;

    virtual char const* class_name() const override { return "Environment"; }

    // This flag is set on the entire variable environment chain when direct eval() is performed.
    // It is used to disable non-local variable access caching.
    bool is_permanently_screwed_by_eval() const { return m_permanently_screwed_by_eval; }
    void set_permanently_screwed_by_eval();

protected:
    explicit Environment(Environment* parent);

    virtual void visit_edges(Visitor&) override;

private:
    virtual bool is_environment() const final { return true; }

    bool m_permanently_screwed_by_eval { false };

    Environment* m_outer_environment { nullptr };
};

}
