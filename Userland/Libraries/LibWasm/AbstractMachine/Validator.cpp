/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/Result.h>
#include <AK/SourceLocation.h>
#include <AK/Try.h>
#include <LibWasm/AbstractMachine/Validator.h>
#include <LibWasm/Printer/Printer.h>

namespace Wasm {

ErrorOr<void, ValidationError> Validator::validate(Module& module)
{
    ErrorOr<void, ValidationError> result {};

    // Note: The spec performs this after populating the context, but there's no real reason to do so,
    //       as this has no dependency.
    HashTable<StringView> seen_export_names;
    module.for_each_section_of_type<ExportSection>([&result, &seen_export_names](ExportSection const& section) {
        if (result.is_error())
            return;
        for (auto& export_ : section.entries()) {
            if (seen_export_names.try_set(export_.name()).release_value_but_fixme_should_propagate_errors() != AK::HashSetResult::InsertedNewEntry)
                result = Errors::duplicate_export_name(export_.name());
            return;
        }
    });
    if (result.is_error()) {
        module.set_validation_status(Module::ValidationStatus::Invalid, {});
        return result;
    }

    m_context = {};

    module.for_each_section_of_type<TypeSection>([this](TypeSection const& section) {
        m_context.types = section.types();
    });

    module.for_each_section_of_type<ImportSection>([&](ImportSection const& section) {
        for (auto& import_ : section.imports()) {
            import_.description().visit(
                [this, &result](TypeIndex const& index) {
                    if (m_context.types.size() > index.value())
                        m_context.functions.append(m_context.types[index.value()]);
                    else
                        result = Errors::invalid("TypeIndex"sv);
                    m_context.imported_function_count++;
                },
                [this](FunctionType const& type) {
                    m_context.functions.append(type);
                    m_context.imported_function_count++;
                },
                [this](TableType const& type) { m_context.tables.append(type); },
                [this](MemoryType const& type) { m_context.memories.append(type); },
                [this](GlobalType const& type) { m_context.globals.append(type); });
        }
    });

    if (result.is_error()) {
        module.set_validation_status(Module::ValidationStatus::Invalid, {});
        return result;
    }

    module.for_each_section_of_type<FunctionSection>([this, &result](FunctionSection const& section) {
        if (result.is_error())
            return;
        m_context.functions.ensure_capacity(section.types().size() + m_context.functions.size());
        for (auto& index : section.types()) {
            if (m_context.types.size() > index.value()) {
                m_context.functions.append(m_context.types[index.value()]);
            } else {
                result = Errors::invalid("TypeIndex");
                break;
            }
        }
    });
    if (result.is_error()) {
        module.set_validation_status(Module::ValidationStatus::Invalid, {});
        return result;
    }

    module.for_each_section_of_type<TableSection>([this](TableSection const& section) {
        m_context.tables.ensure_capacity(m_context.tables.size() + section.tables().size());
        for (auto& table : section.tables())
            m_context.tables.unchecked_append(table.type());
    });
    module.for_each_section_of_type<MemorySection>([this](MemorySection const& section) {
        m_context.memories.ensure_capacity(m_context.memories.size() + section.memories().size());
        for (auto& memory : section.memories())
            m_context.memories.unchecked_append(memory.type());
    });
    module.for_each_section_of_type<GlobalSection>([this](GlobalSection const& section) {
        m_context.globals.ensure_capacity(m_context.globals.size() + section.entries().size());
        for (auto& global : section.entries())
            m_context.globals.unchecked_append(global.type());
    });
    module.for_each_section_of_type<ElementSection>([this](ElementSection const& section) {
        m_context.elements.ensure_capacity(section.segments().size());
        for (auto& segment : section.segments())
            m_context.elements.unchecked_append(segment.type);
    });
    module.for_each_section_of_type<DataSection>([this](DataSection const& section) {
        m_context.datas.resize(section.data().size());
    });

    // FIXME: C.refs is the set funcidx(module with funcs=ϵ with start=ϵ),
    //        i.e., the set of function indices occurring in the module, except in its functions or start function.
    // This is rather weird, it seems to ultimately be checking that `ref.func` uses a specific set of predetermined functions:
    // The only place where this is accessed is in validate_instruction<ref_func>(), but we *populate* this from the ref.func instructions occurring outside regular functions,
    // which limits it to only functions referenced from the elements section.
    // so the only reason for this (as I see) is to ensure that ref.func only hands out references that occur within the elements and global sections
    // _if_ that is indeed the case, then this should be much more specific about where the "valid" references are, and about the actual purpose of this field.
    //
    // For now, we simply assume that we need to scan the aforementioned section initializers for (ref.func f).
    auto scan_expression_for_function_indices = [&](auto& expression) {
        for (auto& instruction : expression.instructions()) {
            if (instruction.opcode() == Instructions::ref_func)
                m_context.references.set(instruction.arguments().template get<FunctionIndex>());
        }
    };
    module.for_each_section_of_type<ElementSection>([&](ElementSection const& section) {
        for (auto& segment : section.segments()) {
            for (auto& expression : segment.init)
                scan_expression_for_function_indices(expression);
        }
    });
    module.for_each_section_of_type<GlobalSection>([&](GlobalSection const& section) {
        for (auto& segment : section.entries())
            scan_expression_for_function_indices(segment.expression());
    });

    for (auto& section : module.sections()) {
        section.visit([this, &result](auto& section) {
            result = validate(section);
        });
        if (result.is_error()) {
            module.set_validation_status(Module::ValidationStatus::Invalid, {});
            return result;
        }
    }

    if (m_context.memories.size() > 1) {
        module.set_validation_status(Module::ValidationStatus::Invalid, {});
        return Errors::out_of_bounds("memory section count"sv, m_context.memories.size(), 1, 1);
    }

    module.set_validation_status(Module::ValidationStatus::Valid, {});
    return {};
}

ErrorOr<void, ValidationError> Validator::validate(ImportSection const& section)
{
    for (auto& import_ : section.imports())
        TRY(import_.description().visit([&](auto& entry) { return validate(entry); }));
    return {};
}

ErrorOr<void, ValidationError> Validator::validate(ExportSection const& section)
{
    for (auto& export_ : section.entries())
        TRY(export_.description().visit([&](auto& entry) { return validate(entry); }));
    return {};
}

ErrorOr<void, ValidationError> Validator::validate(StartSection const& section)
{
    TRY(validate(section.function().index()));
    FunctionType const& type = m_context.functions[section.function().index().value()];
    if (!type.parameters().is_empty() || !type.results().is_empty())
        return Errors::invalid("start function signature");
    return {};
}

ErrorOr<void, ValidationError> Validator::validate(DataSection const& section)
{
    for (auto& entry : section.data()) {
        TRY(entry.value().visit(
            [](DataSection::Data::Passive const&) { return ErrorOr<void, ValidationError> {}; },
            [&](DataSection::Data::Active const& active) -> ErrorOr<void, ValidationError> {
                TRY(validate(active.index));

                auto expression_result = TRY(validate(active.offset, { ValueType(ValueType::I32) }));

                if (!expression_result.is_constant)
                    return Errors::invalid("active data initializer");

                if (expression_result.result_types.size() != 1 || !expression_result.result_types.first().is_of_kind(ValueType::I32))
                    return Errors::invalid("active data initializer type", ValueType(ValueType::I32), expression_result.result_types);

                return {};
            }));
    }

    return {};
}

ErrorOr<void, ValidationError> Validator::validate(ElementSection const& section)
{
    for (auto& segment : section.segments()) {
        TRY(segment.mode.visit(
            [](ElementSection::Declarative const&) -> ErrorOr<void, ValidationError> { return {}; },
            [](ElementSection::Passive const&) -> ErrorOr<void, ValidationError> { return {}; },
            [&](ElementSection::Active const& active) -> ErrorOr<void, ValidationError> {
                TRY(validate(active.index));
                auto expression_result = TRY(validate(active.expression, { ValueType(ValueType::I32) }));
                if (!expression_result.is_constant)
                    return Errors::invalid("active element initializer");
                if (expression_result.result_types.size() != 1 || !expression_result.result_types.first().is_of_kind(ValueType::I32))
                    return Errors::invalid("active element initializer type", ValueType(ValueType::I32), expression_result.result_types);
                return {};
            }));
    }
    return {};
}

ErrorOr<void, ValidationError> Validator::validate(GlobalSection const& section)
{
    for (auto& entry : section.entries()) {
        auto& type = entry.type();
        TRY(validate(type));
        auto expression_result = TRY(validate(entry.expression(), { type.type() }));
        if (!expression_result.is_constant)
            return Errors::invalid("global variable initializer");
        if (expression_result.result_types.size() != 1 || !expression_result.result_types.first().is_of_kind(type.type().kind()))
            return Errors::invalid("global variable initializer type", ValueType(ValueType::I32), expression_result.result_types);
    }

    return {};
}

ErrorOr<void, ValidationError> Validator::validate(MemorySection const& section)
{
    for (auto& entry : section.memories())
        TRY(validate(entry.type()));
    return {};
}

ErrorOr<void, ValidationError> Validator::validate(TableSection const& section)
{
    for (auto& entry : section.tables())
        TRY(validate(entry.type()));
    return {};
}

ErrorOr<void, ValidationError> Validator::validate(CodeSection const& section)
{
    size_t index = m_context.imported_function_count;
    for (auto& entry : section.functions()) {
        auto function_index = index++;
        TRY(validate(FunctionIndex { function_index }));
        auto& function_type = m_context.functions[function_index];
        auto& function = entry.func();

        auto function_validator = fork();
        function_validator.m_context.locals = {};
        function_validator.m_context.locals.extend(function_type.parameters());
        for (auto& local : function.locals()) {
            for (size_t i = 0; i < local.n(); ++i)
                function_validator.m_context.locals.append(local.type());
        }

        function_validator.m_context.labels = { ResultType { function_type.results() } };
        function_validator.m_context.return_ = ResultType { function_type.results() };

        TRY(function_validator.validate(function.body(), function_type.results()));
    }

    return {};
}

ErrorOr<void, ValidationError> Validator::validate(TableType const& type)
{
    return validate(type.limits(), 32);
}

ErrorOr<void, ValidationError> Validator::validate(MemoryType const& type)
{
    return validate(type.limits(), 16);
}

ErrorOr<FunctionType, ValidationError> Validator::validate(BlockType const& type)
{
    if (type.kind() == BlockType::Index) {
        TRY(validate(type.type_index()));
        return m_context.types[type.type_index().value()];
    }

    if (type.kind() == BlockType::Type) {
        FunctionType function_type { {}, { type.value_type() } };
        TRY(validate(function_type));
        return function_type;
    }

    if (type.kind() == BlockType::Empty)
        return FunctionType { {}, {} };

    return Errors::invalid("BlockType"sv);
}

ErrorOr<void, ValidationError> Validator::validate(Limits const& limits, size_t k)
{
    auto bound = (1ull << k) - 1;
    auto check_bound = [bound](auto value) {
        return static_cast<u64>(value) < bound;
    };

    if (!check_bound(limits.min()))
        return Errors::out_of_bounds("limit minimum"sv, limits.min(), 0, bound);

    if (limits.max().has_value() && (limits.max().value() < limits.min() || !check_bound(*limits.max())))
        return Errors::out_of_bounds("limit maximum"sv, limits.max().value(), limits.min(), bound);

    return {};
}

template<u32 opcode>
ErrorOr<void, ValidationError> Validator::validate_instruction(Instruction const&, Stack&, bool&)
{
    return Errors::invalid("instruction opcode"sv);
}

#define VALIDATE_INSTRUCTION(name) \
    template<>                     \
    ErrorOr<void, ValidationError> Validator::validate_instruction<Instructions::name.value()>([[maybe_unused]] Instruction const& instruction, [[maybe_unused]] Stack& stack, [[maybe_unused]] bool& is_constant)

// https://webassembly.github.io/spec/core/bikeshed/#-tmathsfhrefsyntax-instr-numericmathsfconstc
VALIDATE_INSTRUCTION(i32_const)
{
    is_constant = true;
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_const)
{
    is_constant = true;
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(f32_const)
{
    is_constant = true;
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f64_const)
{
    is_constant = true;
    stack.append(ValueType(ValueType::F64));
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#-tmathsfhrefsyntax-unopmathitunop
VALIDATE_INSTRUCTION(i32_clz)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i32_ctz)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i32_popcnt)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i64_clz)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i64_ctz)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i64_popcnt)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f32_abs)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f32_neg)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f32_sqrt)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f32_ceil)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f32_floor)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f32_trunc)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f32_nearest)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f64_abs)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f64_neg)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f64_sqrt)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f64_ceil)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f64_floor)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f64_trunc)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(f64_nearest)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i32_extend16_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i32_extend8_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i64_extend32_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i64_extend16_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(i64_extend8_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#-tmathsfhrefsyntax-binopmathitbinop
VALIDATE_INSTRUCTION(i32_add)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_sub)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_mul)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_divs)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_divu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_rems)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_remu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_and)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_or)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_xor)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_shl)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_shrs)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_shru)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_rotl)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_rotr)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_add)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_sub)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_mul)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_divs)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_divu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_rems)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_remu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_and)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_or)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_xor)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_shl)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_shrs)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_shru)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_rotl)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_rotr)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(f32_add)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_sub)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_mul)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_div)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_min)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_max)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_copysign)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f64_add)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_sub)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_mul)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_div)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_min)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_max)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_copysign)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#-tmathsfhrefsyntax-testopmathittestop
VALIDATE_INSTRUCTION(i32_eqz)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i64_eqz)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#-tmathsfhrefsyntax-relopmathitrelop
VALIDATE_INSTRUCTION(i32_eq)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_ne)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_lts)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_ltu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_gts)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_gtu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_les)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_leu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_ges)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_geu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_eq)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_ne)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_lts)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_ltu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_gts)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_gtu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_les)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_leu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_ges)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_geu)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f32_eq)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f32_ne)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f32_lt)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f32_le)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f32_gt)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f32_ge)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f64_eq)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f64_ne)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f64_lt)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f64_le)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f64_gt)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(f64_ge)
{
    if (stack.size() < 2 || stack.take_last() != stack.last() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#-t_2mathsfhrefsyntax-cvtopmathitcvtopmathsf_t_1mathsf_hrefsyntax-sxmathitsx
VALIDATE_INSTRUCTION(i32_wrap_i64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_extend_si32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_extend_ui32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_sf32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_uf32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_sf64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_uf64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_sf32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_uf32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_sf64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_uf64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_sat_f32_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_sat_f32_u)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_sat_f64_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_trunc_sat_f64_u)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_sat_f32_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_sat_f32_u)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_sat_f64_s)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_trunc_sat_f64_u)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(f32_convert_si32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_convert_ui32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_convert_si64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f32_convert_ui64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f64_convert_si32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_convert_ui32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_convert_si64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f64_convert_ui64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f32_demote_f64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f64_promote_f32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(f32_reinterpret_i32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f64_reinterpret_i64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(i32_reinterpret_f32)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_reinterpret_f64)
{
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#reference-instructions%E2%91%A2

VALIDATE_INSTRUCTION(ref_null)
{
    is_constant = true;
    stack.append(instruction.arguments().get<ValueType>());
    return {};
}

VALIDATE_INSTRUCTION(ref_is_null)
{
    if (stack.is_empty() || !stack.last().is_reference())
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(ref_func)
{
    auto index = instruction.arguments().get<FunctionIndex>();
    TRY(validate(index));

    if (!m_context.references.contains(index))
        return Errors::invalid("function reference");

    is_constant = true;
    stack.append(ValueType(ValueType::FunctionReference));
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#parametric-instructions%E2%91%A2
VALIDATE_INSTRUCTION(drop)
{
    if (stack.is_empty())
        return Errors::invalid_stack_state();
    stack.take_last();
    return {};
}

VALIDATE_INSTRUCTION(select)
{
    if (stack.size() < 3)
        return Errors::invalid_stack_state();

    auto index_type = stack.take_last();
    auto arg0_type = stack.take_last();
    auto& arg1_type = stack.last();
    if (!index_type.is_of_kind(ValueType::I32))
        return Errors::invalid("select index type", ValueType(ValueType::I32), index_type);

    if (arg0_type != arg1_type)
        return Errors::invalid("select argument types", Vector { arg0_type, arg0_type }, Vector { arg0_type, arg1_type });

    return {};
}

VALIDATE_INSTRUCTION(select_typed)
{
    if (stack.size() < 3)
        return Errors::invalid_stack_state();

    auto& required_types = instruction.arguments().get<Vector<ValueType>>();
    if (required_types.size() != 1)
        return Errors::invalid("select types", "exactly one type", required_types);

    auto index_type = stack.take_last();
    auto arg0_type = stack.take_last();
    auto& arg1_type = stack.last();
    if (!index_type.is_of_kind(ValueType::I32))
        return Errors::invalid("select index type", ValueType(ValueType::I32), index_type);

    if (arg0_type != arg1_type || arg0_type != required_types.first())
        return Errors::invalid("select argument types", Vector { required_types.first(), required_types.first() }, Vector { arg0_type, arg1_type });

    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#variable-instructions%E2%91%A2
VALIDATE_INSTRUCTION(local_get)
{
    auto index = instruction.arguments().get<LocalIndex>();
    TRY(validate(index));

    stack.append(m_context.locals[index.value()]);
    return {};
}

VALIDATE_INSTRUCTION(local_set)
{
    auto index = instruction.arguments().get<LocalIndex>();
    TRY(validate(index));

    auto& value_type = m_context.locals[index.value()];
    if (stack.take_last() != value_type)
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(local_tee)
{
    auto index = instruction.arguments().get<LocalIndex>();
    TRY(validate(index));

    auto& value_type = m_context.locals[index.value()];
    if (stack.last() != value_type)
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(global_get)
{
    auto index = instruction.arguments().get<GlobalIndex>();
    TRY(validate(index));

    auto& global = m_context.globals[index.value()];

    is_constant = !global.is_mutable();
    stack.append(global.type());
    return {};
}

VALIDATE_INSTRUCTION(global_set)
{
    auto index = instruction.arguments().get<GlobalIndex>();
    TRY(validate(index));

    auto& global = m_context.globals[index.value()];

    if (!global.is_mutable())
        return Errors::invalid("global variable for global.set");

    stack.append(global.type());
    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#table-instructions%E2%91%A2
VALIDATE_INSTRUCTION(table_get)
{
    auto index = instruction.arguments().get<TableIndex>();
    TRY(validate(index));

    auto& table = m_context.tables[index.value()];
    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(table.element_type());
    return {};
}

VALIDATE_INSTRUCTION(table_set)
{
    auto index = instruction.arguments().get<TableIndex>();
    TRY(validate(index));

    auto& table = m_context.tables[index.value()];
    if (stack.is_empty())
        return Errors::invalid_stack_state();

    if (stack.take_last() != table.element_type())
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(table_size)
{
    auto index = instruction.arguments().get<TableIndex>();
    TRY(validate(index));

    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(table_grow)
{
    auto index = instruction.arguments().get<TableIndex>();
    TRY(validate(index));

    auto& table = m_context.tables[index.value()];
    if (stack.is_empty())
        return Errors::invalid_stack_state();

    if (!stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || stack.take_last() != table.element_type())
        return Errors::invalid_stack_state();

    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(table_fill)
{
    auto index = instruction.arguments().get<TableIndex>();
    TRY(validate(index));

    auto& table = m_context.tables[index.value()];
    if (stack.is_empty())
        return Errors::invalid_stack_state();

    if (!stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || stack.take_last() != table.element_type())
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(table_copy)
{
    auto& args = instruction.arguments().get<Instruction::TableTableArgs>();

    TRY(validate(args.lhs));
    TRY(validate(args.rhs));

    auto& lhs_table = m_context.tables[args.lhs.value()];
    auto& rhs_table = m_context.tables[args.rhs.value()];

    if (lhs_table.element_type() != rhs_table.element_type())
        return Errors::non_conforming_types("table.copy", lhs_table.element_type(), rhs_table.element_type());

    if (!lhs_table.element_type().is_reference())
        return Errors::invalid("table.copy element type", "a reference type", lhs_table.element_type());

    if (stack.size() < 3)
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < 3; ++i) {
        if (!stack.take_last().is_of_kind(ValueType::I32))
            return Errors::invalid_stack_state();
    }

    return {};
}

VALIDATE_INSTRUCTION(table_init)
{
    auto& args = instruction.arguments().get<Instruction::TableElementArgs>();

    TRY(validate(args.table_index));
    TRY(validate(args.element_index));

    auto& table = m_context.tables[args.table_index.value()];
    auto& element_type = m_context.elements[args.element_index.value()];

    if (table.element_type() != element_type)
        return Errors::non_conforming_types("table.init", table.element_type(), element_type);

    if (stack.size() < 3)
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < 3; ++i) {
        if (!stack.take_last().is_of_kind(ValueType::I32))
            return Errors::invalid_stack_state();
    }

    return {};
}

VALIDATE_INSTRUCTION(elem_drop)
{
    auto index = instruction.arguments().get<ElementIndex>();
    TRY(validate(index));

    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#memory-instructions%E2%91%A2
VALIDATE_INSTRUCTION(i32_load)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(i32))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(i32));

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i64_load)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(i64))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(i64));

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(f32_load)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(float))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(float));

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F32));
    return {};
}

VALIDATE_INSTRUCTION(f64_load)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(double))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(double));

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::F64));
    return {};
}

