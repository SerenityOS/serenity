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

#include "StylePropertiesModel.h"
#include <LibHTML/CSS/PropertyID.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/CSS/StyleProperties.h>

StylePropertiesModel::StylePropertiesModel(const StyleProperties& properties)
    : m_properties(properties)
{
    properties.for_each_property([&](auto property_id, auto& property_value) {
	Value value;
	value.name = CSS::string_from_property_id(property_id);
	value.value = property_value.to_string();
	m_values.append(value);
    });
}

int StylePropertiesModel::row_count(const GModelIndex&) const
{
    return m_values.size();
}

String StylePropertiesModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::PropertyName:
        return "Name";
    case Column::PropertyValue:
        return "Value";
    default:
        ASSERT_NOT_REACHED();
    }
}
GVariant StylePropertiesModel::data(const GModelIndex& index, Role role) const
{
    auto& value = m_values[index.row()];
    if (role == Role::Display) {
        if (index.column() == Column::PropertyName)
            return value.name;
        if (index.column() == Column::PropertyValue)
            return value.value;
    }
    return {};
}

void StylePropertiesModel::update()
{
    did_update();
}
