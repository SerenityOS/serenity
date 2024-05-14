/*
 * Copyright (c) 2021-2024, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/SinglyLinkedList.h>
#include <LibJS/AST.h>
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
#include <LibRegex/Regex.h>

namespace JS::Bytecode {

class Generator {
public:
    VM& vm() { return m_vm; }

    enum class SurroundingScopeKind {
        Global,
        Function,
        Block,
    };

    enum class MustPropagateCompletion {
        No,
        Yes,
    };

    static CodeGenerationErrorOr<NonnullGCPtr<Executable>> generate_from_ast_node(VM&, ASTNode const&, FunctionKind = FunctionKind::Normal);
    static CodeGenerationErrorOr<NonnullGCPtr<Executable>> generate_from_function(VM&, ECMAScriptFunctionObject const& function);

    CodeGenerationErrorOr<void> emit_function_declaration_instantiation(ECMAScriptFunctionObject const& function);

    [[nodiscard]] ScopedOperand allocate_register();
    [[nodiscard]] ScopedOperand local(u32 local_index);
    [[nodiscard]] ScopedOperand accumulator();

    void free_register(Register);

    void set_local_initialized(u32 local_index);
    [[nodiscard]] bool is_local_initialized(u32 local_index) const;

    class SourceLocationScope {
    public:
        SourceLocationScope(Generator&, ASTNode const& node);
        ~SourceLocationScope();

    private:
        Generator& m_generator;
        ASTNode const* m_previous_node { nullptr };
    };

    class UnwindContext {
    public:
        UnwindContext(Generator&, Optional<Label> finalizer);

        UnwindContext const* previous() const { return m_previous_context; }
        void set_handler(Label handler) { m_handler = handler; }
        Optional<Label> handler() const { return m_handler; }
        Optional<Label> finalizer() const { return m_finalizer; }

        ~UnwindContext();

    private:
        Generator& m_generator;
        Optional<Label> m_finalizer;
        Optional<Label> m_handler {};
        UnwindContext const* m_previous_context { nullptr };
    };

    template<typename OpType, typename... Args>
    requires(requires { OpType(declval<Args>()...); })
    void emit(Args&&... args)
    {
        VERIFY(!is_current_block_terminated());
        size_t slot_offset = m_current_basic_block->size();
        m_current_basic_block->set_last_instruction_start_offset(slot_offset);
        grow(sizeof(OpType));
        void* slot = m_current_basic_block->data() + slot_offset;
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            m_current_basic_block->terminate({});
        m_current_basic_block->add_source_map_entry(slot_offset, { m_current_ast_node->start_offset(), m_current_ast_node->end_offset() });
    }

    template<typename OpType, typename ExtraSlotType, typename... Args>
    requires(requires { OpType(declval<Args>()...); })
    void emit_with_extra_slots(size_t extra_slot_count, Args&&... args)
    {
        VERIFY(!is_current_block_terminated());

        size_t size_to_allocate = round_up_to_power_of_two(sizeof(OpType) + extra_slot_count * sizeof(ExtraSlotType), alignof(void*));
        size_t slot_offset = m_current_basic_block->size();
        m_current_basic_block->set_last_instruction_start_offset(slot_offset);
        grow(size_to_allocate);
        void* slot = m_current_basic_block->data() + slot_offset;
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            m_current_basic_block->terminate({});
        m_current_basic_block->add_source_map_entry(slot_offset, { m_current_ast_node->start_offset(), m_current_ast_node->end_offset() });
    }

    template<typename OpType, typename... Args>
    requires(requires { OpType(declval<Args>()...); })
    void emit_with_extra_operand_slots(size_t extra_operand_slots, Args&&... args)
    {
        emit_with_extra_slots<OpType, Operand>(extra_operand_slots, forward<Args>(args)...);
    }

    template<typename OpType, typename... Args>
    requires(requires { OpType(declval<Args>()...); })
    void emit_with_extra_value_slots(size_t extra_operand_slots, Args&&... args)
    {
        emit_with_extra_slots<OpType, Value>(extra_operand_slots, forward<Args>(args)...);
    }

    void emit_jump_if(ScopedOperand const& condition, Label true_target, Label false_target);

    struct ReferenceOperands {
        Optional<ScopedOperand> base {};                                 // [[Base]]
        Optional<ScopedOperand> referenced_name {};                      // [[ReferencedName]] as an operand
        Optional<IdentifierTableIndex> referenced_identifier {};         // [[ReferencedName]] as an identifier
        Optional<IdentifierTableIndex> referenced_private_identifier {}; // [[ReferencedName]] as a private identifier
        Optional<ScopedOperand> this_value {};                           // [[ThisValue]]
        Optional<ScopedOperand> loaded_value {};                         // Loaded value, if we've performed a load.
    };

    CodeGenerationErrorOr<ReferenceOperands> emit_load_from_reference(JS::ASTNode const&, Optional<ScopedOperand> preferred_dst = {});
    CodeGenerationErrorOr<void> emit_store_to_reference(JS::ASTNode const&, ScopedOperand value);
    CodeGenerationErrorOr<void> emit_store_to_reference(ReferenceOperands const&, ScopedOperand value);
    CodeGenerationErrorOr<Optional<ScopedOperand>> emit_delete_reference(JS::ASTNode const&);

    CodeGenerationErrorOr<ReferenceOperands> emit_super_reference(MemberExpression const&);

    void emit_set_variable(JS::Identifier const& identifier, ScopedOperand value, Bytecode::Op::SetVariable::InitializationMode initialization_mode = Bytecode::Op::SetVariable::InitializationMode::Set, Bytecode::Op::EnvironmentMode mode = Bytecode::Op::EnvironmentMode::Lexical);

    void push_home_object(ScopedOperand);
    void pop_home_object();
    void emit_new_function(ScopedOperand dst, JS::FunctionExpression const&, Optional<IdentifierTableIndex> lhs_name);

    CodeGenerationErrorOr<Optional<ScopedOperand>> emit_named_evaluation_if_anonymous_function(Expression const&, Optional<IdentifierTableIndex> lhs_name, Optional<ScopedOperand> preferred_dst = {});

    void begin_continuable_scope(Label continue_target, Vector<DeprecatedFlyString> const& language_label_set);
    void end_continuable_scope();
    void begin_breakable_scope(Label breakable_target, Vector<DeprecatedFlyString> const& language_label_set);
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
            name = MUST(String::number(m_next_block++));
        auto block = BasicBlock::create(m_root_basic_blocks.size(), name);
        if (auto const* context = m_current_unwind_context) {
            if (context->handler().has_value())
                block->set_handler(*m_root_basic_blocks[context->handler().value().basic_block_index()]);
            if (m_current_unwind_context->finalizer().has_value())
                block->set_finalizer(*m_root_basic_blocks[context->finalizer().value().basic_block_index()]);
        }
        m_root_basic_blocks.append(move(block));
        return *m_root_basic_blocks.last();
    }

    bool is_current_block_terminated() const
    {
        return m_current_basic_block->is_terminated();
    }

    StringTableIndex intern_string(ByteString string)
    {
        return m_string_table->insert(move(string));
    }

    RegexTableIndex intern_regex(ParsedRegex regex)
    {
        return m_regex_table->insert(move(regex));
    }

    IdentifierTableIndex intern_identifier(DeprecatedFlyString string)
    {
        return m_identifier_table->insert(move(string));
    }

    Optional<IdentifierTableIndex> intern_identifier_for_expression(Expression const& expression);

    bool is_in_generator_or_async_function() const { return m_enclosing_function_kind == FunctionKind::Async || m_enclosing_function_kind == FunctionKind::Generator || m_enclosing_function_kind == FunctionKind::AsyncGenerator; }
    bool is_in_generator_function() const { return m_enclosing_function_kind == FunctionKind::Generator || m_enclosing_function_kind == FunctionKind::AsyncGenerator; }
    bool is_in_async_function() const { return m_enclosing_function_kind == FunctionKind::Async || m_enclosing_function_kind == FunctionKind::AsyncGenerator; }
    bool is_in_async_generator_function() const { return m_enclosing_function_kind == FunctionKind::AsyncGenerator; }

    enum class BindingMode {
        Lexical,
        Var,
        Global,
    };
    struct LexicalScope {
        SurroundingScopeKind kind;
    };

    // Returns true if a lexical environment was created.
    bool emit_block_declaration_instantiation(ScopeNode const&);

    void begin_variable_scope();
    void end_variable_scope();

    enum class BlockBoundaryType {
        Break,
        Continue,
        Unwind,
        ReturnToFinally,
        LeaveFinally,
        LeaveLexicalEnvironment,
    };
    template<typename OpType>
    void perform_needed_unwinds()
    requires(OpType::IsTerminator && !IsSame<OpType, Op::Jump>)
    {
        for (size_t i = m_boundaries.size(); i > 0; --i) {
            auto boundary = m_boundaries[i - 1];
            using enum BlockBoundaryType;
            switch (boundary) {
            case Unwind:
                if constexpr (IsSame<OpType, Bytecode::Op::Throw>)
                    return;
                emit<Bytecode::Op::LeaveUnwindContext>();
                break;
            case LeaveLexicalEnvironment:
                emit<Bytecode::Op::LeaveLexicalEnvironment>();
                break;
            case Break:
            case Continue:
                break;
            case ReturnToFinally:
                return;
            case LeaveFinally:
                emit<Bytecode::Op::LeaveFinally>();
                break;
            };
        }
    }

    bool is_in_finalizer() const { return m_boundaries.contains_slow(BlockBoundaryType::LeaveFinally); }

    void generate_break();
    void generate_break(DeprecatedFlyString const& break_label);

    void generate_continue();
    void generate_continue(DeprecatedFlyString const& continue_label);

    void start_boundary(BlockBoundaryType type) { m_boundaries.append(type); }
    void end_boundary(BlockBoundaryType type)
    {
        VERIFY(m_boundaries.last() == type);
        m_boundaries.take_last();
    }

    [[nodiscard]] ScopedOperand get_this(Optional<ScopedOperand> preferred_dst = {});

    void emit_get_by_id(ScopedOperand dst, ScopedOperand base, IdentifierTableIndex property_identifier, Optional<IdentifierTableIndex> base_identifier = {});

    void emit_get_by_id_with_this(ScopedOperand dst, ScopedOperand base, IdentifierTableIndex, ScopedOperand this_value);

    void emit_iterator_value(ScopedOperand dst, ScopedOperand result);
    void emit_iterator_complete(ScopedOperand dst, ScopedOperand result);

    [[nodiscard]] size_t next_global_variable_cache() { return m_next_global_variable_cache++; }
    [[nodiscard]] size_t next_property_lookup_cache() { return m_next_property_lookup_cache++; }

    enum class DeduplicateConstant {
        Yes,
        No,
    };
    [[nodiscard]] ScopedOperand add_constant(Value value, DeduplicateConstant deduplicate_constant = DeduplicateConstant::Yes)
    {
        if (deduplicate_constant == DeduplicateConstant::Yes) {
            for (size_t i = 0; i < m_constants.size(); ++i) {
                if (m_constants[i] == value)
                    return ScopedOperand(*this, Operand(Operand::Type::Constant, i));
            }
        }
        m_constants.append(value);
        return ScopedOperand(*this, Operand(Operand::Type::Constant, m_constants.size() - 1));
    }

    UnwindContext const* current_unwind_context() const { return m_current_unwind_context; }

    [[nodiscard]] bool is_finished() const { return m_finished; }

    [[nodiscard]] bool must_propagate_completion() const { return m_must_propagate_completion; }

private:
    VM& m_vm;

    static CodeGenerationErrorOr<NonnullGCPtr<Executable>> emit_function_body_bytecode(VM&, ASTNode const&, FunctionKind, GCPtr<ECMAScriptFunctionObject const>, MustPropagateCompletion = MustPropagateCompletion::Yes);

    enum class JumpType {
        Continue,
        Break,
    };
    void generate_scoped_jump(JumpType);
    void generate_labelled_jump(JumpType, DeprecatedFlyString const& label);

    Generator(VM&, MustPropagateCompletion);
    ~Generator() = default;

    void grow(size_t);

    // Returns true if a fused instruction was emitted.
    [[nodiscard]] bool fuse_compare_and_jump(ScopedOperand const& condition, Label true_target, Label false_target);

    struct LabelableScope {
        Label bytecode_target;
        Vector<DeprecatedFlyString> language_label_set;
    };

    BasicBlock* m_current_basic_block { nullptr };
    ASTNode const* m_current_ast_node { nullptr };
    UnwindContext const* m_current_unwind_context { nullptr };

    Vector<NonnullOwnPtr<BasicBlock>> m_root_basic_blocks;
    NonnullOwnPtr<StringTable> m_string_table;
    NonnullOwnPtr<IdentifierTable> m_identifier_table;
    NonnullOwnPtr<RegexTable> m_regex_table;
    MarkedVector<Value> m_constants;

    ScopedOperand m_accumulator;
    Vector<Register> m_free_registers;

    u32 m_next_register { Register::reserved_register_count };
    u32 m_next_block { 1 };
    u32 m_next_property_lookup_cache { 0 };
    u32 m_next_global_variable_cache { 0 };
    FunctionKind m_enclosing_function_kind { FunctionKind::Normal };
    Vector<LabelableScope> m_continuable_scopes;
    Vector<LabelableScope> m_breakable_scopes;
    Vector<BlockBoundaryType> m_boundaries;
    Vector<ScopedOperand> m_home_objects;

    HashTable<u32> m_initialized_locals;

    bool m_finished { false };
    bool m_must_propagate_completion { true };
};

}
