/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "VBWidgetPropertyModel.h"
#include "VBProperty.h"
#include "VBWidget.h"
#include <LibDraw/Font.h>

VBWidgetPropertyModel::VBWidgetPropertyModel(VBWidget& widget)
    : m_widget(widget)
{
}

VBWidgetPropertyModel::~VBWidgetPropertyModel()
{
}

int VBWidgetPropertyModel::row_count(const GModelIndex&) const
{
    return m_widget.m_properties.size();
}

String VBWidgetPropertyModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    case Column::Value:
        return "Value";
    case Column::Type:
        return "Type";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata VBWidgetPropertyModel::column_metadata(int column) const
{
    UNUSED_PARAM(column);
    if (column == Column::Name)
        return { 110, TextAlignment::CenterLeft, &Font::default_bold_font() };
    return { 90, TextAlignment::CenterLeft };
}

GVariant VBWidgetPropertyModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Custom) {
        auto& property = m_widget.m_properties[index.row()];
        if (index.column() == Column::Type)
            return (int)property.value().type();
        return {};
    }
    if (role == Role::Display) {
        auto& property = m_widget.m_properties[index.row()];
        switch (index.column()) {
        case Column::Name:
            return property.name();
        case Column::Value:
            return property.value();
        case Column::Type:
            return to_string(property.value().type());
        }
        ASSERT_NOT_REACHED();
    }
    if (role == Role::ForegroundColor) {
        auto& property = m_widget.m_properties[index.row()];
        switch (index.column()) {
        case Column::Name:
            return Color::Black;
        case Column::Type:
            return Color::Blue;
        case Column::Value:
            return property.is_readonly() ? Color(Color::MidGray) : Color(Color::Black);
        }
        ASSERT_NOT_REACHED();
    }
    return {};
}

void VBWidgetPropertyModel::set_data(const GModelIndex& index, const GVariant& value)
{
    ASSERT(index.column() == Column::Value);
    auto& property = m_widget.m_properties[index.row()];
    ASSERT(!property.is_readonly());
    property.set_value(value);
}

bool VBWidgetPropertyModel::is_editable(const GModelIndex& index) const
{
    if (index.column() != Column::Value)
        return false;
    auto& property = m_widget.m_properties[index.row()];
    return !property.is_readonly();
}
