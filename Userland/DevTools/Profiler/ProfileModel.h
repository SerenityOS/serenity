/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>

namespace Profiler {

class Profile;

class ProfileModel final : public GUI::Model {
public:
    static NonnullRefPtr<ProfileModel> create(Profile& profile)
    {
        return adopt_ref(*new ProfileModel(profile));
    }

    enum Column {
        SampleCount,
        SelfCount,
        ObjectName,
        StackFrame,
        SymbolAddress,
        __Count
    };

    virtual ~ProfileModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual int tree_column() const override { return Column::StackFrame; }
    virtual bool is_column_sortable(int) const override { return false; }
    virtual bool is_searchable() const override { return true; }
    virtual Vector<GUI::ModelIndex> matches(StringView, unsigned flags, GUI::ModelIndex const&) override;

private:
    explicit ProfileModel(Profile&);

    Profile& m_profile;

    GUI::Icon m_user_frame_icon;
    GUI::Icon m_kernel_frame_icon;
};

}