VALIDATE_INSTRUCTION(i32_load16_s)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 16 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 16 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_load16_u)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 16 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 16 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_load8_s)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 8 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 8 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i32_load8_u)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 8 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 8 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(i64_load32_s)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 32 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 32 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_load32_u)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 32 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 32 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_load16_s)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 16 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 16 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_load16_u)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 16 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 16 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_load8_s)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 8 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 8 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i64_load8_u)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 8 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 8 / 8);

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    stack.take_last();
    stack.append(ValueType(ValueType::I64));
    return {};
}

VALIDATE_INSTRUCTION(i32_store)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(i32))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(i32));

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i64_store)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(i64))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(i64));

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(f32_store)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(float))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(float));

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::F32))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(f64_store)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > sizeof(double))
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, sizeof(double));

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::F64))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i32_store16)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 16 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 16 / 8);

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i32_store8)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 8 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 8 / 8);

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i64_store32)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 32 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 32 / 8);

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i64_store16)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 16 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 16 / 8);

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(i64_store8)
{
    TRY(validate(MemoryIndex { 0 }));

    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    if ((1ull << arg.align) > 8 / 8)
        return Errors::out_of_bounds("memory op alignment", 1ull << arg.align, 0, 8 / 8);

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I64))
        return Errors::invalid_stack_state();

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    return {};
}

