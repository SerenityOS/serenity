/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>

namespace Profiler {

class Profile;

class SamplesModel final : public GUI::Model {
public:
    static NonnullRefPtr<SamplesModel> create(Profile& profile)
    {
        return adopt_ref(*new SamplesModel(profile));
    }

    enum Column {
        SampleIndex,
        Timestamp,
        ProcessID,
        ThreadID,
        ExecutableName,
        LostSamples,
        InnermostStackFrame,
        Path,
        __Count
    };

    virtual ~SamplesModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual bool is_column_sortable(int) const override { return false; }

private:
    explicit SamplesModel(Profile&);

    Profile& m_profile;

    GUI::Icon m_user_frame_icon;
    GUI::Icon m_kernel_frame_icon;
};

}
