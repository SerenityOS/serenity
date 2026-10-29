/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibACPI/Definitions.h>
#include <LibACPI/Object.h>

namespace ACPI::AML {

enum class OperandType : u8 {
    NameString,
    PackageLength,
    ByteData,
    WordData,
    DWordData,
    QWordData,
    String,
    ByteBuffer,

    FieldElement,
    TermArg,

    DataRefObject,
    PackageElement,

    MarkAsCount,
    RepeatNextCount,
    RepeatNextBytes,
    End,
};

class OpcodeFrame {
public:
    static ErrorOr<OpcodeFrame> from_opcode(Opcode opcode, size_t start);
    static ErrorOr<OpcodeFrame> root_frame();

    bool filter_opcode_for_next_operand(Opcode opcode);

    ErrorOr<OperandType> begin_operand();
    ErrorOr<void> finish_operand(size_t current_offset);
    ErrorOr<void> finish_operand(Object const& operand, size_t current_offset);

    Object const& operand(size_t index) const { return m_operands[index]; }
    size_t operand_count() const { return m_operands.size(); }

    Vector<Object> release_operands() { return move(m_operands); }

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
    Vector<Object> m_operands;

    Vector<OperandType> m_operand_template;
    size_t m_start;

    size_t m_operand_template_index { 0 };

    size_t m_counter_value_index { 0 };
    Optional<size_t> m_counter;
};

}