VALIDATE_INSTRUCTION(memory_size)
{
    TRY(validate(MemoryIndex { 0 }));

    stack.append(ValueType(ValueType::I32));
    return {};
}

VALIDATE_INSTRUCTION(memory_grow)
{
    TRY(validate(MemoryIndex { 0 }));

    if (stack.is_empty() || !stack.last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();
    return {};
}

VALIDATE_INSTRUCTION(memory_fill)
{
    TRY(validate(MemoryIndex { 0 }));

    if (stack.size() < 3)
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < 3; ++i) {
        if (!stack.take_last().is_of_kind(ValueType::I32))
            return Errors::invalid_stack_state();
    }

    return {};
}

VALIDATE_INSTRUCTION(memory_init)
{
    TRY(validate(MemoryIndex { 0 }));

    auto index = instruction.arguments().get<DataIndex>();
    TRY(validate(index));

    if (stack.size() < 3)
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < 3; ++i) {
        if (!stack.take_last().is_of_kind(ValueType::I32))
            return Errors::invalid_stack_state();
    }

    return {};
}

VALIDATE_INSTRUCTION(data_drop)
{
    auto index = instruction.arguments().get<DataIndex>();
    TRY(validate(index));

    return {};
}

// https://webassembly.github.io/spec/core/bikeshed/#control-instructions%E2%91%A2
VALIDATE_INSTRUCTION(nop)
{
    return {};
}

