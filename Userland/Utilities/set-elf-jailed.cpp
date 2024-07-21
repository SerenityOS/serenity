/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibELF/Image.h>
#include <LibMain/Main.h>
#include <sys/mman.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView elf_file_path;
    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_positional_argument(elf_file_path, "ELF file to change", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio cpath rpath wpath"));

    Optional<ELF::Image::ProgramHeader&> maybe_interpreter_header;

    auto file_or_error = Core::MappedFile::map(elf_file_path, Core::MappedFile::Mode::ReadWrite);
    if (file_or_error.is_error())
        return Error::from_errno(EBADF);

    auto image = make<ELF::Image>(file_or_error.value()->bytes());

    image->for_each_program_header([&maybe_interpreter_header](auto program_header) {
        if (program_header.type() == PT_INTERP) {
            maybe_interpreter_header = program_header;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (!maybe_interpreter_header.has_value())
        return Error::from_errno(EINVAL);

    auto interpreter_header = maybe_interpreter_header.release_value();

    if (interpreter_header.offset() != interpreter_header.vaddr().get())
        return Error::from_errno(ENOTSUP);

    auto file = TRY(Core::File::open(elf_file_path, Core::File::OpenMode::ReadWrite));

    // FIXME: This is kinda hacky, but should work fine for now, as we don't
    // really want to parse the entire ELF file and change program hadears
    // intrusively for now.
    auto new_path = "/usr/lib/ldjail.so\0"sv;
    VERIFY(interpreter_header.size_in_image() >= new_path.length());
    TRY(file->seek(interpreter_header.offset(), SeekMode::SetPosition));
    TRY(file->write_some(new_path.bytes()));
    file->close();

    return 0;
}
