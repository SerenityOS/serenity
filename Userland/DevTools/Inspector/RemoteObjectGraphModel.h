/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/Model.h>

namespace Inspector {

class RemoteProcess;

class RemoteObjectGraphModel final : public GUI::Model {
public:
    static NonnullRefPtr<RemoteObjectGraphModel> create(RemoteProcess& process)
    {
        return adopt_ref(*new RemoteObjectGraphModel(process));
    }

    virtual ~RemoteObjectGraphModel() override = default;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;

private:
    explicit RemoteObjectGraphModel(RemoteProcess&);

    RemoteProcess& m_process;

    GUI::Icon m_object_icon;
    GUI::Icon m_window_icon;
    GUI::Icon m_layout_icon;
    GUI::Icon m_timer_icon;
};

}