VALIDATE_INSTRUCTION(unreachable)
{
    // https://webassembly.github.io/spec/core/bikeshed/#polymorphism
    stack.append(StackEntry());

    return {};
}

// Note: This is responsible for _all_ structured instructions, and is *not* from the spec.
VALIDATE_INSTRUCTION(structured_end)
{
    if (m_entered_scopes.is_empty())
        return Errors::invalid("usage of structured end");

    auto last_scope = m_entered_scopes.take_last();
    m_context = m_parent_contexts.take_last();
    auto last_block_type = m_entered_blocks.take_last();

    if (last_scope == ChildScopeKind::Block) {
        auto details = m_block_details.take_last();
        // FIXME: Validate the returns.
        return {};
    }

    if (last_scope == ChildScopeKind::Else) {
        auto details = m_block_details.take_last().details.get<BlockDetails::IfDetails>();
        if (details.true_branch_stack != stack)
            return Errors::invalid("stack configuration after if-else", details.true_branch_stack.release_vector(), stack.release_vector());

        return {};
    }

    return {};
}

// Note: This is *not* from the spec.
VALIDATE_INSTRUCTION(structured_else)
{
    if (m_entered_scopes.is_empty())
        return Errors::invalid("usage of structured else");

    if (m_entered_scopes.last() != ChildScopeKind::IfWithElse)
        return Errors::invalid("usage of structured else");

    m_entered_scopes.last() = ChildScopeKind::Else;
    auto& if_details = m_block_details.last().details.get<BlockDetails::IfDetails>();
    if_details.true_branch_stack = exchange(stack, move(if_details.initial_stack));
    m_context = m_parent_contexts.last();
    return {};
}

