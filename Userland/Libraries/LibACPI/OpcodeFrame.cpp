/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibACPI/Definitions.h>
#include <LibACPI/OpcodeFilters.h>
#include <LibACPI/OpcodeFrame.h>

#ifdef KERNEL
#    define warnln dbgln
#endif

namespace ACPI::AML {

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#aml-byte-stream-byte-values
constexpr static auto EMPTY_OPERANDS = to_array({ OperandType::End });

constexpr static auto SCOPE_OPERANDS = to_array({ OperandType::PackageLength, OperandType::NameString, OperandType::End });
constexpr static auto NAME_OPERANDS = to_array({ OperandType::NameString, OperandType::DataRefObject, OperandType::End });

constexpr static auto BYTE_CONST_OPERANDS = to_array({ OperandType::ByteData, OperandType::End });
constexpr static auto WORD_CONST_OPERANDS = to_array({ OperandType::WordData, OperandType::End });
constexpr static auto DWORD_CONST_OPERANDS = to_array({ OperandType::DWordData, OperandType::End });
constexpr static auto QWORD_CONST_OPERANDS = to_array({ OperandType::QWordData, OperandType::End });
constexpr static auto STRING_CONST_OPERANDS = to_array({ OperandType::String, OperandType::End });

constexpr static auto BUFFER_CONST_OPERANDS = to_array({
    OperandType::PackageLength,
    OperandType::TermArg,
    OperandType::ByteBuffer,
    OperandType::End,
});

constexpr static auto PACKAGE_OPERANDS = to_array({
    OperandType::MarkAsCount,
    OperandType::PackageLength,
    OperandType::ByteData,
    OperandType::RepeatNextBytes,
    OperandType::PackageElement,
    OperandType::End,
});

constexpr static auto VAR_PACKAGE_OPERANDS = to_array({
    OperandType::MarkAsCount,
    OperandType::PackageLength,
    OperandType::TermArg,
    OperandType::RepeatNextBytes,
    OperandType::PackageElement,
    OperandType::End,
});

constexpr static auto ALIAS_OPERANDS = to_array({
    OperandType::NameString,
    OperandType::NameString,
    OperandType::End,
});

constexpr static auto METHOD_OPERANDS = to_array({
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::End,
});

constexpr static auto MUTEX_OPERANDS = to_array({
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::End,
});

constexpr static auto OP_REGION_OPERANDS = to_array({
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::TermArg,
    OperandType::TermArg,
    OperandType::End,
});

constexpr static auto FIELD_OPERANDS = to_array({
    OperandType::MarkAsCount,
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::RepeatNextBytes,
    OperandType::FieldElement,
    OperandType::End,
});

constexpr static auto DEVICE_OPERANDS = to_array({
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::End,
});

constexpr static auto PROCESSOR_OPERANDS = to_array({
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::DWordData,
    OperandType::ByteData,
    OperandType::End,
});

constexpr static auto POWER_RESOURCE_OPERANDS = to_array({
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::WordData,
    OperandType::End,
});

constexpr static auto INDEX_FIELD_OPERANDS = to_array({
    OperandType::MarkAsCount,
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::RepeatNextBytes,
    OperandType::IndexFieldElement,
    OperandType::End,
});

constexpr static auto TARGET_BINARY_OPERANDS = to_array({
    OperandType::TermArg,
    OperandType::TermArg,
    OperandType::Target,
    OperandType::End,
});

constexpr static auto BINARY_OPERANDS = to_array({
    OperandType::TermArg,
    OperandType::TermArg,
    OperandType::End,
});

constexpr static auto UNARY_OPERANDS = to_array({
    OperandType::TermArg,
    OperandType::End,
});

constexpr static auto STORE_OPERANDS = to_array({
    OperandType::TermArg,
    OperandType::SuperName,
    OperandType::End,
});

constexpr static auto IF_OPERANDS = to_array({
    OperandType::PackageLength,
    OperandType::TermArg,
    OperandType::End,
});

constexpr static auto ELSE_OPERANDS = to_array({
    OperandType::PackageLength,
    OperandType::End,
});

constexpr static auto RETURN_OPERANDS = to_array({
    OperandType::TermArg,
    OperandType::End,
});

static ErrorOr<ReadonlySpan<OperandType>> get_opcode_operand_template(Opcode opcode)
{
    switch (opcode) {
    case Opcode::BytePrefix:
        return BYTE_CONST_OPERANDS.span();
    case Opcode::WordPrefix:
        return WORD_CONST_OPERANDS.span();
    case Opcode::DWordPrefix:
        return DWORD_CONST_OPERANDS.span();
    case Opcode::QWordPrefix:
        return QWORD_CONST_OPERANDS.span();
    case Opcode::StringPrefix:
        return STRING_CONST_OPERANDS.span();
    case Opcode::BufferOp:
        return BUFFER_CONST_OPERANDS.span();
    case Opcode::PackageOp:
        return PACKAGE_OPERANDS.span();
    case Opcode::VarPackageOp:
        return VAR_PACKAGE_OPERANDS.span();

    case Opcode::AliasOp:
        return ALIAS_OPERANDS.span();
    case Opcode::ScopeOp:
        return SCOPE_OPERANDS.span();
    case Opcode::NameOp:
        return NAME_OPERANDS.span();
    case Opcode::MethodOp:
        return METHOD_OPERANDS.span();
    case Opcode::MutexOp:
        return MUTEX_OPERANDS.span();
    case Opcode::OpRegionOp:
        return OP_REGION_OPERANDS.span();
    case Opcode::FieldOp:
        return FIELD_OPERANDS.span();
    case Opcode::DeviceOp:
        return DEVICE_OPERANDS.span();
    case Opcode::ProcessorOp:
        return PROCESSOR_OPERANDS.span();
    case Opcode::PowerResOp:
        return POWER_RESOURCE_OPERANDS.span();
    case Opcode::IndexFieldOp:
        return INDEX_FIELD_OPERANDS.span();

    case Opcode::AddOp:
    case Opcode::SubtractOp:
    case Opcode::MultiplyOp:
    case Opcode::ShiftLeftOp:
    case Opcode::ShiftRightOp:
    case Opcode::AndOp:
    case Opcode::NandOp:
    case Opcode::OrOp:
    case Opcode::NorOp:
    case Opcode::XorOp:
        return TARGET_BINARY_OPERANDS.span();

    case Opcode::LAndOp:
    case Opcode::LOrOp:
    case Opcode::LEqualOp:
    case Opcode::LGreaterOp:
    case Opcode::LLessOp:
        return BINARY_OPERANDS.span();

    case Opcode::LNotOp:
        return UNARY_OPERANDS.span();

    case Opcode::StoreOp:
        return STORE_OPERANDS.span();

    case Opcode::IfOp:
        return IF_OPERANDS.span();
    case Opcode::ElseOp:
        return ELSE_OPERANDS.span();
    case Opcode::ReturnOp:
        return RETURN_OPERANDS.span();

    case Opcode::ZeroOp:
    case Opcode::OneOp:
    case Opcode::OnesOp:
    case Opcode::Local0Op... Opcode::Local7Op:
    case Opcode::Arg0Op... Opcode::Arg6Op:
        return EMPTY_OPERANDS.span();

    case Opcode::NodeEvaluation:
    case Opcode::EvaluatedValue:
        VERIFY_NOT_REACHED();

    default:
        warnln("ACPI: get_opcode_operand_template: Unknown opcode template: {}!", opcode);
        return Error::from_string_view_or_print_error_and_return_errno("Unknown opcode template!"sv, EINVAL);
    }
}

ErrorOr<OpcodeFrame> OpcodeFrame::from_opcode(Opcode opcode, size_t start, Optional<ReadonlySpan<OperandType>> operand_template)
{
    if (!operand_template.has_value())
        operand_template = TRY(get_opcode_operand_template(opcode));

    auto frame = OpcodeFrame(opcode, TRY(Vector<OperandType>::from_span(*operand_template)), start);
    return frame;
}

ErrorOr<OpcodeFrame> OpcodeFrame::root_frame()
{
    return OpcodeFrame({}, TRY(Vector<OperandType>::from_span(EMPTY_OPERANDS)), 0);
}

OperandType OpcodeFrame::peek_next_operand()
{
    auto operand = m_operand_template[m_operand_template_index];
    if (operand == OperandType::MarkAsCount || operand == OperandType::RepeatNextBytes)
        operand = m_operand_template[m_operand_template_index + 1];

    return operand;
}

bool OpcodeFrame::filter_opcode_for_next_operand(Opcode opcode)
{
    auto pending_operand = peek_next_operand();

    switch (pending_operand) {
    case OperandType::NameString:
    case OperandType::PackageLength:
    case OperandType::ByteData:
    case OperandType::WordData:
    case OperandType::DWordData:
    case OperandType::QWordData:
    case OperandType::String:
    case OperandType::ByteBuffer:
    case OperandType::FieldElement:
    case OperandType::IndexFieldElement:
        return false;

    case OperandType::TermArg:
        return term_arg_filter(opcode);

    case OperandType::DataRefObject:
        return data_ref_object_filter(opcode);
    case OperandType::PackageElement:
        return package_element_filter(opcode);
    case OperandType::SuperName:
        return super_name_filter(opcode);
    case OperandType::Target:
        return target_filter(opcode);

    case OperandType::End:
        return term_filter(opcode);

    case OperandType::MarkAsCount:
    case OperandType::RepeatNextBytes:
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<OperandType> OpcodeFrame::begin_operand(size_t current_offset)
{
    auto operand = m_operand_template[m_operand_template_index];

    if (operand == OperandType::MarkAsCount) {
        m_counter_value_index = m_operands.size();
    } else if (operand == OperandType::RepeatNextBytes) {
        if (!m_counter.has_value()) {
            m_counter = TRY(m_operands.at(m_counter_value_index).as<u64>());
            check_repeat_opcode(current_offset);
        }
    }

    return peek_next_operand();
}

ErrorOr<void> OpcodeFrame::finish_operand(size_t current_offset)
{
    auto operand_type = m_operand_template[m_operand_template_index];
    if (operand_type == OperandType::MarkAsCount) {
        m_operand_template_index += 2;
        return {};
    }

    if (!check_repeat_opcode(current_offset))
        m_operand_template_index++;

    return {};
}

ErrorOr<void> OpcodeFrame::finish_operand(Object&& operand, size_t current_offset)
{
    TRY(m_operands.try_append(move(operand)));
    return finish_operand(current_offset);
}

bool OpcodeFrame::check_repeat_opcode(size_t current_offset)
{
    auto operand_type = m_operand_template[m_operand_template_index];

    if (operand_type == OperandType::RepeatNextBytes) {
        if (current_offset >= m_start + m_counter.value()) {
            m_counter = {};
            m_operand_template_index += 2;
            return true;
        }

        return true;
    }

    return false;
}

}
