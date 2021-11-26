/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonPath.h>
#include <AK/JsonValue.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/Model.h>

namespace Inspector {

class RemoteObject;

class RemoteObjectPropertyModel final : public GUI::Model {
public:
    virtual ~RemoteObjectPropertyModel() override { }
    static NonnullRefPtr<RemoteObjectPropertyModel> create(RemoteObject& object)
    {
        return adopt_ref(*new RemoteObjectPropertyModel(object));
    }

    enum Column {
        Name,
        Value,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual void set_data(const GUI::ModelIndex&, const GUI::Variant&) override;
    virtual bool is_editable(const GUI::ModelIndex& index) const override { return index.column() == Column::Value; }
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;

private:
    explicit RemoteObjectPropertyModel(RemoteObject&);

    const JsonPath* cached_path_at(int n, const Vector<JsonPathElement>& prefix) const;
    const JsonPath* find_cached_path(const Vector<JsonPathElement>& path) const;

    RemoteObject& m_object;
    mutable NonnullOwnPtrVector<JsonPath> m_paths;
};

}