VALIDATE_INSTRUCTION(block)
{
    auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
    auto block_type = TRY(validate(args.block_type));

    auto& parameters = block_type.parameters();
    if (stack.size() < parameters.size())
        return Errors::invalid_stack_state();

    for (size_t i = 1; i <= parameters.size(); ++i) {
        if (stack.take_last() != parameters[parameters.size() - i])
            return Errors::invalid_stack_state();
    }

    for (auto& parameter : parameters)
        stack.append(parameter);

    m_entered_scopes.append(ChildScopeKind::Block);
    m_block_details.empend(stack.actual_size(), Empty {});
    m_parent_contexts.append(m_context);
    m_entered_blocks.append(block_type);
    m_context.labels.prepend(ResultType { block_type.results() });
    return {};
}

VALIDATE_INSTRUCTION(loop)
{
    auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
    auto block_type = TRY(validate(args.block_type));

    auto& parameters = block_type.parameters();
    if (stack.size() < parameters.size())
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < parameters.size(); ++i) {
        if (stack.take_last() != parameters[parameters.size() - i - 1])
            return Errors::invalid_stack_state();
    }

    for (auto& parameter : parameters)
        stack.append(parameter);

    m_entered_scopes.append(ChildScopeKind::Block);
    m_block_details.empend(stack.actual_size(), Empty {});
    m_parent_contexts.append(m_context);
    m_entered_blocks.append(block_type);
    m_context.labels.prepend(ResultType { block_type.results() });
    return {};
}

