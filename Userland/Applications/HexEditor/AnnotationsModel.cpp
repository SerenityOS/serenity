/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AnnotationsModel.h"

GUI::Variant AnnotationsModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (index.row() < 0 || index.row() >= row_count())
        return {};

    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::CenterLeft;

    auto& annotation = m_annotations.at(index.row());
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Start:
            return MUST(String::formatted("{:#08X}", annotation.start_offset));
        case Column::End:
            return MUST(String::formatted("{:#08X}", annotation.end_offset));
        case Column::Comments:
            return annotation.comments;
        }
    }
    switch (to_underlying(role)) {
    case to_underlying(CustomRole::StartOffset):
        return annotation.start_offset;
    case to_underlying(CustomRole::EndOffset):
        return annotation.end_offset;
    case to_underlying(CustomRole::Comments):
        return annotation.comments;
    }

    return {};
}

void AnnotationsModel::add_annotation(Annotation annotation)
{
    m_annotations.append(move(annotation));
    invalidate();
}

void AnnotationsModel::delete_annotation(Annotation const& annotation)
{
    m_annotations.remove_first_matching([&](auto& other) {
        return other == annotation;
    });
    invalidate();
}

Optional<Annotation&> AnnotationsModel::closest_annotation_at(size_t position)
{
    // FIXME: If we end up with a lot of annotations, we'll need to store them and query them in a smarter way.
    Optional<Annotation&> result;
    for (auto& annotation : m_annotations) {
        if (annotation.start_offset <= position && position <= annotation.end_offset) {
            // If multiple annotations cover this position, use whichever starts latest. This would be the innermost one
            // if they overlap fully rather than partially.
            if (!result.has_value() || result->start_offset < annotation.start_offset)
                result = annotation;
        }
    }

    return result;
}
