/*
 * Copyright (c) 2021-2024, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/TemporaryChange.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Runtime/VM.h>

namespace JS::Bytecode {

Generator::Generator(VM& vm)
    : m_vm(vm)
    , m_string_table(make<StringTable>())
    , m_identifier_table(make<IdentifierTable>())
    , m_regex_table(make<RegexTable>())
    , m_constants(vm.heap())
    , m_accumulator(*this, Operand(Register::accumulator()))
{
}

CodeGenerationErrorOr<NonnullGCPtr<Executable>> Generator::generate(VM& vm, ASTNode const& node, ReadonlySpan<FunctionParameter> parameters, FunctionKind enclosing_function_kind)
{
    Generator generator(vm);

    for (auto const& parameter : parameters) {
        if (auto const* identifier = parameter.binding.get_pointer<NonnullRefPtr<Identifier const>>();
            identifier && (*identifier)->is_local()) {
            generator.set_local_initialized((*identifier)->local_variable_index());
        }
    }

    generator.switch_to_basic_block(generator.make_block());
    SourceLocationScope scope(generator, node);
    generator.m_enclosing_function_kind = enclosing_function_kind;
    if (generator.is_in_generator_or_async_function()) {
        // Immediately yield with no value.
        auto& start_block = generator.make_block();
        generator.emit<Bytecode::Op::Yield>(Label { start_block }, generator.add_constant(js_undefined()));
        generator.switch_to_basic_block(start_block);
        // NOTE: This doesn't have to handle received throw/return completions, as GeneratorObject::resume_abrupt
        //       will not enter the generator from the SuspendedStart state and immediately completes the generator.
    }

    auto last_value = TRY(node.generate_bytecode(generator));

    if (!generator.current_block().is_terminated() && last_value.has_value()) {
        generator.emit<Bytecode::Op::End>(last_value.value());
    }

    if (generator.is_in_generator_or_async_function()) {
        // Terminate all unterminated blocks with yield return
        for (auto& block : generator.m_root_basic_blocks) {
            if (block->is_terminated())
                continue;
            generator.switch_to_basic_block(*block);
            generator.emit<Bytecode::Op::Yield>(nullptr, generator.add_constant(js_undefined()));
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

    size_t size_needed = 0;
    for (auto& block : generator.m_root_basic_blocks) {
        size_needed += block->size();
    }

    Vector<u8> bytecode;
    bytecode.ensure_capacity(size_needed);

    Vector<size_t> basic_block_start_offsets;
    basic_block_start_offsets.ensure_capacity(generator.m_root_basic_blocks.size());

    HashMap<BasicBlock const*, size_t> block_offsets;
    Vector<size_t> label_offsets;

    struct UnlinkedExceptionHandlers {
        size_t start_offset;
        size_t end_offset;
        BasicBlock const* handler;
        BasicBlock const* finalizer;
    };
    Vector<UnlinkedExceptionHandlers> unlinked_exception_handlers;

    HashMap<size_t, SourceRecord> source_map;

    for (auto& block : generator.m_root_basic_blocks) {
        basic_block_start_offsets.append(bytecode.size());
        if (block->handler() || block->finalizer()) {
            unlinked_exception_handlers.append({
                .start_offset = bytecode.size(),
                .end_offset = 0,
                .handler = block->handler(),
                .finalizer = block->finalizer(),
            });
        }

        block_offsets.set(block.ptr(), bytecode.size());

        for (auto& [offset, source_record] : block->source_map()) {
            source_map.set(bytecode.size() + offset, source_record);
        }

        Bytecode::InstructionStreamIterator it(block->instruction_stream());
        while (!it.at_end()) {
            auto& instruction = const_cast<Instruction&>(*it);

            // OPTIMIZATION: Don't emit jumps that just jump to the next block.
            if (instruction.type() == Instruction::Type::Jump) {
                auto& jump = static_cast<Bytecode::Op::Jump&>(instruction);
                if (jump.target().basic_block_index() == block->index() + 1) {
                    if (basic_block_start_offsets.last() == bytecode.size()) {
                        // This block is empty, just skip it.
                        basic_block_start_offsets.take_last();
                    }
                    ++it;
                    continue;
                }
            }

            // OPTIMIZATION: For `JumpIf` where one of the targets is the very next block,
            //               we can emit a `JumpTrue` or `JumpFalse` (to the other block) instead.
            if (instruction.type() == Instruction::Type::JumpIf) {
                auto& jump = static_cast<Bytecode::Op::JumpIf&>(instruction);
                if (jump.true_target().basic_block_index() == block->index() + 1) {
                    Op::JumpFalse jump_false(jump.condition(), Label { jump.false_target() });
                    auto& label = jump_false.target();
                    size_t label_offset = bytecode.size() + (bit_cast<FlatPtr>(&label) - bit_cast<FlatPtr>(&jump_false));
                    label_offsets.append(label_offset);
                    bytecode.append(reinterpret_cast<u8 const*>(&jump_false), jump_false.length());
                    ++it;
                    continue;
                }
                if (jump.false_target().basic_block_index() == block->index() + 1) {
                    Op::JumpTrue jump_true(jump.condition(), Label { jump.true_target() });
                    auto& label = jump_true.target();
                    size_t label_offset = bytecode.size() + (bit_cast<FlatPtr>(&label) - bit_cast<FlatPtr>(&jump_true));
                    label_offsets.append(label_offset);
                    bytecode.append(reinterpret_cast<u8 const*>(&jump_true), jump_true.length());
                    ++it;
                    continue;
                }
            }

            instruction.visit_labels([&](Label& label) {
                size_t label_offset = bytecode.size() + (bit_cast<FlatPtr>(&label) - bit_cast<FlatPtr>(&instruction));
                label_offsets.append(label_offset);
            });
            bytecode.append(reinterpret_cast<u8 const*>(&instruction), instruction.length());
            ++it;
        }
        if (!block->is_terminated()) {
            Op::End end(generator.add_constant(js_undefined()));
            bytecode.append(reinterpret_cast<u8 const*>(&end), end.length());
        }
        if (block->handler() || block->finalizer()) {
            unlinked_exception_handlers.last().end_offset = bytecode.size();
        }
    }
    for (auto label_offset : label_offsets) {
        auto& label = *reinterpret_cast<Label*>(bytecode.data() + label_offset);
        auto* block = generator.m_root_basic_blocks[label.basic_block_index()].ptr();
        label.set_address(block_offsets.get(block).value());
    }

    auto executable = vm.heap().allocate_without_realm<Executable>(
        move(bytecode),
        move(generator.m_identifier_table),
        move(generator.m_string_table),
        move(generator.m_regex_table),
        move(generator.m_constants),
        node.source_code(),
        generator.m_next_property_lookup_cache,
        generator.m_next_global_variable_cache,
        generator.m_next_environment_variable_cache,
        generator.m_next_register,
        is_strict_mode);

    Vector<Executable::ExceptionHandlers> linked_exception_handlers;

    for (auto& unlinked_handler : unlinked_exception_handlers) {
        auto start_offset = unlinked_handler.start_offset;
        auto end_offset = unlinked_handler.end_offset;
        auto handler_offset = unlinked_handler.handler ? block_offsets.get(unlinked_handler.handler).value() : Optional<size_t> {};
        auto finalizer_offset = unlinked_handler.finalizer ? block_offsets.get(unlinked_handler.finalizer).value() : Optional<size_t> {};
        linked_exception_handlers.append({ start_offset, end_offset, handler_offset, finalizer_offset });
    }

    quick_sort(linked_exception_handlers, [](auto const& a, auto const& b) {
        return a.start_offset < b.start_offset;
    });

    executable->exception_handlers = move(linked_exception_handlers);
    executable->basic_block_start_offsets = move(basic_block_start_offsets);
    executable->source_map = move(source_map);

    generator.m_finished = true;

    return executable;
}

void Generator::grow(size_t additional_size)
{
    VERIFY(m_current_basic_block);
    m_current_basic_block->grow(additional_size);
}

ScopedOperand Generator::allocate_register()
{
    if (!m_free_registers.is_empty()) {
        return ScopedOperand { *this, Operand { m_free_registers.take_last() } };
    }
    VERIFY(m_next_register != NumericLimits<u32>::max());
    return ScopedOperand { *this, Operand { Register { m_next_register++ } } };
}

void Generator::free_register(Register reg)
{
    m_free_registers.append(reg);
}

ScopedOperand Generator::local(u32 local_index)
{
    return ScopedOperand { *this, Operand { Operand::Type::Local, static_cast<u32>(local_index) } };
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

CodeGenerationErrorOr<Generator::ReferenceOperands> Generator::emit_super_reference(MemberExpression const& expression)
{
    VERIFY(is<SuperExpression>(expression.object()));

    // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
    // 1. Let env be GetThisEnvironment().
    // 2. Let actualThis be ? env.GetThisBinding().
    auto actual_this = allocate_register();
    emit<Bytecode::Op::ResolveThisBinding>(actual_this);

    Optional<ScopedOperand> computed_property_value;

    if (expression.is_computed()) {
        // SuperProperty : super [ Expression ]
        // 3. Let propertyNameReference be ? Evaluation of Expression.
        // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
        computed_property_value = TRY(expression.property().generate_bytecode(*this)).value();
    }

    // 5/7. Return ? MakeSuperPropertyReference(actualThis, propertyKey, strict).

    // https://tc39.es/ecma262/#sec-makesuperpropertyreference
    // 1. Let env be GetThisEnvironment().
    // 2. Assert: env.HasSuperBinding() is true.
    // 3. Let baseValue be ? env.GetSuperBase().
    auto base_value = allocate_register();
    emit<Bytecode::Op::ResolveSuperBase>(base_value);

    // 4. Return the Reference Record { [[Base]]: baseValue, [[ReferencedName]]: propertyKey, [[Strict]]: strict, [[ThisValue]]: actualThis }.
    return ReferenceOperands {
        .base = base_value,
        .referenced_name = computed_property_value,
        .this_value = actual_this,
    };
}

CodeGenerationErrorOr<Generator::ReferenceOperands> Generator::emit_load_from_reference(JS::ASTNode const& node, Optional<ScopedOperand> preferred_dst)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        auto loaded_value = TRY(identifier.generate_bytecode(*this, preferred_dst)).value();
        return ReferenceOperands {
            .loaded_value = loaded_value,
        };
    }
    if (!is<MemberExpression>(node)) {
        return CodeGenerationError {
            &node,
            "Unimplemented/invalid node used as a reference"sv
        };
    }
    auto& expression = static_cast<MemberExpression const&>(node);

    // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
    if (is<SuperExpression>(expression.object())) {
        auto super_reference = TRY(emit_super_reference(expression));
        auto dst = preferred_dst.has_value() ? preferred_dst.value() : allocate_register();

        if (super_reference.referenced_name.has_value()) {
            // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
            // FIXME: This does ToPropertyKey out of order, which is observable by Symbol.toPrimitive!
            emit<Bytecode::Op::GetByValueWithThis>(dst, *super_reference.base, *super_reference.referenced_name, *super_reference.this_value);
        } else {
            // 3. Let propertyKey be StringValue of IdentifierName.
            auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
            emit_get_by_id_with_this(dst, *super_reference.base, identifier_table_ref, *super_reference.this_value);
        }

        super_reference.loaded_value = dst;
        return super_reference;
    }

    auto base = TRY(expression.object().generate_bytecode(*this)).value();
    auto base_identifier = intern_identifier_for_expression(expression.object());

    if (expression.is_computed()) {
        auto property = TRY(expression.property().generate_bytecode(*this)).value();
        auto saved_property = allocate_register();
        emit<Bytecode::Op::Mov>(saved_property, property);
        auto dst = preferred_dst.has_value() ? preferred_dst.value() : allocate_register();
        emit<Bytecode::Op::GetByValue>(dst, base, property, move(base_identifier));
        return ReferenceOperands {
            .base = base,
            .referenced_name = saved_property,
            .this_value = base,
            .loaded_value = dst,
        };
    }
    if (expression.property().is_identifier()) {
        auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
        auto dst = preferred_dst.has_value() ? preferred_dst.value() : allocate_register();
        emit_get_by_id(dst, base, identifier_table_ref, move(base_identifier));
        return ReferenceOperands {
            .base = base,
            .referenced_identifier = identifier_table_ref,
            .this_value = base,
            .loaded_value = dst,
        };
    }
    if (expression.property().is_private_identifier()) {
        auto identifier_table_ref = intern_identifier(verify_cast<PrivateIdentifier>(expression.property()).string());
        auto dst = preferred_dst.has_value() ? preferred_dst.value() : allocate_register();
        emit<Bytecode::Op::GetPrivateById>(dst, base, identifier_table_ref);
        return ReferenceOperands {
            .base = base,
            .referenced_private_identifier = identifier_table_ref,
            .this_value = base,
            .loaded_value = dst,
        };
    }
    return CodeGenerationError {
        &expression,
        "Unimplemented non-computed member expression"sv
    };
}

CodeGenerationErrorOr<void> Generator::emit_store_to_reference(JS::ASTNode const& node, ScopedOperand value)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        emit_set_variable(identifier, value);
        return {};
    }
    if (is<MemberExpression>(node)) {
        auto& expression = static_cast<MemberExpression const&>(node);

        // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
        if (is<SuperExpression>(expression.object())) {
            auto super_reference = TRY(emit_super_reference(expression));

            // 4. Return the Reference Record { [[Base]]: baseValue, [[ReferencedName]]: propertyKey, [[Strict]]: strict, [[ThisValue]]: actualThis }.
            if (super_reference.referenced_name.has_value()) {
                // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
                // FIXME: This does ToPropertyKey out of order, which is observable by Symbol.toPrimitive!
                emit<Bytecode::Op::PutByValueWithThis>(*super_reference.base, *super_reference.referenced_name, *super_reference.this_value, value);
            } else {
                // 3. Let propertyKey be StringValue of IdentifierName.
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit<Bytecode::Op::PutByIdWithThis>(*super_reference.base, *super_reference.this_value, identifier_table_ref, value, Bytecode::Op::PropertyKind::KeyValue, next_property_lookup_cache());
            }
        } else {
            auto object = TRY(expression.object().generate_bytecode(*this)).value();

            if (expression.is_computed()) {
                auto property = TRY(expression.property().generate_bytecode(*this)).value();
                emit<Bytecode::Op::PutByValue>(object, property, value);
            } else if (expression.property().is_identifier()) {
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit<Bytecode::Op::PutById>(object, identifier_table_ref, value, Bytecode::Op::PropertyKind::KeyValue, next_property_lookup_cache());
            } else if (expression.property().is_private_identifier()) {
                auto identifier_table_ref = intern_identifier(verify_cast<PrivateIdentifier>(expression.property()).string());
                emit<Bytecode::Op::PutPrivateById>(object, identifier_table_ref, value);
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

CodeGenerationErrorOr<void> Generator::emit_store_to_reference(ReferenceOperands const& reference, ScopedOperand value)
{
    if (reference.referenced_private_identifier.has_value()) {
        emit<Bytecode::Op::PutPrivateById>(*reference.base, *reference.referenced_private_identifier, value);
        return {};
    }
    if (reference.referenced_identifier.has_value()) {
        if (reference.base == reference.this_value)
            emit<Bytecode::Op::PutById>(*reference.base, *reference.referenced_identifier, value, Bytecode::Op::PropertyKind::KeyValue, next_property_lookup_cache());
        else
            emit<Bytecode::Op::PutByIdWithThis>(*reference.base, *reference.this_value, *reference.referenced_identifier, value, Bytecode::Op::PropertyKind::KeyValue, next_property_lookup_cache());
        return {};
    }
    if (reference.base == reference.this_value)
        emit<Bytecode::Op::PutByValue>(*reference.base, *reference.referenced_name, value);
    else
        emit<Bytecode::Op::PutByValueWithThis>(*reference.base, *reference.referenced_name, *reference.this_value, value);
    return {};
}

CodeGenerationErrorOr<Optional<ScopedOperand>> Generator::emit_delete_reference(JS::ASTNode const& node)
{
    if (is<Identifier>(node)) {
        auto& identifier = static_cast<Identifier const&>(node);
        if (identifier.is_local()) {
            return add_constant(Value(false));
        }
        auto dst = allocate_register();
        emit<Bytecode::Op::DeleteVariable>(dst, intern_identifier(identifier.string()));
        return dst;
    }

    if (is<MemberExpression>(node)) {
        auto& expression = static_cast<MemberExpression const&>(node);

        // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
        if (is<SuperExpression>(expression.object())) {
            auto super_reference = TRY(emit_super_reference(expression));

            auto dst = Operand(allocate_register());
            if (super_reference.referenced_name.has_value()) {
                emit<Bytecode::Op::DeleteByValueWithThis>(dst, *super_reference.base, *super_reference.this_value, *super_reference.referenced_name);
            } else {
                auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
                emit<Bytecode::Op::DeleteByIdWithThis>(dst, *super_reference.base, *super_reference.this_value, identifier_table_ref);
            }

            return Optional<ScopedOperand> {};
        }

        auto object = TRY(expression.object().generate_bytecode(*this)).value();
        auto dst = allocate_register();

        if (expression.is_computed()) {
            auto property = TRY(expression.property().generate_bytecode(*this)).value();
            emit<Bytecode::Op::DeleteByValue>(dst, object, property);
        } else if (expression.property().is_identifier()) {
            auto identifier_table_ref = intern_identifier(verify_cast<Identifier>(expression.property()).string());
            emit<Bytecode::Op::DeleteById>(dst, object, identifier_table_ref);
        } else {
            // NOTE: Trying to delete a private field generates a SyntaxError in the parser.
            return CodeGenerationError {
                &expression,
                "Unimplemented non-computed member expression"sv
            };
        }
        return dst;
    }

    // Though this will have no deletion effect, we still have to evaluate the node as it can have side effects.
    // For example: delete a(); delete ++c.b; etc.

    // 13.5.1.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-delete-operator-runtime-semantics-evaluation
    // 1. Let ref be the result of evaluating UnaryExpression.
    // 2. ReturnIfAbrupt(ref).
    (void)TRY(node.generate_bytecode(*this));

    // 3. If ref is not a Reference Record, return true.
    // NOTE: The rest of the steps are handled by Delete{Variable,ByValue,Id}.
    return add_constant(Value(true));
}

void Generator::emit_set_variable(JS::Identifier const& identifier, ScopedOperand value, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Op::EnvironmentMode mode)
{
    if (identifier.is_local()) {
        if (value.operand().is_local() && value.operand().index() == identifier.local_variable_index()) {
            // Moving a local to itself is a no-op.
            return;
        }
        emit<Bytecode::Op::SetLocal>(identifier.local_variable_index(), value);
    } else {
        emit<Bytecode::Op::SetVariable>(intern_identifier(identifier.string()), value, next_environment_variable_cache(), initialization_mode, mode);
    }
}

static Optional<ByteString> expression_identifier(Expression const& expression)
{
    if (expression.is_identifier()) {
        auto const& identifier = static_cast<Identifier const&>(expression);
        return identifier.string();
    }

    if (expression.is_numeric_literal()) {
        auto const& literal = static_cast<NumericLiteral const&>(expression);
        return literal.value().to_string_without_side_effects().to_byte_string();
    }

    if (expression.is_string_literal()) {
        auto const& literal = static_cast<StringLiteral const&>(expression);
        return ByteString::formatted("'{}'", literal.value());
    }

    if (expression.is_member_expression()) {
        auto const& member_expression = static_cast<MemberExpression const&>(expression);
        StringBuilder builder;

        if (auto identifer = expression_identifier(member_expression.object()); identifer.has_value())
            builder.append(*identifer);

        if (auto identifer = expression_identifier(member_expression.property()); identifer.has_value()) {
            if (member_expression.is_computed())
                builder.appendff("[{}]", *identifer);
            else
                builder.appendff(".{}", *identifer);
        }

        return builder.to_byte_string();
    }

    return {};
}

Optional<IdentifierTableIndex> Generator::intern_identifier_for_expression(Expression const& expression)
{
    if (auto identifer = expression_identifier(expression); identifer.has_value())
        return intern_identifier(identifer.release_value());
    return {};
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
        }
        case LeaveFinally:
            emit<Op::LeaveFinally>();
            break;
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

void Generator::push_home_object(ScopedOperand object)
{
    m_home_objects.append(object);
}

void Generator::pop_home_object()
{
    m_home_objects.take_last();
}

void Generator::emit_new_function(ScopedOperand dst, FunctionExpression const& function_node, Optional<IdentifierTableIndex> lhs_name)
{
    if (m_home_objects.is_empty()) {
        emit<Op::NewFunction>(dst, function_node, lhs_name);
    } else {
        emit<Op::NewFunction>(dst, function_node, lhs_name, m_home_objects.last());
    }
}

CodeGenerationErrorOr<Optional<ScopedOperand>> Generator::emit_named_evaluation_if_anonymous_function(Expression const& expression, Optional<IdentifierTableIndex> lhs_name, Optional<ScopedOperand> preferred_dst)
{
    if (is<FunctionExpression>(expression)) {
        auto const& function_expression = static_cast<FunctionExpression const&>(expression);
        if (!function_expression.has_name()) {
            return TRY(function_expression.generate_bytecode_with_lhs_name(*this, move(lhs_name), preferred_dst)).value();
        }
    }

    if (is<ClassExpression>(expression)) {
        auto const& class_expression = static_cast<ClassExpression const&>(expression);
        if (!class_expression.has_name()) {
            return TRY(class_expression.generate_bytecode_with_lhs_name(*this, move(lhs_name), preferred_dst)).value();
        }
    }

    return expression.generate_bytecode(*this, preferred_dst);
}

void Generator::emit_get_by_id(ScopedOperand dst, ScopedOperand base, IdentifierTableIndex property_identifier, Optional<IdentifierTableIndex> base_identifier)
{
    emit<Op::GetById>(dst, base, property_identifier, move(base_identifier), m_next_property_lookup_cache++);
}

void Generator::emit_get_by_id_with_this(ScopedOperand dst, ScopedOperand base, IdentifierTableIndex id, ScopedOperand this_value)
{
    emit<Op::GetByIdWithThis>(dst, base, id, this_value, m_next_property_lookup_cache++);
}

void Generator::emit_iterator_value(ScopedOperand dst, ScopedOperand result)
{
    emit_get_by_id(dst, result, intern_identifier("value"sv));
}

void Generator::emit_iterator_complete(ScopedOperand dst, ScopedOperand result)
{
    emit_get_by_id(dst, result, intern_identifier("done"sv));
}

bool Generator::is_local_initialized(u32 local_index) const
{
    return m_initialized_locals.find(local_index) != m_initialized_locals.end();
}

void Generator::set_local_initialized(u32 local_index)
{
    m_initialized_locals.set(local_index);
}

ScopedOperand Generator::get_this(Optional<ScopedOperand> preferred_dst)
{
    if (m_current_basic_block->this_().has_value())
        return m_current_basic_block->this_().value();
    if (m_root_basic_blocks[0]->this_().has_value()) {
        m_current_basic_block->set_this(m_root_basic_blocks[0]->this_().value());
        return m_root_basic_blocks[0]->this_().value();
    }
    auto dst = preferred_dst.has_value() ? preferred_dst.value() : allocate_register();
    emit<Bytecode::Op::ResolveThisBinding>(dst);
    m_current_basic_block->set_this(dst);
    return dst;
}

ScopedOperand Generator::accumulator()
{
    return m_accumulator;
}

}
