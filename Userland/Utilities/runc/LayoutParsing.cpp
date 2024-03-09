/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LayoutParsing.h"

namespace LayoutParsing {

struct JSONPropertyHandler {
    ErrorOr<bool> (*probe)(JsonObject const& object) = nullptr;
    ErrorOr<void> (*handle)(VFSRootContextLayout& layout, JsonObject const& object) = nullptr;
};

static ErrorOr<bool> mount_object_probe(JsonObject const& object)
{
    VERIFY(object.has_string("type"sv));
    auto type = object.get_byte_string("type"sv).value();
    if (type != "mount"sv)
        return false;

    if (!object.has_null("source"sv) && !object.has_string("source"sv))
        return Error::from_string_view("Object source property not found"sv);
    if (!object.has_string("target"sv))
        return Error::from_string_view("Object mount property not found"sv);
    if (!object.has_string("fs_type"sv))
        return Error::from_string_view("Object fs_type property not found"sv);

    return true;
}

static ErrorOr<void> mount_object_handle(VFSRootContextLayout& layout, JsonObject const& object)
{
    VERIFY(object.has_string("target"sv));
    VERIFY(object.has_string("source"sv) || object.has_null("source"sv));
    VERIFY(object.has_string("fs_type"sv));
    auto target = object.get_byte_string("target"sv).value();
    auto fs_type = object.get_byte_string("fs_type"sv).value();
    auto source = object.get_byte_string("source"sv);
    StringView actual_source = "none"sv;
    if (source.has_value())
        actual_source = source.value();
    TRY(layout.mount_new_filesystem(fs_type, actual_source, target, 0));
    return {};
}

static ErrorOr<bool> directory_object_probe(JsonObject const& object)
{
    VERIFY(object.has_string("type"sv));
    auto type = object.get_byte_string("type"sv).value();
    if (type != "directory"sv)
        return false;

    if (!object.has_string("target"sv))
        return Error::from_string_view("Object (directory type) target property not found"sv);

    return true;
}

static ErrorOr<void> directory_object_handle(VFSRootContextLayout& layout, JsonObject const& object)
{
    VERIFY(object.has_string("target"sv));
    auto target = object.get_byte_string("target"sv).value();
    TRY(layout.mkdir(target));
    return {};
}

static ErrorOr<bool> copy_custom_object_probe(JsonObject const& object)
{
    VERIFY(object.has_string("type"sv));
    auto type = object.get_byte_string("type"sv).value();
    if (type != "copy_custom"sv)
        return false;

    if (!object.has_string("source"sv))
        return Error::from_string_view("Object (copy_custom type) source property not found"sv);

    if (!object.has_string("target"sv))
        return Error::from_string_view("Object (copy_custom type) target property not found"sv);

    return true;
}

static ErrorOr<void> copy_custom_object_handle(VFSRootContextLayout& layout, JsonObject const& object)
{
    VERIFY(object.has_string("source"sv));
    VERIFY(object.has_string("target"sv));
    auto source = object.get_byte_string("source"sv).value();
    auto target = object.get_byte_string("target"sv).value();
    TRY(layout.copy_to_custom_location(source, target));
    return {};
}

static ErrorOr<bool> copy_original_object_probe(JsonObject const& object)
{
    VERIFY(object.has_string("type"sv));
    auto type = object.get_byte_string("type"sv).value();
    if (type != "copy_original"sv)
        return false;

    if (!object.has_array("sources"sv))
        return Error::from_string_view("Object (copy_original type) sources array property not found"sv);

    return true;
}

static ErrorOr<void> copy_original_object_handle(VFSRootContextLayout& layout, JsonObject const& object)
{
    VERIFY(object.has_array("sources"sv));
    auto sources = object.get_array("sources"sv).value();
    for (size_t index = 0; index < sources.size(); index++) {
        auto& path = sources[index];
        if (!path.is_string())
            return Error::from_string_view("Object (copy_original type) sources array property invalid"sv);
        TRY(layout.copy_as_original(path.as_string()));
    }
    return {};
}

static ErrorOr<bool> symlink_object_probe(JsonObject const& object)
{
    VERIFY(object.has_string("type"sv));
    auto type = object.get_byte_string("type"sv).value();
    if (type != "symlink"sv)
        return false;

    if (!object.has_string("path"sv))
        return Error::from_string_view("Object (symlink) path property not found"sv);
    if (!object.has_string("target"sv))
        return Error::from_string_view("Object (symlink) target property not found"sv);

    return true;
}

static ErrorOr<void> symlink_object_handle(VFSRootContextLayout& layout, JsonObject const& object)
{
    VERIFY(object.has_string("path"sv));
    VERIFY(object.has_string("target"sv));
    auto path = object.get_byte_string("path"sv).value();
    auto target = object.get_byte_string("target"sv).value();
    TRY(layout.symlink(path, target));
    return {};
}

static constexpr JSONPropertyHandler s_handlers[] = {
    { mount_object_probe, mount_object_handle },
    { directory_object_probe, directory_object_handle },
    { copy_custom_object_probe, copy_custom_object_handle },
    { copy_original_object_probe, copy_original_object_handle },
    { symlink_object_probe, symlink_object_handle },
};

static ErrorOr<void> handle_property(VFSRootContextLayout& layout, JsonObject const& object)
{
    for (auto& handler : s_handlers) {
        auto found_handler = TRY(handler.probe(object));
        if (!found_handler)
            continue;
        return handler.handle(layout, object);
    }

    auto type = object.get_byte_string("type"sv).value();
    dbgln("WARNING: Unknown object type - {}, it might affect layout creation severely", type);
    return {};
}

ErrorOr<void> handle_creation_sequence(VFSRootContextLayout& layout, JsonArray const& layout_creation_sequence)
{
    for (size_t index = 0; index < layout_creation_sequence.size(); index++) {
        auto& object = layout_creation_sequence[index];
        if (!object.is_object())
            return Error::from_string_view("Invalid layout JSON object"sv);
        if (!object.as_object().has_string("type"sv))
            return Error::from_string_view("Invalid layout JSON object - no type being specified"sv);
        TRY(handle_property(layout, object.as_object()));
    }
    return {};
}

}
