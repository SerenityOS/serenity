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

        size_t size_to_allocate = round_up_to_power_of_two(sizeof(OpType) + extra_register_slots * sizeof(Register), alignof(void*));

        // If the block doesn't have enough space, switch to another block
        if constexpr (!OpType::IsTerminator)
            ensure_enough_space(size_to_allocate);

        void* slot = next_slot();
        grow(size_to_allocate);
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            m_current_basic_block->terminate({});
        return *static_cast<OpType*>(slot);
    }

    CodeGenerationErrorOr<void> emit_load_from_reference(JS::ASTNode const&);
    CodeGenerationErrorOr<void> emit_store_to_reference(JS::ASTNode const&);
    CodeGenerationErrorOr<void> emit_delete_reference(JS::ASTNode const&);

    void begin_continuable_scope(Label continue_target, Vector<FlyString> const& language_label_set);
    void end_continuable_scope();
    void begin_breakable_scope(Label breakable_target, Vector<FlyString> const& language_label_set);
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
    bool has_binding(IdentifierTableIndex identifier, Optional<BindingMode> const& specific_binding_mode = {}) const
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
    bool has_binding_in_current_scope(IdentifierTableIndex identifier) const
    {
        if (m_variable_scopes.is_empty())
            return false;

        return m_variable_scopes.last().known_bindings.contains(identifier);
    }

    void begin_variable_scope(BindingMode mode = BindingMode::Lexical, SurroundingScopeKind kind = SurroundingScopeKind::Block);
    void end_variable_scope();

    enum class BlockBoundaryType {
        Break,
        Continue,
        Unwind,
        LeaveLexicalEnvironment,
        LeaveVariableEnvironment,
    };
    template<typename OpType>
    void perform_needed_unwinds(bool is_break_node = false) requires(OpType::IsTerminator)
    {
        Optional<BlockBoundaryType> boundary_to_stop_at;
        if constexpr (IsSame<OpType, Bytecode::Op::Return> || IsSame<OpType, Bytecode::Op::Yield>)
            VERIFY(!is_break_node);
        else if constexpr (IsSame<OpType, Bytecode::Op::Throw>)
            boundary_to_stop_at = BlockBoundaryType::Unwind;
        else
            boundary_to_stop_at = is_break_node ? BlockBoundaryType::Break : BlockBoundaryType::Continue;

        for (size_t i = m_boundaries.size(); i > 0; --i) {
            auto boundary = m_boundaries[i - 1];
            if (boundary_to_stop_at.has_value() && boundary == *boundary_to_stop_at)
                break;
            if (boundary == BlockBoundaryType::Unwind)
                emit<Bytecode::Op::LeaveUnwindContext>();
            else if (boundary == BlockBoundaryType::LeaveLexicalEnvironment)
                emit<Bytecode::Op::LeaveEnvironment>(Bytecode::Op::EnvironmentMode::Lexical);
            else if (boundary == BlockBoundaryType::LeaveVariableEnvironment)
                emit<Bytecode::Op::LeaveEnvironment>(Bytecode::Op::EnvironmentMode::Var);
        }
    }

    Label perform_needed_unwinds_for_labelled_break_and_return_target_block(FlyString const& break_label);
    Label perform_needed_unwinds_for_labelled_continue_and_return_target_block(FlyString const& continue_label);

    void start_boundary(BlockBoundaryType type) { m_boundaries.append(type); }
    void end_boundary(BlockBoundaryType type)
    {
        VERIFY(m_boundaries.last() == type);
        m_boundaries.take_last();
    }

private:
    Generator();
    ~Generator() = default;

    void grow(size_t);
    void* next_slot();

    struct LabelableScope {
        Label bytecode_target;
        Vector<FlyString> language_label_set;
    };

    BasicBlock* m_current_basic_block { nullptr };
    NonnullOwnPtrVector<BasicBlock> m_root_basic_blocks;
    NonnullOwnPtr<StringTable> m_string_table;
    NonnullOwnPtr<IdentifierTable> m_identifier_table;

    u32 m_next_register { 2 };
    u32 m_next_block { 1 };
    FunctionKind m_enclosing_function_kind { FunctionKind::Normal };
    Vector<LabelableScope> m_continuable_scopes;
    Vector<LabelableScope> m_breakable_scopes;
    Vector<LexicalScope> m_variable_scopes;
    Vector<BlockBoundaryType> m_boundaries;
};

}
