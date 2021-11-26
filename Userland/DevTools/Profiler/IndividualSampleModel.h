/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

    virtual ~IndividualSampleModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

private:
    IndividualSampleModel(Profile&, size_t event_index);

    Profile& m_profile;
    const size_t m_event_index { 0 };
};

}
