/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibDisassembly/x86/Instruction.h>
#include <LibGUI/Model.h>
#include <sys/arch/regs.h>

namespace Debug {

class DebugSession;

}

namespace HackStudio {

struct InstructionData {
    NonnullOwnPtr<Disassembly::Instruction> insn;
    ByteString disassembly;
    StringView bytes;
    FlatPtr address { 0 };
};

class DisassemblyModel final : public GUI::Model {
public:
    static NonnullRefPtr<DisassemblyModel> create(Debug::DebugSession const& debug_session, PtraceRegisters const& regs)
    {
        return adopt_ref(*new DisassemblyModel(debug_session, regs));
    }

    enum Column {
        Address,
        InstructionBytes,
        Disassembly,
        __Count
    };

    virtual ~DisassemblyModel() override = default;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

private:
    DisassemblyModel(Debug::DebugSession const&, PtraceRegisters const&);

    Vector<InstructionData> m_instructions;
};

}
