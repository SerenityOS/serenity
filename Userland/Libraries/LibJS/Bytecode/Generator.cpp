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

Generator::~Generator()
{
}

Executable Generator::generate(ASTNode const& node, FunctionKind enclosing_function_kind)
{
    Generator generator;
    generator.switch_to_basic_block(generator.make_block());
    generator.m_enclosing_function_kind = enclosing_function_kind;
    if (generator.is_in_generator_or_async_function()) {
        // Immediately yield with no value.
        auto& start_block = generator.make_block();
        generator.emit<Bytecode::Op::Yield>(Label { start_block });
        generator.switch_to_basic_block(start_block);
    }
    node.generate_bytecode(generator);
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
    return { {}, move(generator.m_root_basic_blocks), move(generator.m_string_table), move(generator.m_identifier_table), generator.m_next_register };
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
    return m_continuable_scopes.last();
}

void Generator::begin_continuable_scope(Label continue_target)
{
    m_continuable_scopes.append(continue_target);
}

void Generator::end_continuable_scope()
{
    m_continuable_scopes.take_last();
}
Label Generator::nearest_breakable_scope() const
{
    return m_breakable_scopes.last();
}
void Generator::begin_breakable_scope(Label breakable_target)
{
    m_breakable_scopes.append(breakable_target);
}

void Generator::end_breakable_scope()
{
    m_breakable_scopes.take_last();
}

void Generator::emit_load_from_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        emit<Bytecode::Op::GetVariable>(intern_identifier(identifier.string()));
        return;
    }
    if (is<MemberExpression>(node)) {
        auto& expression = static_cast<MemberExpression const&>(node);
        expression.object().generate_bytecode(*this);

        auto object_reg = allocate_register();
        emit<Bytecode::Op::Store>(object_reg);

        if (expression.is_computed()) {
            expression.property().generate_bytecode(*this);
            emit<Bytecode::Op::GetByValue>(object_reg);
        } else {
            auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
            emit<Bytecode::Op::GetById>(identifier_table_ref);
        }
        return;
    }
    VERIFY_NOT_REACHED();
}

void Generator::emit_store_to_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        emit<Bytecode::Op::SetVariable>(intern_identifier(identifier.string()));
        return;
    }
    if (is<MemberExpression>(node)) {
        // NOTE: The value is in the accumulator, so we have to store that away first.
        auto value_reg = allocate_register();
        emit<Bytecode::Op::Store>(value_reg);

        auto& expression = static_cast<MemberExpression const&>(node);
        expression.object().generate_bytecode(*this);

        auto object_reg = allocate_register();
        emit<Bytecode::Op::Store>(object_reg);

        if (expression.is_computed()) {
            expression.property().generate_bytecode(*this);
            auto property_reg = allocate_register();
            emit<Bytecode::Op::Store>(property_reg);
            emit<Bytecode::Op::Load>(value_reg);
            emit<Bytecode::Op::PutByValue>(object_reg, property_reg);
        } else {
            emit<Bytecode::Op::Load>(value_reg);
            auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
            emit<Bytecode::Op::PutById>(object_reg, identifier_table_ref);
        }
        return;
    }
    VERIFY_NOT_REACHED();
}

}
