/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>

namespace Profiler {

class Profile;
class ProfileNode;

struct SourceLineData {
    u32 event_count { 0 };
    float percent { 0 };
    ByteString location;
    u32 line_number { 0 };
    ByteString source_code;
};

class SourceModel final : public GUI::Model {
public:
    static NonnullRefPtr<SourceModel> create(Profile& profile, ProfileNode& node)
    {
        return adopt_ref(*new SourceModel(profile, node));
    }

    enum Column {
        SampleCount,
        Location,
        LineNumber,
        SourceCode,
        __Count
    };

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual bool is_column_sortable(int) const override { return false; }

private:
    SourceModel(Profile&, ProfileNode&);

    Profile& m_profile;
    ProfileNode& m_node;

    Vector<SourceLineData> m_source_lines;
};

}
