/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "OpcodeFrame.h"
#include "Definitions.h"
#include "OpcodeFilters.h"

namespace ACPI::AML {

static Array<OperandType, 1> empty_operands = { OperandType::End };

static Array<OperandType, 3> scope_operands = { OperandType::PackageLength, OperandType::NameString, OperandType::End };
static Array<OperandType, 3> name_operands = { OperandType::NameString, OperandType::DataRefObject, OperandType::End };

static Array<OperandType, 2> byte_const_operands = { OperandType::ByteData, OperandType::End };
static Array<OperandType, 2> word_const_operands = { OperandType::WordData, OperandType::End };
static Array<OperandType, 2> dword_const_operands = { OperandType::DWordData, OperandType::End };
static Array<OperandType, 2> qword_const_operands = { OperandType::QWordData, OperandType::End };

static ErrorOr<Span<OperandType>> get_opcode_operand_template(Opcode opcode)
{
    switch (opcode) {
    case Opcode::BytePrefix:
        return byte_const_operands.span();
    case Opcode::WordPrefix:
        return word_const_operands.span();
    case Opcode::DWordPrefix:
        return dword_const_operands.span();
    case Opcode::QWordPrefix:
        return qword_const_operands.span();

    case Opcode::ScopeOp:
        return scope_operands.span();
    case Opcode::NameOp:
        return name_operands.span();

    case Opcode::ZeroOp:
    case Opcode::OneOp:
        return empty_operands.span();

    default:
        dbgln("[ACPI] get_opcode_operand_template: Unknown opcode template: {}!", opcode);
        return Error::from_string_view_or_print_error_and_return_errno("Unknown opcode template!"sv, EINVAL);
    }
}

ErrorOr<OpcodeFrame> OpcodeFrame::from_opcode(Opcode opcode, size_t start)
{
    auto operand_template = TRY(get_opcode_operand_template(opcode));
    return OpcodeFrame(opcode, TRY(Vector<OperandType>::from_span(operand_template)), start);
}

ErrorOr<OpcodeFrame> OpcodeFrame::root_frame()
{
    return OpcodeFrame({}, TRY(Vector<OperandType>::from_span(empty_operands)), 0);
}

OperandType OpcodeFrame::peek_next_operand()
{
    return m_operand_template[m_operand_template_index];
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
        return false;

    case OperandType::TermArg:
        return term_arg_filter(opcode);

    case OperandType::DataRefObject:
        return data_ref_object_filter(opcode);

    case OperandType::End:
        return term_filter(opcode);
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<OperandType> OpcodeFrame::begin_operand()
{
    return peek_next_operand();
}

ErrorOr<void> OpcodeFrame::finish_operand(NodeData const& operand)
{
    TRY(try_append(operand));

    m_operand_template_index++;
    return {};
}

}
