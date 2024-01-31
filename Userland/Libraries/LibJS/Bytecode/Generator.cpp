/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Runtime/VM.h>

namespace JS::Bytecode {

Generator::Generator()
    : m_string_table(make<StringTable>())
    , m_identifier_table(make<IdentifierTable>())
    , m_regex_table(make<RegexTable>())
{
}

CodeGenerationErrorOr<NonnullGCPtr<Executable>> Generator::generate(VM& vm, ASTNode const& node, FunctionKind enclosing_function_kind)
{
    Generator generator;
    generator.switch_to_basic_block(generator.make_block());
    SourceLocationScope scope(generator, node);
    generator.m_enclosing_function_kind = enclosing_function_kind;
    if (generator.is_in_generator_or_async_function()) {
        // Immediately yield with no value.
        auto& start_block = generator.make_block();
        generator.emit<Bytecode::Op::Yield>(Label { start_block });
        generator.switch_to_basic_block(start_block);
        // NOTE: This doesn't have to handle received throw/return completions, as GeneratorObject::resume_abrupt
        //       will not enter the generator from the SuspendedStart state and immediately completes the generator.
    }
    TRY(node.generate_bytecode(generator));
    if (generator.is_in_generator_or_async_function()) {
        // Terminate all unterminated blocks with yield return
        for (auto& block : generator.m_root_basic_blocks) {
            if (block->is_terminated())
                continue;
            generator.switch_to_basic_block(*block);
            generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
            generator.emit<Bytecode::Op::Yield>(nullptr);
        }
    }

    bool is_strict_mode = false;
    if (is<Program>(node))
        is_strict_mode = static_cast<Program const&>(node).is_strict_mode();
    else if (is<FunctionBody>(node))
        is_strict_mode = static_cast<FunctionBody const&>(node).in_strict_mode();
    else if (is<FunctionDeclaration>(node))
        is_strict_mode = static_cast<FunctionDeclaration const&>(node).is_strict_mode();
    else if (is<FunctionExpression>(node))
        is_strict_mode = static_cast<FunctionExpression const&>(node).is_strict_mode();

    auto executable = vm.heap().allocate_without_realm<Executable>(
        move(generator.m_identifier_table),
        move(generator.m_string_table),
        move(generator.m_regex_table),
        node.source_code(),
        generator.m_next_property_lookup_cache,
        generator.m_next_global_variable_cache,
        generator.m_next_environment_variable_cache,
        generator.m_next_register,
        move(generator.m_root_basic_blocks),
        is_strict_mode);

    return executable;
}

void Generator::grow(size_t additional_size)
{
    VERIFY(m_current_basic_block);
    m_current_basic_block->grow(additional_size);
}

Register Generator::allocate_register()
{
    VERIFY(m_next_register != NumericLimits<u32>::max());
    return Register { m_next_register++ };
}

Generator::SourceLocationScope::SourceLocationScope(Generator& generator, ASTNode const& node)
    : m_generator(generator)
    , m_previous_node(m_generator.m_current_ast_node)
{
    m_generator.m_current_ast_node = &node;
}

Generator::SourceLocationScope::~SourceLocationScope()
{
    m_generator.m_current_ast_node = m_previous_node;
}

Generator::UnwindContext::UnwindContext(Generator& generator, Optional<Label> finalizer)
    : m_generator(generator)
    , m_finalizer(finalizer)
    , m_previous_context(m_generator.m_current_unwind_context)
{
    m_generator.m_current_unwind_context = this;
}

Generator::UnwindContext::~UnwindContext()
{
    VERIFY(m_generator.m_current_unwind_context == this);
    m_generator.m_current_unwind_context = m_previous_context;
}

Label Generator::nearest_continuable_scope() const
{
    return m_continuable_scopes.last().bytecode_target;
}

void Generator::block_declaration_instantiation(ScopeNode const& scope_node)
{
    start_boundary(BlockBoundaryType::LeaveLexicalEnvironment);
    emit<Bytecode::Op::BlockDeclarationInstantiation>(scope_node);
}

void Generator::begin_variable_scope()
{
    start_boundary(BlockBoundaryType::LeaveLexicalEnvironment);
    emit<Bytecode::Op::CreateLexicalEnvironment>();
}

void Generator::end_variable_scope()
{
    end_boundary(BlockBoundaryType::LeaveLexicalEnvironment);

    if (!m_current_basic_block->is_terminated()) {
        emit<Bytecode::Op::LeaveLexicalEnvironment>();
    }
}

void Generator::begin_continuable_scope(Label continue_target, Vector<DeprecatedFlyString> const& language_label_set)
{
    m_continuable_scopes.append({ continue_target, language_label_set });
    start_boundary(BlockBoundaryType::Continue);
}

void Generator::end_continuable_scope()
{
    m_continuable_scopes.take_last();
    end_boundary(BlockBoundaryType::Continue);
}

Label Generator::nearest_breakable_scope() const
{
    return m_breakable_scopes.last().bytecode_target;
}

void Generator::begin_breakable_scope(Label breakable_target, Vector<DeprecatedFlyString> const& language_label_set)
{
    m_breakable_scopes.append({ breakable_target, language_label_set });
    start_boundary(BlockBoundaryType::Break);
}

void Generator::end_breakable_scope()
{
    m_breakable_scopes.take_last();
    end_boundary(BlockBoundaryType::Break);
}

CodeGenerationErrorOr<Generator::ReferenceRegisters> Generator::emit_super_reference(MemberExpression const& expression)
{
    VERIFY(is<SuperExpression>(expression.object()));

    // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
    // 1. Let env be GetThisEnvironment().
    // 2. Let actualThis be ? env.GetThisBinding().
    auto actual_this_register = allocate_register();
    emit<Bytecode::Op::ResolveThisBinding>();
    emit<Bytecode::Op::Store>(actual_this_register);

    Optional<Bytecode::Register> computed_property_value_register;

    if (expression.is_computed()) {
        // SuperProperty : super [ Expression ]
        // 3. Let propertyNameReference be ? Evaluation of Expression.
        // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
        TRY(expression.property().generate_bytecode(*this));
        computed_property_value_register = allocate_register();
        emit<Bytecode::Op::Store>(*computed_property_value_register);
    }

    // 5/7. Return ? MakeSuperPropertyReference(actualThis, propertyKey, strict).

    // https://tc39.es/ecma262/#sec-makesuperpropertyreference
    // 1. Let env be GetThisEnvironment().
    // 2. Assert: env.HasSuperBinding() is true.
    // 3. Let baseValue be ? env.GetSuperBase().
    auto super_base_register = allocate_register();
    emit<Bytecode::Op::ResolveSuperBase>();
    emit<Bytecode::Op::Store>(super_base_register);

    // 4. Return the Reference Record { [[Base]]: baseValue, [[ReferencedName]]: propertyKey, [[Strict]]: strict, [[ThisValue]]: actualThis }.
    return ReferenceRegisters {
        .base = super_base_register,
        .referenced_name = move(computed_property_value_register),
        .this_value = actual_this_register,
    };
}

CodeGenerationErrorOr<Optional<Generator::ReferenceRegisters>> Generator::emit_load_from_reference(JS::ASTNode const& node, CollectRegisters collect_registers)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        TRY(identifier.generate_bytecode(*this));
        return Optional<ReferenceRegisters> {};
    }
    if (is<MemberExpression>(node)) {
        auto& expression = static_cast<MemberExpression const&>(node);

        // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
        if (is<SuperExpression>(expression.object())) {
            auto super_reference = TRY(emit_super_reference(expression));

            if (super_reference.referenced_name.has_value()) {
                // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
                // FIXME: This does ToPropertyKey out of order, which is observable by Symbol.toPrimitive!
                emit<Bytecode::Op::Load>(*super_reference.referenced_name);
                emit<Bytecode::Op::GetByValueWithThis>(super_reference.base, super_reference.this_value);
            } else {
                // 3. Let propertyKey be StringValue of IdentifierName.
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit_get_by_id_with_this(identifier_table_ref, super_reference.this_value);
            }

            return super_reference;
        } else {
            TRY(expression.object().generate_bytecode(*this));
            if (expression.is_computed()) {
                auto object_reg = allocate_register();
                emit<Bytecode::Op::Store>(object_reg);

                TRY(expression.property().generate_bytecode(*this));
                Optional<Register> property_reg {};
                if (collect_registers == CollectRegisters::Yes) {
                    property_reg = allocate_register();
                    emit<Bytecode::Op::Store>(property_reg.value());
                }

                emit<Bytecode::Op::GetByValue>(object_reg);
                if (collect_registers == CollectRegisters::Yes)
                    return ReferenceRegisters {
                        .base = object_reg,
                        .referenced_name = property_reg.value(),
                        .this_value = object_reg,
                    };
                return Optional<ReferenceRegisters> {};
            } else if (expression.property().is_identifier()) {
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit_get_by_id(identifier_table_ref);
            } else if (expression.property().is_private_identifier()) {
                auto identifier_table_ref = intern_identifier(verify_cast<PrivateIdentifier>(expression.property()).string());
                emit<Bytecode::Op::GetPrivateById>(identifier_table_ref);
            } else {
                return CodeGenerationError {
                    &expression,
                    "Unimplemented non-computed member expression"sv
                };
            }
        }
        return Optional<ReferenceRegisters> {};
    }
    return CodeGenerationError {
        &node,
        "Unimplemented/invalid node used a reference"sv
    };
}

