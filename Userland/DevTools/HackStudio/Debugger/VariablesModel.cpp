/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "VariablesModel.h"
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>

namespace HackStudio {

GUI::ModelIndex VariablesModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid()) {
        if (static_cast<size_t>(row) >= m_variables.size())
            return {};
        return create_index(row, column, &m_variables[row]);
    }
    auto* parent = static_cast<const Debug::DebugInfo::VariableInfo*>(parent_index.internal_data());
    if (static_cast<size_t>(row) >= parent->members.size())
        return {};
    auto* child = &parent->members[row];
    return create_index(row, column, child);
}

GUI::ModelIndex VariablesModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* child = static_cast<const Debug::DebugInfo::VariableInfo*>(index.internal_data());
    auto* parent = child->parent;
    if (parent == nullptr)
        return {};

    if (parent->parent == nullptr) {
        for (size_t row = 0; row < m_variables.size(); row++)
            if (m_variables.ptr_at(row).ptr() == parent)
                return create_index(row, 0, parent);
        VERIFY_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent->members.size(); row++) {
        Debug::DebugInfo::VariableInfo* child_at_row = parent->parent->members.ptr_at(row).ptr();
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    VERIFY_NOT_REACHED();
}

int VariablesModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_variables.size();
    auto* node = static_cast<const Debug::DebugInfo::VariableInfo*>(index.internal_data());
    return node->members.size();
}

static String variable_value_as_string(const Debug::DebugInfo::VariableInfo& variable)
{
    if (variable.location_type != Debug::DebugInfo::VariableInfo::LocationType::Address)
        return "N/A";

    auto variable_address = variable.location_data.address;

    if (variable.is_enum_type()) {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        VERIFY(value.has_value());
        auto it = variable.type->members.find_if([&enumerator_value = value.value()](const auto& enumerator) {
            return enumerator->constant_data.as_u32 == enumerator_value;
        });
        if (it.is_end())
            return String::formatted("Unknown ({})", value.value());
        return String::formatted("{}::{}", variable.type_name, (*it)->name);
    }

    if (variable.type_name == "int") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        VERIFY(value.has_value());
        return String::formatted("{}", static_cast<int>(value.value()));
    }

    if (variable.type_name == "char") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        VERIFY(value.has_value());
        return String::formatted("'{0:c}'", (char)value.value());
    }

    if (variable.type_name == "bool") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        VERIFY(value.has_value());
        return (value.value() & 1) ? "true" : "false";
    }

    return String::formatted("type: {} @ {:p}, ", variable.type_name, variable_address);
}

static Optional<u32> string_to_variable_value(StringView string_value, const Debug::DebugInfo::VariableInfo& variable)
{
    if (variable.is_enum_type()) {
        auto prefix_string = String::formatted("{}::", variable.type_name);
        auto string_to_use = string_value;
        if (string_value.starts_with(prefix_string))
            string_to_use = string_value.substring_view(prefix_string.length(), string_value.length() - prefix_string.length());

        auto it = variable.type->members.find_if([string_to_use](const auto& enumerator) {
            return enumerator->name == string_to_use;
        });

        if (it.is_end())
            return {};
        return (*it)->constant_data.as_u32;
    }

    if (variable.type_name == "int") {
        auto value = string_value.to_int();
        if (value.has_value())
            return value.value();
        return {};
    }

    if (variable.type_name == "bool") {
        if (string_value == "true")
            return true;
        if (string_value == "false")
            return false;
        return {};
    }

    return {};
}

void VariablesModel::set_variable_value(const GUI::ModelIndex& index, StringView string_value, GUI::Window* parent_window)
{
    auto variable = static_cast<const Debug::DebugInfo::VariableInfo*>(index.internal_data());

    auto value = string_to_variable_value(string_value, *variable);

    if (value.has_value()) {
        auto success = Debugger::the().session()->poke((u32*)variable->location_data.address, value.value());
        VERIFY(success);
        return;
    }

    GUI::MessageBox::show(
        parent_window,
        String::formatted("String value \"{}\" could not be converted to a value of type {}.", string_value, variable->type_name),
        "Set value failed",
        GUI::MessageBox::Type::Error);
}

GUI::Variant VariablesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* variable = static_cast<const Debug::DebugInfo::VariableInfo*>(index.internal_data());
    switch (role) {
    case GUI::ModelRole::Display: {
        auto value_as_string = variable_value_as_string(*variable);
        return String::formatted("{}: {}", variable->name, value_as_string);
    }
    case GUI::ModelRole::Icon:
        return m_variable_icon;
    default:
        return {};
    }
}

RefPtr<VariablesModel> VariablesModel::create(Debug::ProcessInspector& inspector, PtraceRegisters const& regs)
{
    auto lib = inspector.library_at(regs.ip());
    if (!lib)
        return nullptr;
    auto variables = lib->debug_info->get_variables_in_current_scope(regs);
    return adopt_ref(*new VariablesModel(inspector, move(variables), regs));
}

}
