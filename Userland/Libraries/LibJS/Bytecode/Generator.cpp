/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>

namespace JS::Bytecode {

Generator::Generator()
    : m_string_table(make<StringTable>())
    , m_identifier_table(make<IdentifierTable>())
{
}

CodeGenerationErrorOr<NonnullOwnPtr<Executable>> Generator::generate(ASTNode const& node, FunctionKind enclosing_function_kind)
{
    Generator generator;
    generator.switch_to_basic_block(generator.make_block());
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
            if (block.is_terminated())
                continue;
            generator.switch_to_basic_block(block);
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

    return adopt_own(*new Executable {
        .name = {},
        .basic_blocks = move(generator.m_root_basic_blocks),
        .string_table = move(generator.m_string_table),
        .identifier_table = move(generator.m_identifier_table),
        .number_of_registers = generator.m_next_register,
        .is_strict_mode = is_strict_mode });
}

void Generator::grow(size_t additional_size)
{
    VERIFY(m_current_basic_block);
    m_current_basic_block->grow(additional_size);
}

void* Generator::next_slot()
{
    VERIFY(m_current_basic_block);
    return m_current_basic_block->next_slot();
}

Register Generator::allocate_register()
{
    VERIFY(m_next_register != NumericLimits<u32>::max());
    return Register { m_next_register++ };
}

Label Generator::nearest_continuable_scope() const
{
    return m_continuable_scopes.last().bytecode_target;
}

void Generator::begin_variable_scope(BindingMode mode, SurroundingScopeKind kind)
{
    m_variable_scopes.append({ kind, mode, {} });
    if (mode != BindingMode::Global) {
        start_boundary(mode == BindingMode::Lexical ? BlockBoundaryType::LeaveLexicalEnvironment : BlockBoundaryType::LeaveVariableEnvironment);
        emit<Bytecode::Op::CreateEnvironment>(
            mode == BindingMode::Lexical
                ? Bytecode::Op::EnvironmentMode::Lexical
                : Bytecode::Op::EnvironmentMode::Var);
    }
}

void Generator::end_variable_scope()
{
    auto mode = m_variable_scopes.take_last().mode;
    if (mode != BindingMode::Global) {
        end_boundary(mode == BindingMode::Lexical ? BlockBoundaryType::LeaveLexicalEnvironment : BlockBoundaryType::LeaveVariableEnvironment);

        if (!m_current_basic_block->is_terminated()) {
            emit<Bytecode::Op::LeaveEnvironment>(
                mode == BindingMode::Lexical
                    ? Bytecode::Op::EnvironmentMode::Lexical
                    : Bytecode::Op::EnvironmentMode::Var);
        }
    }
}

void Generator::begin_continuable_scope(Label continue_target, Vector<FlyString> const& language_label_set)
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

void Generator::begin_breakable_scope(Label breakable_target, Vector<FlyString> const& language_label_set)
{
    m_breakable_scopes.append({ breakable_target, language_label_set });
    start_boundary(BlockBoundaryType::Break);
}

void Generator::end_breakable_scope()
{
    m_breakable_scopes.take_last();
    end_boundary(BlockBoundaryType::Break);
}

CodeGenerationErrorOr<void> Generator::emit_load_from_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        emit<Bytecode::Op::GetVariable>(intern_identifier(identifier.string()));
        return {};
    }
    if (is<MemberExpression>(node)) {
        auto& expression = static_cast<MemberExpression const&>(node);
        TRY(expression.object().generate_bytecode(*this));

        if (expression.is_computed()) {
            auto object_reg = allocate_register();
            emit<Bytecode::Op::Store>(object_reg);

            TRY(expression.property().generate_bytecode(*this));
            emit<Bytecode::Op::GetByValue>(object_reg);
        } else if (expression.property().is_identifier()) {
            auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
            emit<Bytecode::Op::GetById>(identifier_table_ref);
        } else {
            return CodeGenerationError {
                &expression,
                "Unimplemented non-computed member expression"sv
            };
        }
        return {};
    }
    VERIFY_NOT_REACHED();
}