CodeGenerationErrorOr<void> Generator::emit_store_to_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        emit_set_variable(identifier);
        return {};
    }
    if (is<MemberExpression>(node)) {
        // NOTE: The value is in the accumulator, so we have to store that away first.
        auto value_reg = allocate_register();
        emit<Bytecode::Op::Store>(value_reg);

        auto& expression = static_cast<MemberExpression const&>(node);

        // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
        if (is<SuperExpression>(expression.object())) {
            auto super_reference = TRY(emit_super_reference(expression));
            emit<Bytecode::Op::Load>(value_reg);

            // 4. Return the Reference Record { [[Base]]: baseValue, [[ReferencedName]]: propertyKey, [[Strict]]: strict, [[ThisValue]]: actualThis }.
            if (super_reference.referenced_name.has_value()) {
                // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
                // FIXME: This does ToPropertyKey out of order, which is observable by Symbol.toPrimitive!
                emit<Bytecode::Op::PutByValueWithThis>(super_reference.base, *super_reference.referenced_name, super_reference.this_value);
            } else {
                // 3. Let propertyKey be StringValue of IdentifierName.
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit<Bytecode::Op::PutByIdWithThis>(super_reference.base, super_reference.this_value, identifier_table_ref, Bytecode::Op::PropertyKind::KeyValue, next_property_lookup_cache());
            }
        } else {
            TRY(expression.object().generate_bytecode(*this));

            auto object_reg = allocate_register();
            emit<Bytecode::Op::Store>(object_reg);

            if (expression.is_computed()) {
                TRY(expression.property().generate_bytecode(*this));
                auto property_reg = allocate_register();
                emit<Bytecode::Op::Store>(property_reg);
                emit<Bytecode::Op::Load>(value_reg);
                emit<Bytecode::Op::PutByValue>(object_reg, property_reg);
            } else if (expression.property().is_identifier()) {
                emit<Bytecode::Op::Load>(value_reg);
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit<Bytecode::Op::PutById>(object_reg, identifier_table_ref, Bytecode::Op::PropertyKind::KeyValue, next_property_lookup_cache());
            } else if (expression.property().is_private_identifier()) {
                emit<Bytecode::Op::Load>(value_reg);
                auto identifier_table_ref = intern_identifier(verify_cast<PrivateIdentifier>(expression.property()).string());
                emit<Bytecode::Op::PutPrivateById>(object_reg, identifier_table_ref);
            } else {
                return CodeGenerationError {
                    &expression,
                    "Unimplemented non-computed member expression"sv
                };
            }
        }

        return {};
    }

    return CodeGenerationError {
        &node,
        "Unimplemented/invalid node used a reference"sv
    };
}

