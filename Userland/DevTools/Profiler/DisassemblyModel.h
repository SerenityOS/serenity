/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
        __Count
    };

    virtual ~DisassemblyModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual void update() override;
    virtual bool is_column_sortable(int) const override { return false; }

private:
    DisassemblyModel(Profile&, ProfileNode&);

    Profile& m_profile;
    ProfileNode& m_node;
    RefPtr<MappedFile> m_kernel_file;

    Vector<InstructionData> m_instructions;
};

}