VALIDATE_INSTRUCTION(if_)
{
    auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
    auto block_type = TRY(validate(args.block_type));

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    auto& parameters = block_type.parameters();
    if (stack.size() < parameters.size())
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < parameters.size(); ++i) {
        if (stack.take_last() != parameters[parameters.size() - i])
            return Errors::invalid_stack_state();
    }

    for (auto& parameter : parameters)
        stack.append(parameter);

    m_entered_scopes.append(args.else_ip.has_value() ? ChildScopeKind::IfWithElse : ChildScopeKind::IfWithoutElse);
    m_block_details.empend(stack.actual_size(), BlockDetails::IfDetails { stack, {} });
    m_parent_contexts.append(m_context);
    m_entered_blocks.append(block_type);
    m_context.labels.prepend(ResultType { block_type.results() });
    return {};
}

VALIDATE_INSTRUCTION(br)
{
    auto label = instruction.arguments().get<LabelIndex>();
    TRY(validate(label));

    auto& type = m_context.labels[label.value()];
    if (stack.size() < type.types().size())
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < type.types().size(); ++i) {
        if (stack.take_last() != type.types()[type.types().size() - i - 1])
            return Errors::invalid_stack_state();
    }
    stack.append(StackEntry());
    return {};
}

VALIDATE_INSTRUCTION(br_if)
{
    auto label = instruction.arguments().get<LabelIndex>();
    TRY(validate(label));

    auto& type = m_context.labels[label.value()];
    if (stack.size() < type.types().size())
        return Errors::invalid_stack_state();

    Vector<StackEntry> entries;
    entries.ensure_capacity(type.types().size());

    for (size_t i = 0; i < type.types().size(); ++i) {
        auto entry = stack.take_last();
        if (entry != type.types()[type.types().size() - i - 1])
            return Errors::invalid_stack_state();
        entries.append(entry);
    }

    for (size_t i = 0; i < entries.size(); ++i)
        stack.append(entries[entries.size() - i - 1]);

    return {};
}