CodeGenerationErrorOr<void> Generator::emit_store_to_reference(ReferenceRegisters const& reference_registers)
{
    if (reference_registers.base == reference_registers.this_value)
        emit<Bytecode::Op::PutByValue>(reference_registers.base, reference_registers.referenced_name.value());
    else
        emit<Bytecode::Op::PutByValueWithThis>(reference_registers.base, reference_registers.referenced_name.value(), reference_registers.this_value);
    return {};
}

CodeGenerationErrorOr<void> Generator::emit_delete_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        if (identifier.is_local())
            emit<Bytecode::Op::LoadImmediate>(Value(false));
        else
            emit<Bytecode::Op::DeleteVariable>(intern_identifier(identifier.string()));
        return {};
    }

    if (is<MemberExpression>(node)) {
        auto& expression = static_cast<MemberExpression const&>(node);

        // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
        if (is<SuperExpression>(expression.object())) {
            auto super_reference = TRY(emit_super_reference(expression));

            if (super_reference.referenced_name.has_value()) {
                emit<Bytecode::Op::DeleteByValueWithThis>(super_reference.this_value, *super_reference.referenced_name);
            } else {
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit<Bytecode::Op::DeleteByIdWithThis>(super_reference.this_value, identifier_table_ref);
            }

            return {};
        }

        TRY(expression.object().generate_bytecode(*this));

        if (expression.is_computed()) {
            auto object_reg = allocate_register();
            emit<Bytecode::Op::Store>(object_reg);

            TRY(expression.property().generate_bytecode(*this));
            emit<Bytecode::Op::DeleteByValue>(object_reg);
        } else if (expression.property().is_identifier()) {
            auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
            emit<Bytecode::Op::DeleteById>(identifier_table_ref);
        } else {
            // NOTE: Trying to delete a private field generates a SyntaxError in the parser.
            return CodeGenerationError {
                &expression,
                "Unimplemented non-computed member expression"sv
            };
        }
        return {};
    }

    // Though this will have no deletion effect, we still have to evaluate the node as it can have side effects.
    // For example: delete a(); delete ++c.b; etc.

    // 13.5.1.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-delete-operator-runtime-semantics-evaluation
    // 1. Let ref be the result of evaluating UnaryExpression.
    // 2. ReturnIfAbrupt(ref).
    TRY(node.generate_bytecode(*this));

    // 3. If ref is not a Reference Record, return true.
    emit<Bytecode::Op::LoadImmediate>(Value(true));

    // NOTE: The rest of the steps are handled by Delete{Variable,ByValue,Id}.
    return {};
}