CodeGenerationErrorOr<void> Generator::emit_store_to_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        emit<Bytecode::Op::SetVariable>(intern_identifier(identifier.string()));
        return {};
    }
    if (is<MemberExpression>(node)) {
        // NOTE: The value is in the accumulator, so we have to store that away first.
        auto value_reg = allocate_register();
        emit<Bytecode::Op::Store>(value_reg);

        auto& expression = static_cast<MemberExpression const&>(node);
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
            emit<Bytecode::Op::PutById>(object_reg, identifier_table_ref);
        } else {
            return CodeGenerationError {
                &expression,
                "Unimplemented non-computed member expression"sv
            };
        }
        return {};
    }

    return CodeGenerationError {
        &node,
        "Unimplemented/invalid node used a reference"sv
    };
}

CodeGenerationErrorOr<void> Generator::emit_delete_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        emit<Bytecode::Op::DeleteVariable>(intern_identifier(identifier.string()));
        return {};
    }

    if (is<MemberExpression>(node)) {
        auto& expression = static_cast<MemberExpression const&>(node);
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

Label Generator::perform_needed_unwinds_for_labelled_break_and_return_target_block(FlyString const& break_label)
{
    size_t current_boundary = m_boundaries.size();
    for (auto& breakable_scope : m_breakable_scopes.in_reverse()) {
        for (; current_boundary > 0; --current_boundary) {
            auto boundary = m_boundaries[current_boundary - 1];
            if (boundary == BlockBoundaryType::Unwind) {
                emit<Bytecode::Op::LeaveUnwindContext>();
            } else if (boundary == BlockBoundaryType::LeaveLexicalEnvironment) {
                emit<Bytecode::Op::LeaveEnvironment>(Bytecode::Op::EnvironmentMode::Lexical);
            } else if (boundary == BlockBoundaryType::LeaveVariableEnvironment) {
                emit<Bytecode::Op::LeaveEnvironment>(Bytecode::Op::EnvironmentMode::Var);
            } else if (boundary == BlockBoundaryType::Break) {
                // Make sure we don't process this boundary twice if the current breakable scope doesn't contain the target label.
                --current_boundary;
                break;
            }
        }

        if (breakable_scope.language_label_set.contains_slow(break_label))
            return breakable_scope.bytecode_target;
    }

    // We must have a breakable scope available that contains the label, as this should be enforced by the parser.
    VERIFY_NOT_REACHED();
}

Label Generator::perform_needed_unwinds_for_labelled_continue_and_return_target_block(FlyString const& continue_label)
{
    size_t current_boundary = m_boundaries.size();
    for (auto& continuable_scope : m_continuable_scopes.in_reverse()) {
        for (; current_boundary > 0; --current_boundary) {
            auto boundary = m_boundaries[current_boundary - 1];
            if (boundary == BlockBoundaryType::Unwind) {
                emit<Bytecode::Op::LeaveUnwindContext>();
            } else if (boundary == BlockBoundaryType::LeaveLexicalEnvironment) {
                emit<Bytecode::Op::LeaveEnvironment>(Bytecode::Op::EnvironmentMode::Lexical);
            } else if (boundary == BlockBoundaryType::LeaveVariableEnvironment) {
                emit<Bytecode::Op::LeaveEnvironment>(Bytecode::Op::EnvironmentMode::Var);
            } else if (boundary == BlockBoundaryType::Continue) {
                // Make sure we don't process this boundary twice if the current continuable scope doesn't contain the target label.
                --current_boundary;
                break;
            }
        }

        if (continuable_scope.language_label_set.contains_slow(continue_label))
            return continuable_scope.bytecode_target;
    }

    // We must have a continuable scope available that contains the label, as this should be enforced by the parser.
    VERIFY_NOT_REACHED();
}

DeprecatedString CodeGenerationError::to_deprecated_string()
{
    return DeprecatedString::formatted("CodeGenerationError in {}: {}", failing_node ? failing_node->class_name() : "<unknown node>", reason_literal);
}

}
