/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <LibGUI/Model.h>

class ManualModel final : public GUI::Model {
public:
    static NonnullRefPtr<ManualModel> create()
    {
        return adopt_ref(*new ManualModel);
    }

    virtual ~ManualModel() override {};

    Optional<GUI::ModelIndex> index_from_path(const StringView&) const;

    String page_path(const GUI::ModelIndex&) const;
    String page_and_section(const GUI::ModelIndex&) const;
    Result<StringView, OSError> page_view(const String& path) const;

    void update_section_node_on_toggle(const GUI::ModelIndex&, const bool);
    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual TriState data_matches(const GUI::ModelIndex&, const GUI::Variant&) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;

private:
    ManualModel();

    GUI::Icon m_section_open_icon;
    GUI::Icon m_section_icon;
    GUI::Icon m_page_icon;
    mutable HashMap<String, NonnullRefPtr<MappedFile>> m_mapped_files;
};
