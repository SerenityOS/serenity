/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntegralMath.h>
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

    virtual ~ProfileModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(GUI::ModelIndex const&) const override;
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