void Generator::emit_set_variable(JS::Identifier const& identifier, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Op::EnvironmentMode mode)
{
    if (identifier.is_local()) {
        emit<Bytecode::Op::SetLocal>(identifier.local_variable_index());
    } else {
        emit<Bytecode::Op::SetVariable>(intern_identifier(identifier.string()), next_environment_variable_cache(), initialization_mode, mode);
    }
}

void Generator::generate_scoped_jump(JumpType type)
{
    TemporaryChange temp { m_current_unwind_context, m_current_unwind_context };
    bool last_was_finally = false;
    for (size_t i = m_boundaries.size(); i > 0; --i) {
        auto boundary = m_boundaries[i - 1];
        using enum BlockBoundaryType;
        switch (boundary) {
        case Break:
            if (type == JumpType::Break) {
                emit<Op::Jump>(nearest_breakable_scope());
                return;
            }
            break;
        case Continue:
            if (type == JumpType::Continue) {
                emit<Op::Jump>(nearest_continuable_scope());
                return;
            }
            break;
        case Unwind:
            VERIFY(last_was_finally || !m_current_unwind_context->finalizer().has_value());
            if (!last_was_finally) {
                VERIFY(m_current_unwind_context && m_current_unwind_context->handler().has_value());
                emit<Bytecode::Op::LeaveUnwindContext>();
                m_current_unwind_context = m_current_unwind_context->previous();
            }
            last_was_finally = false;
            break;
        case LeaveLexicalEnvironment:
            emit<Bytecode::Op::LeaveLexicalEnvironment>();
            break;
        case ReturnToFinally: {
            VERIFY(m_current_unwind_context->finalizer().has_value());
            m_current_unwind_context = m_current_unwind_context->previous();
            auto jump_type_name = type == JumpType::Break ? "break"sv : "continue"sv;
            auto block_name = MUST(String::formatted("{}.{}", current_block().name(), jump_type_name));
            auto& block = make_block(block_name);
            emit<Op::ScheduleJump>(Label { block });
            switch_to_basic_block(block);
            last_was_finally = true;
            break;
        };
        }
    }
    VERIFY_NOT_REACHED();
}

