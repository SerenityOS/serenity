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

class IndividualSampleModel final : public GUI::Model {
public:
    static NonnullRefPtr<IndividualSampleModel> create(Profile& profile, size_t event_index)
    {
        return adopt_ref(*new IndividualSampleModel(profile, event_index));
    }

    enum Column {
        Address,
        ObjectName,
        Symbol,
        __Count
    };

    virtual ~IndividualSampleModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;

private:
    IndividualSampleModel(Profile&, size_t event_index);

    Profile& m_profile;
    size_t const m_event_index { 0 };
};

}
