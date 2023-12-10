/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>

static ErrorOr<void> seek_and_read(size_t offset, ByteBuffer& buffer, Core::File& file)
{
    TRY(file.seek(offset, SeekMode::SetPosition));
    TRY(file.read_until_filled(buffer));
    return {};
}

static ErrorOr<void> seek_and_write(size_t offset, ByteBuffer& buffer, Core::File& file)
{
    TRY(file.seek(offset, SeekMode::SetPosition));
    TRY(file.write_until_depleted(buffer));
    return {};
}

static ErrorOr<void> process_file(Core::File& file, size_t block_size, size_t file_size, size_t file_size_rounded)
{
    size_t head = 0;
    size_t tail = file_size_rounded - block_size;
    size_t const last_block_size = file_size - tail;

    auto head_buffer = TRY(ByteBuffer::create_uninitialized(block_size));
    auto tail_buffer = TRY(ByteBuffer::create_uninitialized(last_block_size));

    auto stdout = TRY(Core::File::standard_output());

    // Overwrite the current block (after saving its contents to a temporary buffer) with the last block until we've processed half of the blocks in the file.
    while (head <= tail) {
        TRY(seek_and_read(head, head_buffer, file));
        TRY(seek_and_read(tail, tail_buffer, file));

        TRY(seek_and_write(head, tail_buffer, file));
        TRY(file.truncate(tail));

        TRY(stdout->write_until_depleted(head_buffer));

        if (tail_buffer.size() != block_size)
            TRY(tail_buffer.try_resize(block_size));

        head += block_size;
        tail -= block_size;
    };

    size_t remaining_size = file_size - head;

    // Note that we iterate downwards from the end of the file, as the above algorithm left all of the remaining blocks in reverse order.
    while (remaining_size) {
        size_t to_write = remaining_size >= block_size ? block_size : last_block_size;

        tail_buffer.trim(to_write, true);
        TRY(seek_and_read(tail, tail_buffer, file));

        TRY(file.truncate(tail));

        TRY(stdout->write_until_depleted(tail_buffer));

        tail -= to_write;
        remaining_size -= to_write;
    }

    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath rpath wpath"));
    StringView path;

    size_t block_size_in_kib = 256;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print file to stdout, while progressively deleting read segments.");
    args_parser.add_option(block_size_in_kib, "Base Block size in KiB, defaults to 256 KiB", "block-size", 'b', "base block size");
    args_parser.add_positional_argument(path, "File path", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    if (block_size_in_kib < 1)
        return Error::from_string_literal("Invalid block size");
    if (Checked<size_t>::multiplication_would_overflow(block_size_in_kib, KiB))
        return Error::from_string_literal("Overflow in block size");

    size_t block_size = block_size_in_kib * KiB;

    if (!FileSystem::exists(path))
        return Error::from_errno(ENOENT);

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::ReadWrite));
    size_t file_size = TRY(file->seek(0, SeekMode::FromEndPosition));
    if (file_size < block_size * 2)
        return Error::from_string_literal("Input file too small");

    size_t file_size_rounded = TRY(file->seek(ceil_div(file_size, block_size) * block_size, SeekMode::SetPosition));

    TRY(process_file(*file, block_size, file_size, file_size_rounded));

    TRY(FileSystem::remove(path, FileSystem::RecursionMode::Disallowed));

    return 0;
}