void Generator::generate_labelled_jump(JumpType type, DeprecatedFlyString const& label)
{
    TemporaryChange temp { m_current_unwind_context, m_current_unwind_context };
    size_t current_boundary = m_boundaries.size();
    bool last_was_finally = false;

    auto const& jumpable_scopes = type == JumpType::Continue ? m_continuable_scopes : m_breakable_scopes;

    for (auto const& jumpable_scope : jumpable_scopes.in_reverse()) {
        for (; current_boundary > 0; --current_boundary) {
            auto boundary = m_boundaries[current_boundary - 1];
            if (boundary == BlockBoundaryType::Unwind) {
                VERIFY(last_was_finally || !m_current_unwind_context->finalizer().has_value());
                if (!last_was_finally) {
                    VERIFY(m_current_unwind_context && m_current_unwind_context->handler().has_value());
                    emit<Bytecode::Op::LeaveUnwindContext>();
                    m_current_unwind_context = m_current_unwind_context->previous();
                }
                last_was_finally = false;
            } else if (boundary == BlockBoundaryType::LeaveLexicalEnvironment) {
                emit<Bytecode::Op::LeaveLexicalEnvironment>();
            } else if (boundary == BlockBoundaryType::ReturnToFinally) {
                VERIFY(m_current_unwind_context->finalizer().has_value());
                m_current_unwind_context = m_current_unwind_context->previous();
                auto jump_type_name = type == JumpType::Break ? "break"sv : "continue"sv;
                auto block_name = MUST(String::formatted("{}.{}", current_block().name(), jump_type_name));
                auto& block = make_block(block_name);
                emit<Op::ScheduleJump>(Label { block });
                switch_to_basic_block(block);
                last_was_finally = true;
            } else if ((type == JumpType::Continue && boundary == BlockBoundaryType::Continue) || (type == JumpType::Break && boundary == BlockBoundaryType::Break)) {
                // Make sure we don't process this boundary twice if the current jumpable scope doesn't contain the target label.
                --current_boundary;
                break;
            }
        }

        if (jumpable_scope.language_label_set.contains_slow(label)) {
            emit<Op::Jump>(jumpable_scope.bytecode_target);
            return;
        }
    }

    // We must have a jumpable scope available that contains the label, as this should be enforced by the parser.
    VERIFY_NOT_REACHED();
}

void Generator::generate_break()
{
    generate_scoped_jump(JumpType::Break);
}

void Generator::generate_break(DeprecatedFlyString const& break_label)
{
    generate_labelled_jump(JumpType::Break, break_label);
}

void Generator::generate_continue()
{
    generate_scoped_jump(JumpType::Continue);
}

void Generator::generate_continue(DeprecatedFlyString const& continue_label)
{
    generate_labelled_jump(JumpType::Continue, continue_label);
}

void Generator::push_home_object(Register register_)
{
    m_home_objects.append(register_);
}

void Generator::pop_home_object()
{
    m_home_objects.take_last();
}

void Generator::emit_new_function(FunctionExpression const& function_node, Optional<IdentifierTableIndex> lhs_name)
{
    if (m_home_objects.is_empty())
        emit<Op::NewFunction>(function_node, lhs_name);
    else
        emit<Op::NewFunction>(function_node, lhs_name, m_home_objects.last());
}

CodeGenerationErrorOr<void> Generator::emit_named_evaluation_if_anonymous_function(Expression const& expression, Optional<IdentifierTableIndex> lhs_name)
{
    if (is<FunctionExpression>(expression)) {
        auto const& function_expression = static_cast<FunctionExpression const&>(expression);
        if (!function_expression.has_name()) {
            TRY(function_expression.generate_bytecode_with_lhs_name(*this, move(lhs_name)));
            return {};
        }
    }

    if (is<ClassExpression>(expression)) {
        auto const& class_expression = static_cast<ClassExpression const&>(expression);
        if (!class_expression.has_name()) {
            TRY(class_expression.generate_bytecode_with_lhs_name(*this, move(lhs_name)));
            return {};
        }
    }

    TRY(expression.generate_bytecode(*this));
    return {};
}

void Generator::emit_get_by_id(IdentifierTableIndex id)
{
    emit<Op::GetById>(id, m_next_property_lookup_cache++);
}

void Generator::emit_get_by_id_with_this(IdentifierTableIndex id, Register this_reg)
{
    emit<Op::GetByIdWithThis>(id, this_reg, m_next_property_lookup_cache++);
}

void Generator::emit_iterator_value()
{
    emit_get_by_id(intern_identifier("value"sv));
}

void Generator::emit_iterator_complete()
{
    emit_get_by_id(intern_identifier("done"sv));
}

}
