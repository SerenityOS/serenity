/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

static Vector<ByteString> found_libraries;

static ErrorOr<void> recusively_resolve_all_necessary_libraries(StringView interpreter_path, bool path_only_formatting, size_t recursive_iteration_max, size_t recursive_iteration, ELF::DynamicObject& object)
{
    if (recursive_iteration > recursive_iteration_max)
        return ELOOP;

    Vector<ByteString> libraries;
    object.for_each_needed_library([&libraries](StringView entry) {
        libraries.append(ByteString::formatted("{}", entry));
    });
    for (auto& library_name : libraries) {
        auto possible_library_path = ELF::DynamicLinker::resolve_library(library_name, object);
        if (!possible_library_path.has_value())
            continue;
        auto library_path = LexicalPath::absolute_path(TRY(Core::System::getcwd()), possible_library_path.value());
        if (found_libraries.contains_slow(library_path))
            continue;
        auto file = TRY(Core::MappedFile::map(library_path));

        auto elf_image_data = file->bytes();
        ELF::Image elf_image(elf_image_data);
        if (!elf_image.is_valid()) {
            outln("Shared library is not valid ELF: {}", library_path);
            continue;
        }
        if (!elf_image.is_dynamic()) {
            outln("Shared library is not dynamic loaded object: {}", library_path);
            continue;
        }

        int fd = TRY(Core::System::open(library_path, O_RDONLY));
        auto result = ELF::DynamicLoader::try_create(fd, library_path);
        if (result.is_error()) {
            outln("{}", result.error().text);
            continue;
        }
        auto& loader = result.value();
        if (!loader->is_valid()) {
            outln("{} is not a valid ELF dynamic shared object!", library_path);
            continue;
        }

        RefPtr<ELF::DynamicObject> library_object = loader->map();
        if (!library_object) {
            outln("Failed to map dynamic ELF object {}", library_path);
            continue;
        }

        if (path_only_formatting)
            outln("{}", library_path);
        else
            outln("{} => {}", library_name, library_path);

        recursive_iteration++;
        found_libraries.append(library_path);
        TRY(recusively_resolve_all_necessary_libraries(interpreter_path, path_only_formatting, recursive_iteration_max, recursive_iteration, *library_object));
    }
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath map_fixed"));

    ByteString path {};
    Optional<size_t> recursive_iteration_max;
    bool force_without_valid_interpreter = false;
    bool path_only_formatting = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive_iteration_max, "Max library resolving recursion", "max-recursion", 'r', "max recursion-level");
    args_parser.add_option(path_only_formatting, "Path-only format printing", "path-only-format", 's');
    args_parser.add_option(force_without_valid_interpreter, "Force library resolving on ELF object without valid interpreter", "force-without-valid-interpreter", 'f');
    args_parser.add_positional_argument(path, "ELF path", "path");
    args_parser.parse(arguments);

    path = LexicalPath::absolute_path(TRY(Core::System::getcwd()), path);

    auto file_or_error = Core::MappedFile::map(path);

    if (file_or_error.is_error()) {
        warnln("Unable to map file {}: {}", path, file_or_error.error());
        return -1;
    }

    auto elf_image_data = file_or_error.value()->bytes();
    ELF::Image elf_image(elf_image_data);

    if (!elf_image.is_valid()) {
        warnln("File is not a valid ELF object");
        return -1;
    }

    Optional<Elf_Phdr> interpreter_path_program_header {};
    if (!ELF::validate_program_headers(*bit_cast<Elf_Ehdr const*>(elf_image_data.data()), elf_image_data.size(), elf_image_data, interpreter_path_program_header)) {
        warnln("Invalid ELF headers");
        return -1;
    }

    StringBuilder interpreter_path_builder;
    if (interpreter_path_program_header.has_value())
        TRY(interpreter_path_builder.try_append({ elf_image_data.offset(interpreter_path_program_header.value().p_offset), static_cast<size_t>(interpreter_path_program_header.value().p_filesz) - 1 }));
    auto interpreter_path = interpreter_path_builder.string_view();

    RefPtr<ELF::DynamicObject> object = nullptr;
    if (elf_image.is_dynamic()) {
        if (interpreter_path != "/usr/lib/Loader.so"sv && !force_without_valid_interpreter) {
            warnln("ELF interpreter image is invalid");
            return 1;
        }

        int fd = TRY(Core::System::open(path, O_RDONLY));
        auto result = ELF::DynamicLoader::try_create(fd, path);
        if (result.is_error()) {
            outln("{}", result.error().text);
            return 1;
        }
        auto& loader = result.value();
        if (!loader->is_valid()) {
            outln("{} is not a valid ELF dynamic shared object!", path);
            return 1;
        }

        object = loader->map();
        if (!object) {
            outln("Failed to map dynamic ELF object {}", path);
            return 1;
        }
        TRY(recusively_resolve_all_necessary_libraries(interpreter_path, path_only_formatting, recursive_iteration_max.value_or(10), 0, *object));
    } else {
        outln("ELF program is not dynamic loaded!");
        return 1;
    }
    return 0;
}