VALIDATE_INSTRUCTION(br_table)
{
    auto& args = instruction.arguments().get<Instruction::TableBranchArgs>();
    TRY(validate(args.default_));

    for (auto& label : args.labels)
        TRY(validate(label));

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    auto& default_types = m_context.labels[args.default_.value()].types();
    auto arity = default_types.size();
    if (stack.size() < arity)
        return Errors::invalid_stack_state();

    for (auto& label : args.labels) {
        auto& label_types = m_context.labels[label.value()].types();
        if (label_types.size() != arity)
            return Errors::invalid_stack_state();
        for (size_t i = 0; i < arity; ++i) {
            if (stack.at(stack.actual_size() - i - 1) != label_types[label_types.size() - i - 1])
                return Errors::invalid_stack_state();
        }
    }

    for (size_t i = 0; i < arity; ++i) {
        if (stack.take_last() != default_types[default_types.size() - i - 1])
            return Errors::invalid_stack_state();
    }

    return {};
}

VALIDATE_INSTRUCTION(return_)
{
    if (!m_context.return_.has_value())
        return Errors::invalid("use of return outside function");

    auto& return_types = m_context.return_->types();
    for (size_t i = 0; i < return_types.size(); ++i) {
        if (stack.is_empty() || stack.take_last() != return_types[return_types.size() - i - 1])
            return Errors::invalid_stack_state();
    }

    stack.append(StackEntry());

    return {};
}

VALIDATE_INSTRUCTION(call)
{
    auto index = instruction.arguments().get<FunctionIndex>();
    TRY(validate(index));

    auto& function_type = m_context.functions[index.value()];
    for (size_t i = 0; i < function_type.parameters().size(); ++i) {
        if (stack.is_empty() || stack.take_last() != function_type.parameters()[function_type.parameters().size() - i - 1])
            return Errors::invalid_stack_state();
    }

    for (auto& type : function_type.results())
        stack.append(type);

    return {};
}

