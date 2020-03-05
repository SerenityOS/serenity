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

#include "RemoteObjectPropertyModel.h"
#include "RemoteObject.h"

RemoteObjectPropertyModel::RemoteObjectPropertyModel(RemoteObject& object)
    : m_object(object)
{
}

int RemoteObjectPropertyModel::row_count(const GUI::ModelIndex&) const
{
    return m_properties.size();
}

String RemoteObjectPropertyModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    case Column::Value:
        return "Value";
    }
    ASSERT_NOT_REACHED();
}

GUI::Variant RemoteObjectPropertyModel::data(const GUI::ModelIndex& index, Role role) const
{
    auto& property = m_properties[index.row()];
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Name:
            return property.name;
        case Column::Value:
            return property.value.to_string();
        }
    }
    return {};
}

void RemoteObjectPropertyModel::update()
{
    m_properties.clear();
    m_object.json.for_each_member([this](auto& name, auto& value) {
        m_properties.append({ name, value });
    });
    did_update();
}
