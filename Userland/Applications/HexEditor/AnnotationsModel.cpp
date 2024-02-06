/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AnnotationsModel.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>

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

Optional<Annotation&> AnnotationsModel::get_annotation(GUI::ModelIndex const& index)
{
    if (index.row() < 0 || index.row() >= row_count())
        return {};
    return m_annotations.at(index.row());
}

ErrorOr<void> AnnotationsModel::save_to_file(Core::File& file) const
{
    JsonArray array {};
    array.ensure_capacity(m_annotations.size());

    for (auto const& annotation : m_annotations) {
        JsonObject object;
        object.set("start_offset", annotation.start_offset);
        object.set("end_offset", annotation.end_offset);
        object.set("background_color", annotation.background_color.to_byte_string());
        object.set("comments", annotation.comments.to_byte_string());
        TRY(array.append(object));
    }

    auto json_string = array.to_byte_string();
    TRY(file.write_until_depleted(json_string.bytes()));

    return {};
}

ErrorOr<void> AnnotationsModel::load_from_file(Core::File& file)
{
    auto json_bytes = TRY(file.read_until_eof());
    StringView json_string { json_bytes };
    auto json = TRY(JsonValue::from_string(json_string));
    if (!json.is_array())
        return Error::from_string_literal("Failed to read annotations from file: Not a JSON array.");
    auto& json_array = json.as_array();

    Vector<Annotation> new_annotations;
    TRY(new_annotations.try_ensure_capacity(json_array.size()));
    TRY(json_array.try_for_each([&](JsonValue const& json_value) -> ErrorOr<void> {
        if (!json_value.is_object())
            return Error::from_string_literal("Failed to read annotation from file: Annotation not a JSON object.");
        auto& json_object = json_value.as_object();
        Annotation annotation;
        if (auto start_offset = json_object.get_u64("start_offset"sv); start_offset.has_value())
            annotation.start_offset = start_offset.value();
        if (auto end_offset = json_object.get_u64("end_offset"sv); end_offset.has_value())
            annotation.end_offset = end_offset.value();
        if (auto background_color = json_object.get_byte_string("background_color"sv).map([](auto& string) { return Color::from_string(string); }); background_color.has_value())
            annotation.background_color = background_color->value();
        if (auto comments = json_object.get_byte_string("comments"sv); comments.has_value())
            annotation.comments = MUST(String::from_byte_string(comments.value()));
        new_annotations.append(annotation);

        return {};
    }));

    m_annotations = move(new_annotations);
    invalidate();
    return {};
}
