/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "DebugInfoWidget.h"
#include "Debugger.h"
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Model.h>
#include <LibGUI/TreeView.h>

GUI::ModelIndex DebugInfoModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid())
        return create_index(row, column, &m_variables[row]);
    auto* parent = static_cast<const DebugInfo::VariableInfo*>(parent_index.internal_data());
    auto* child = &parent->members[row];
    return create_index(row, column, child);
}

GUI::ModelIndex DebugInfoModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* child = static_cast<const DebugInfo::VariableInfo*>(index.internal_data());
    auto* parent = child->parent;
    if (parent == nullptr)
        return {};

    if (parent->parent == nullptr) {
        for (size_t row = 0; row < m_variables.size(); row++)
            if (m_variables.ptr_at(row).ptr() == parent)
                return create_index(row, 0, parent);
        ASSERT_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent->members.size(); row++) {
        DebugInfo::VariableInfo* child_at_row = parent->parent->members.ptr_at(row).ptr();
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    ASSERT_NOT_REACHED();
}

int DebugInfoModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_variables.size();
    auto* node = static_cast<const DebugInfo::VariableInfo*>(index.internal_data());
    return node->members.size();
}

String variable_value_as_string(const DebugInfo::VariableInfo& variable)
{
    if (variable.location_type != DebugInfo::VariableInfo::LocationType::Address)
        return "N/A";

    auto variable_address = variable.location_data.address;

    if (variable.type == "int") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        ASSERT(value.has_value());
        return String::format("%d", static_cast<int>(value.value()));
    }

    if (variable.type == "char") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        ASSERT(value.has_value());
        return String::format("'%c' (%d)", static_cast<char>(value.value()), static_cast<char>(value.value()));
    }

    return String::format("type: %s @ %08x, ", variable.type.characters(), variable_address);
}

GUI::Variant DebugInfoModel::data(const GUI::ModelIndex& index, Role role) const
{
    auto* variable = static_cast<const DebugInfo::VariableInfo*>(index.internal_data());
    switch (role) {
    case Role::Display: {
        auto value_as_string = variable_value_as_string(*variable);
        return String::format("%s: %s", variable->name.characters(), value_as_string.characters());
    }
    case Role::Icon:
        return m_variable_icon;
    default:
        return {};
    }
}

void DebugInfoModel::update()
{
    did_update();
}

static RefPtr<DebugInfoModel> create_model(const PtraceRegisters& regs)
{
    auto variables = Debugger::the().session()->debug_info().get_variables_in_current_scope(regs);
    return adopt(*new DebugInfoModel(move(variables), regs));
}

DebugInfoWidget::DebugInfoWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    m_info_view = add<GUI::TreeView>();
}

void DebugInfoWidget::update_variables(const PtraceRegisters& regs)
{
    auto model = create_model(regs);
    m_info_view->set_model(model);
}

void DebugInfoWidget::program_stopped()
{
    m_info_view->set_model({});
}
