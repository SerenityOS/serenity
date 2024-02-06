/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

struct Annotation {
    size_t start_offset { 0 };
    size_t end_offset { 0 };
    Gfx::Color background_color { Color::from_argb(0xfffce94f) };
    String comments {};

    bool operator==(Annotation const& other) const = default;
};

class AnnotationsModel final : public GUI::Model {
public:
    enum Column {
        Start,
        End,
        Comments,
    };

    enum class CustomRole {
        StartOffset = to_underlying(GUI::ModelRole::Custom) + 1,
        EndOffset,
        Comments,
    };

    virtual int row_count(GUI::ModelIndex const& index = GUI::ModelIndex()) const override
    {
        if (!index.is_valid())
            return m_annotations.size();
        return 0;
    }

    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override
    {
        return 3;
    }

    virtual ErrorOr<String> column_name(int column) const override
    {
        switch (column) {
        case Column::Start:
            return "Start"_string;
        case Column::End:
            return "End"_string;
        case Column::Comments:
            return "Comments"_string;
        }
        VERIFY_NOT_REACHED();
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const override;

    void add_annotation(Annotation);
    void delete_annotation(Annotation const&);
    Optional<Annotation&> closest_annotation_at(size_t position);
    Optional<Annotation&> get_annotation(GUI::ModelIndex const& index);

    ErrorOr<void> save_to_file(Core::File&) const;
    ErrorOr<void> load_from_file(Core::File&);

private:
    Vector<Annotation> m_annotations;
};
