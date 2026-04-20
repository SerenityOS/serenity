/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LayoutParsing.h"

#include <AK/LexicalPath.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>

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

static ErrorOr<bool> copy_executable_object_probe(JsonObject const& object)
{
    VERIFY(object.has_string("type"sv));
    auto type = object.get_byte_string("type"sv).value();
    if (type != "copy_executable"sv)
        return false;

    if (!object.has_string("source"sv))
        return Error::from_string_view("Object (copy_executable type) source property not found"sv);

    if (!object.has_string("target"sv))
        return Error::from_string_view("Object (copy_executable type) target property not found"sv);

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

static ErrorOr<void> copy_executable_object_handle(VFSRootContextLayout& layout, JsonObject const& object)
{
    VERIFY(object.has_string("source"sv));
    VERIFY(object.has_string("target"sv));
    auto source = object.get_byte_string("source"sv).value();
    auto target = object.get_byte_string("target"sv).value();

    // First, try to copy the actual executable...
    TRY(layout.copy_to_custom_location(source, target));

    auto path = LexicalPath::absolute_path(TRY(Core::System::getcwd()), source);

    auto file = TRY(Core::MappedFile::map(path));

    auto elf_image_data = file->bytes();
    ELF::Image elf_image(elf_image_data);

    if (!elf_image.is_valid()) {
        dbgln("File is not a valid ELF object");
        return EINVAL;
    }

    Optional<Elf_Phdr> interpreter_path_program_header {};
    if (!ELF::validate_program_headers(*bit_cast<Elf_Ehdr const*>(elf_image_data.data()), elf_image_data.size(), elf_image_data, interpreter_path_program_header))
        return EINVAL;

    StringBuilder interpreter_path_builder;
    if (interpreter_path_program_header.has_value())
        TRY(interpreter_path_builder.try_append({ elf_image_data.offset(interpreter_path_program_header.value().p_offset), static_cast<size_t>(interpreter_path_program_header.value().p_filesz) - 1 }));
    auto interpreter_path = interpreter_path_builder.string_view();

    RefPtr<ELF::DynamicObject> dynamic_object = nullptr;
    if (!elf_image.is_dynamic()) {
        dbgln("ELF program is not dynamic loaded!");
        return EINVAL;
    }

    // We technically can support this if we want to
    if (interpreter_path != "/usr/lib/Loader.so"sv)
        return EINVAL;

    int fd = TRY(Core::System::open(path, O_RDONLY));
    auto result = ELF::DynamicLoader::try_create(fd, path);
    if (result.is_error()) {
        outln("{}", result.error().text);
        return EINVAL;
    }
    auto& loader = result.value();
    if (!loader->is_valid())
        return EINVAL;

    dynamic_object = loader->map();
    if (!dynamic_object)
        return ENOTSUP;

    Vector<ByteString> found_libraries;
    // FIXME: Is 50 really enough for max recursion? It seems like the Browser can fail on less than 35
    TRY(dynamic_object->recusively_resolve_dynamic_dependency_paths(found_libraries,
        50,
        0,
        [&layout](ByteString, ELF::DynamicObject::DynamicDependecyPath library_path) -> ErrorOr<void> {
            TRY(layout.copy_to_custom_location(library_path, library_path));
            return {};
        }));
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

static ErrorOr<bool> bindmount_object_probe(JsonObject const& object)
{
    VERIFY(object.has_string("type"sv));
    auto type = object.get_byte_string("type"sv).value();
    if (type != "bindmount"sv)
        return false;

    if (!object.has_string("source"sv))
        return Error::from_string_view("Object (bindmount) source property not found"sv);
    if (!object.has_string("target"sv))
        return Error::from_string_view("Object (bindmount) target property not found"sv);

    return true;
}

static ErrorOr<void> bindmount_object_handle(VFSRootContextLayout& layout, JsonObject const& object)
{
    VERIFY(object.has_string("source"sv));
    VERIFY(object.has_string("target"sv));
    auto path = object.get_byte_string("source"sv).value();
    auto target = object.get_byte_string("target"sv).value();
    TRY(layout.bindmount(path, target));
    return {};
}

static constexpr JSONPropertyHandler s_handlers[] = {
    { mount_object_probe, mount_object_handle },
    { directory_object_probe, directory_object_handle },
    { copy_custom_object_probe, copy_custom_object_handle },
    { copy_executable_object_probe, copy_executable_object_handle },
    { copy_original_object_probe, copy_original_object_handle },
    { bindmount_object_probe, bindmount_object_handle },
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
    if (type != "root_mount"sv)
        dbgln("WARNING: Unknown object type - {}, it might affect layout creation severely", type);
    return {};
}

ErrorOr<void> create_root_mount_point(StringView preparation_environment_path, JsonArray const& layout_creation_sequence)
{
    for (size_t index = 0; index < layout_creation_sequence.size(); index++) {
        auto& maybe_object = layout_creation_sequence[index];
        if (!maybe_object.is_object())
            return Error::from_string_view("Invalid layout JSON object"sv);
        if (!maybe_object.as_object().has_string("type"sv))
            return Error::from_string_view("Invalid layout JSON object - no type being specified"sv);

        auto& object = maybe_object.as_object();

        auto type = object.get_byte_string("type"sv).value();
        if (type != "root_mount"sv)
            continue;

        if (!object.has_null("source"sv) && !object.has_string("source"sv))
            return Error::from_string_view("Object root_mount source property not found"sv);
        if (!object.has_string("fs_type"sv))
            return Error::from_string_view("Object root_mount fs_type property not found"sv);

        auto fs_type = object.get_byte_string("fs_type"sv).value();
        auto source = object.get_byte_string("source"sv).value_or("none");
        auto source_fd = TRY(VFSRootContextLayout::get_source_fd(source));
        TRY(Core::System::mount({}, source_fd, preparation_environment_path, fs_type, 0));
    }
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
