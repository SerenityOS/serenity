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

#include "VariablesModel.h"
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>

namespace HackStudio {

GUI::ModelIndex VariablesModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid())
        return create_index(row, column, &m_variables[row]);
    auto* parent = static_cast<const Debug::DebugInfo::VariableInfo*>(parent_index.internal_data());
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
        ASSERT_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent->members.size(); row++) {
        Debug::DebugInfo::VariableInfo* child_at_row = parent->parent->members.ptr_at(row).ptr();
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    ASSERT_NOT_REACHED();
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
        ASSERT(value.has_value());
        auto it = variable.type->members.find([enumerator_value = value.value()](auto& enumerator) {
            return enumerator->constant_data.as_u32 == enumerator_value;
        });
        ASSERT(!it.is_end());
        return String::formatted("{}::{}", variable.type_name, (*it)->name);
    }

    if (variable.type_name == "int") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        ASSERT(value.has_value());
        return String::formatted("{}", static_cast<int>(value.value()));
    }

    if (variable.type_name == "char") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        ASSERT(value.has_value());
        return String::formatted("'{0:c}' ({0:d})", value.value());
    }

    if (variable.type_name == "bool") {
        auto value = Debugger::the().session()->peek((u32*)variable_address);
        ASSERT(value.has_value());
        return (value.value() & 1) ? "true" : "false";
    }

    return String::formatted("type: {} @ {:p}, ", variable.type_name, variable_address);
}

static Optional<u32> string_to_variable_value(const StringView& string_value, const Debug::DebugInfo::VariableInfo& variable)
{
    if (variable.is_enum_type()) {
        auto prefix_string = String::formatted("{}::", variable.type_name);
        auto string_to_use = string_value;
        if (string_value.starts_with(prefix_string))
            string_to_use = string_value.substring_view(prefix_string.length(), string_value.length() - prefix_string.length());

        auto it = variable.type->members.find([string_to_use](auto& enumerator) {
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

void VariablesModel::set_variable_value(const GUI::ModelIndex& index, const StringView& string_value, GUI::Window* parent_window)
{
    auto variable = static_cast<const Debug::DebugInfo::VariableInfo*>(index.internal_data());

    auto value = string_to_variable_value(string_value, *variable);

    if (value.has_value()) {
        auto success = Debugger::the().session()->poke((u32*)variable->location_data.address, value.value());
        ASSERT(success);
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

void VariablesModel::update()
{
    did_update();
}

RefPtr<VariablesModel> VariablesModel::create(const PtraceRegisters& regs)
{
    auto variables = Debugger::the().session()->debug_info().get_variables_in_current_scope(regs);
    return adopt(*new VariablesModel(move(variables), regs));
}

}
