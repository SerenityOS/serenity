/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDebug/DebugInfo.h>
#include <LibGUI/Model.h>
#include <LibX86/Instruction.h>

namespace Profiler {

class Profile;
class ProfileNode;

struct InstructionData {
    X86::Instruction insn;
    String disassembly;
    StringView bytes;
    FlatPtr address { 0 };
    u32 event_count { 0 };
    float percent { 0 };
    Debug::DebugInfo::SourcePositionWithInlines source_position_with_inlines;
};

class DisassemblyModel final : public GUI::Model {
public:
    static NonnullRefPtr<DisassemblyModel> create(Profile& profile, ProfileNode& node)
    {
        return adopt_ref(*new DisassemblyModel(profile, node));
    }

    enum Column {
        Address,
        SampleCount,
        InstructionBytes,
        Disassembly,
        SourceLocation,
        __Count
    };

    virtual ~DisassemblyModel() override;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual bool is_column_sortable(int) const override { return false; }

private:
    DisassemblyModel(Profile&, ProfileNode&);

    Profile& m_profile;
    ProfileNode& m_node;

    Vector<InstructionData> m_instructions;
};

}
