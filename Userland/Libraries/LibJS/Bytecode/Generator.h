/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/SinglyLinkedList.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/CodeGenerationError.h>
#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/FunctionKind.h>

namespace JS::Bytecode {

class Generator {
public:
    enum class SurroundingScopeKind {
        Global,
        Function,
        Block,
    };
    static CodeGenerationErrorOr<NonnullOwnPtr<Executable>> generate(ASTNode const&, FunctionKind = FunctionKind::Normal);

    Register allocate_register();

    void ensure_enough_space(size_t size)
    {
        // Make sure there's always enough space for a single jump at the end.
        if (!m_current_basic_block->can_grow(size + sizeof(Op::Jump))) {
            auto& new_block = make_block();
            emit<Op::Jump>().set_targets(
                Label { new_block },
                {});
            switch_to_basic_block(new_block);
        }
    }

    template<typename OpType, typename... Args>
    OpType& emit(Args&&... args)
    {
        VERIFY(!is_current_block_terminated());
        // If the block doesn't have enough space, switch to another block
        if constexpr (!OpType::IsTerminator)
            ensure_enough_space(sizeof(OpType));

        void* slot = next_slot();
        grow(sizeof(OpType));
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            m_current_basic_block->terminate({});
        return *static_cast<OpType*>(slot);
    }

    template<typename OpType, typename... Args>
    OpType& emit_with_extra_register_slots(size_t extra_register_slots, Args&&... args)
    {
        VERIFY(!is_current_block_terminated());
        // If the block doesn't have enough space, switch to another block
        if constexpr (!OpType::IsTerminator)
            ensure_enough_space(sizeof(OpType) + extra_register_slots * sizeof(Register));

        void* slot = next_slot();
        grow(sizeof(OpType) + extra_register_slots * sizeof(Register));
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            m_current_basic_block->terminate({});
        return *static_cast<OpType*>(slot);
    }

    CodeGenerationErrorOr<void> emit_load_from_reference(JS::ASTNode const&);
    CodeGenerationErrorOr<void> emit_store_to_reference(JS::ASTNode const&);

    void begin_continuable_scope(Label continue_target);
    void end_continuable_scope();
    void begin_breakable_scope(Label breakable_target);
    void end_breakable_scope();

    [[nodiscard]] Label nearest_continuable_scope() const;
    [[nodiscard]] Label nearest_breakable_scope() const;

    void switch_to_basic_block(BasicBlock& block)
    {
        m_current_basic_block = &block;
    }

    [[nodiscard]] BasicBlock& current_block() { return *m_current_basic_block; }

    BasicBlock& make_block(String name = {})
    {
        if (name.is_empty())
            name = String::number(m_next_block++);
        m_root_basic_blocks.append(BasicBlock::create(name));
        return m_root_basic_blocks.last();
    }

    bool is_current_block_terminated() const
    {
        return m_current_basic_block->is_terminated();
    }

    StringTableIndex intern_string(String string)
    {
        return m_string_table->insert(move(string));
    }

    IdentifierTableIndex intern_identifier(FlyString string)
    {
        return m_identifier_table->insert(move(string));
    }

    bool is_in_generator_or_async_function() const { return m_enclosing_function_kind == FunctionKind::Async || m_enclosing_function_kind == FunctionKind::Generator; }
    bool is_in_generator_function() const { return m_enclosing_function_kind == FunctionKind::Generator; }
    bool is_in_async_function() const { return m_enclosing_function_kind == FunctionKind::Async; }

    enum class BindingMode {
        Lexical,
        Var,
        Global,
    };
    struct LexicalScope {
        SurroundingScopeKind kind;
        BindingMode mode;
        HashTable<IdentifierTableIndex> known_bindings;
    };

    void register_binding(IdentifierTableIndex identifier, BindingMode mode = BindingMode::Lexical)
    {
        m_variable_scopes.last_matching([&](auto& x) { return x.mode == BindingMode::Global || x.mode == mode; })->known_bindings.set(identifier);
    }
    bool has_binding(IdentifierTableIndex identifier, Optional<BindingMode> const& specific_binding_mode = {})
    {
        for (auto index = m_variable_scopes.size(); index > 0; --index) {
            auto& scope = m_variable_scopes[index - 1];

            if (scope.mode != BindingMode::Global && specific_binding_mode.value_or(scope.mode) != scope.mode)
                continue;

            if (scope.known_bindings.contains(identifier))
                return true;
        }
        return false;
    }
    void begin_variable_scope(BindingMode mode = BindingMode::Lexical, SurroundingScopeKind kind = SurroundingScopeKind::Block)
    {
        m_variable_scopes.append({ kind, mode, {} });
        if (mode != BindingMode::Global) {
            emit<Bytecode::Op::CreateEnvironment>(
                mode == BindingMode::Lexical
                    ? Bytecode::Op::EnvironmentMode::Lexical
                    : Bytecode::Op::EnvironmentMode::Var);
        }
    }
    void end_variable_scope()
    {
        auto mode = m_variable_scopes.take_last().mode;
        if (mode != BindingMode::Global && !m_current_basic_block->is_terminated()) {
            emit<Bytecode::Op::LeaveEnvironment>(
                mode == BindingMode::Lexical
                    ? Bytecode::Op::EnvironmentMode::Lexical
                    : Bytecode::Op::EnvironmentMode::Var);
        }
    }

private:
    Generator();
    ~Generator();

    void grow(size_t);
    void* next_slot();

    BasicBlock* m_current_basic_block { nullptr };
    NonnullOwnPtrVector<BasicBlock> m_root_basic_blocks;
    NonnullOwnPtr<StringTable> m_string_table;
    NonnullOwnPtr<IdentifierTable> m_identifier_table;

    u32 m_next_register { 2 };
    u32 m_next_block { 1 };
    FunctionKind m_enclosing_function_kind { FunctionKind::Normal };
    Vector<Label> m_continuable_scopes;
    Vector<Label> m_breakable_scopes;
    Vector<LexicalScope> m_variable_scopes;
};

}
