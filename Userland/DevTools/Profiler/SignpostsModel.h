/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>

namespace Profiler {

class Profile;

class SignpostsModel final : public GUI::Model {
public:
    static NonnullRefPtr<SignpostsModel> create(Profile& profile)
    {
        return adopt_ref(*new SignpostsModel(profile));
    }

    enum Column {
        SignpostIndex,
        Timestamp,
        ProcessID,
        ThreadID,
        ExecutableName,
        SignpostString,
        SignpostArgument,
        __Count
    };

    virtual ~SignpostsModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual bool is_column_sortable(int) const override { return false; }

private:
    explicit SignpostsModel(Profile&);

    Profile& m_profile;
};

}