VALIDATE_INSTRUCTION(call_indirect)
{
    auto& args = instruction.arguments().get<Instruction::IndirectCallArgs>();
    TRY(validate(args.table));
    TRY(validate(args.type));

    auto& table = m_context.tables[args.table.value()];
    if (!table.element_type().is_reference())
        return Errors::invalid("table element type for call.indirect", "a reference type", table.element_type());

    auto& type = m_context.types[args.type.value()];

    if (stack.is_empty() || !stack.take_last().is_of_kind(ValueType::I32))
        return Errors::invalid_stack_state();

    for (size_t i = 0; i < type.parameters().size(); ++i) {
        if (stack.is_empty() || stack.take_last() != type.parameters()[type.parameters().size() - i - 1])
            return Errors::invalid_stack_state();
    }

    for (auto& type : type.results())
        stack.append(type);

    return {};
}

ErrorOr<void, ValidationError> Validator::validate(const Instruction& instruction, Stack& stack, bool& is_constant)
{
    switch (instruction.opcode().value()) {
#define M(name, integer_value)       \
    case Instructions::name.value(): \
        return validate_instruction<integer_value>(instruction, stack, is_constant);

        ENUMERATE_WASM_OPCODES(M)

#undef M
    default:
        is_constant = false;
        return Errors::invalid("instruction opcode");
    }
}

ErrorOr<Validator::ExpressionTypeResult, ValidationError> Validator::validate(Expression const& expression, Vector<ValueType> const& result_types)
{
    Stack stack;
    bool is_constant_expression = true;

    for (auto& instruction : expression.instructions()) {
        bool is_constant = false;
        TRY(validate(instruction, stack, is_constant));

        is_constant_expression &= is_constant;
    }

    auto expected_result_types = result_types;
    while (!expected_result_types.is_empty()) {
        if (stack.is_empty())
            return Errors::invalid_stack_state();

        auto stack_type = stack.take_last();
        auto expected_type = expected_result_types.take_last();

        if (stack_type != expected_type)
            return Errors::invalid_stack_state();
    }

    for (auto& type : result_types)
        stack.append(type);

    return ExpressionTypeResult { stack.release_vector(), is_constant_expression };
}

bool Validator::Stack::operator==(const Stack& other) const
{
    if (!m_did_insert_unknown_entry && !other.m_did_insert_unknown_entry)
        return static_cast<Vector<StackEntry> const&>(*this) == static_cast<Vector<StackEntry> const&>(other);

    Optional<size_t> own_last_unknown_entry_index_from_end, other_last_unknown_entry_index_from_end;
    auto other_size = static_cast<Vector<StackEntry> const&>(other).size();
    auto own_size = Vector<StackEntry>::size();

    for (size_t i = 0; i < own_size; ++i) {
        if (other_size <= i)
            break;

        auto own_entry = at(own_size - i - 1);
        auto other_entry = other.at(other_size - i - 1);
        if (!own_entry.is_known) {
            own_last_unknown_entry_index_from_end = i;
            break;
        }

        if (!other_entry.is_known) {
            other_last_unknown_entry_index_from_end = i;
            break;
        }
    }

    if (!own_last_unknown_entry_index_from_end.has_value() && !other_last_unknown_entry_index_from_end.has_value()) {
        if (static_cast<Vector<StackEntry> const&>(other).is_empty() || Vector<StackEntry>::is_empty())
            return true;

        dbgln("Equality check internal error between");
        dbgln("stack:");
        for (auto& entry : *this)
            dbgln("- {}", entry.is_known ? Wasm::ValueType::kind_name(entry.concrete_type.kind()) : "<unknown>");
        dbgln("and stack:");
        for (auto& entry : other)
            dbgln("- {}", entry.is_known ? Wasm::ValueType::kind_name(entry.concrete_type.kind()) : "<unknown>");

        VERIFY_NOT_REACHED();
    }

    auto index_from_end = max(own_last_unknown_entry_index_from_end.value_or(0), other_last_unknown_entry_index_from_end.value_or(0));

    for (size_t i = 0; i < index_from_end; ++i) {
        if (at(own_size - i - 1) != other.at(other_size - i - 1))
            return false;
    }

    return true;
}

#if WASM_VALIDATOR_DEBUG
ValidationError Validator::Errors::invalid_stack_state(SourceLocation location)
{
    auto index = location.function_name().find('<');
    auto end_index = location.function_name().find('>');
    if (!index.has_value() || !end_index.has_value())
        return ValidationError { "Invalid stack state"sv };

    auto opcode = location.function_name().substring_view(index.value() + 1, end_index.value() - index.value() - 1).to_uint();
    if (!opcode.has_value())
        return ValidationError { "Invalid stack state"sv };

    auto name = instruction_name(OpCode { *opcode });
    return String::formatted("Invalid stack state for {}", name);
}
#else
ValidationError Validator::Errors::invalid_stack_state()
{
    return ValidationError { "Invalid stack state"sv };
}
#endif
}
