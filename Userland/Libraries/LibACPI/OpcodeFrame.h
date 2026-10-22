/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Definitions.h"
#include "NodeData.h"
#include <AK/Vector.h>

namespace ACPI::AML {

enum class OperandType : u8 {
    NameString,
    PackageLength,
    ByteData,
    WordData,
    DWordData,
    QWordData,

    TermArg,

    DataRefObject,

    End,
};

class OpcodeFrame : public Vector<NodeData> {
public:
    static ErrorOr<OpcodeFrame> from_opcode(Opcode opcode, size_t start);
    static ErrorOr<OpcodeFrame> root_frame();

    bool filter_opcode_for_next_operand(Opcode opcode);

    ErrorOr<OperandType> begin_operand();
    ErrorOr<void> finish_operand(NodeData const& operand);

    // NOTE: This should never get called on the root frame
    Opcode opcode() const { return m_opcode.value(); }
    size_t start() const { return m_start; }

private:
    OpcodeFrame(Optional<Opcode> opcode, Vector<OperandType> operand_template, size_t start)
        : m_opcode(opcode)
        , m_operand_template(move(operand_template))
        , m_start(start)
    {
    }

    OperandType peek_next_operand();

    Optional<Opcode> m_opcode;
    Vector<OperandType> m_operand_template;
    size_t m_start;

    size_t m_operand_template_index { 0 };
};

}
