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
    static ErrorOr<NonnullRefPtr<ManualModel>> create()
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) ManualModel);
    }

    virtual ~ManualModel() override {};

    Optional<GUI::ModelIndex> index_from_path(StringView) const;

    Optional<String> page_name(const GUI::ModelIndex&) const;
    Optional<String> page_path(const GUI::ModelIndex&) const;
    Optional<String> page_and_section(const GUI::ModelIndex&) const;
    ErrorOr<StringView> page_view(String const& path) const;

    void update_section_node_on_toggle(const GUI::ModelIndex&, bool const);
    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::Model::MatchResult data_matches(const GUI::ModelIndex&, const GUI::Variant&) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;

private:
    ManualModel();

    GUI::Icon m_section_open_icon;
    GUI::Icon m_section_icon;
    GUI::Icon m_page_icon;
    mutable HashMap<String, NonnullOwnPtr<Core::MappedFile>> m_mapped_files;
};
