/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Debugger.h"
#include <AK/NonnullOwnPtr.h>
#include <LibGUI/Model.h>
#include <LibGUI/TreeView.h>
#include <sys/arch/regs.h>

namespace HackStudio {

class VariablesModel final : public GUI::Model {
public:
    static RefPtr<VariablesModel> create(Debug::ProcessInspector&, PtraceRegisters const& regs);

    void set_variable_value(const GUI::ModelIndex&, StringView, GUI::Window*);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    Debug::ProcessInspector& inspector() { return m_inspector; }

private:
    explicit VariablesModel(Debug::ProcessInspector& inspector, Vector<NonnullOwnPtr<Debug::DebugInfo::VariableInfo>>&& variables, PtraceRegisters const& regs)
        : m_variables(move(variables))
        , m_regs(regs)
        , m_inspector(inspector)
    {
        m_variable_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"sv).release_value_but_fixme_should_propagate_errors());
    }
    Vector<NonnullOwnPtr<Debug::DebugInfo::VariableInfo>> m_variables;
    PtraceRegisters m_regs;

    GUI::Icon m_variable_icon;
    Debug::ProcessInspector& m_inspector;
};

}
