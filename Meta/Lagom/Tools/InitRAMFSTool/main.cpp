/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <Kernel/API/Device.h>
#include <Kernel/API/initramfs_definitions.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

void create_inode(initramfs_inode& result_inode, size_t data_blocks_offset, struct stat& statbuf, size_t file_size, size_t inode_name_offset, size_t inode_name_length, size_t inode_blocks_count)
{
    // FIXME: Should we consider supporting non-ASCII UTF-8 characters?
    result_inode.name_offset = inode_name_offset;
    result_inode.name_length = inode_name_length;
    result_inode.file_size = file_size;
    result_inode.mode = statbuf.st_mode;
    result_inode.major = serenity_dev_major(statbuf.st_rdev);
    result_inode.minor = serenity_dev_minor(statbuf.st_rdev);
    result_inode.blocks_count = inode_blocks_count;
    result_inode.blocks_offset = data_blocks_offset;
    result_inode.uid = statbuf.st_uid;
    result_inode.gid = statbuf.st_gid;
    result_inode.mtime_seconds = statbuf.st_mtime;
    // FIXME: Find a way to put the nanoseconds value in platform-agnostic way.
    result_inode.mtime_nanoseconds = 0;
}

ErrorOr<void> write_inode_data_to_file(Core::Stream::File& output_file_stream, ReadonlyBytes file_buffer_bytes)
{
    auto nwritten = TRY(output_file_stream.write(file_buffer_bytes));
    if (nwritten != file_buffer_bytes.size())
        return Error::from_string_literal("InitRAMFSTool: Failed to write a complete file buffer.");
    return {};
}

enum class EntryType {
    Directory,
    File,
    Link,
};

static void print_warning(Error error, EntryType entry_type, StringView path)
{
    if (entry_type == EntryType::Directory) {
        warnln("Couldn't find directory '{}': {}", path, error);
        return;
    }
    if (entry_type == EntryType::File) {
        warnln("Couldn't find file '{}': {}", path, error);
        return;
    }
    if (entry_type == EntryType::Link) {
        warnln("Couldn't find link '{}': {}", path, error);
        return;
    }
    VERIFY_NOT_REACHED();
}

static void handle_statbuf_with_force_owners(struct stat& statbuf, Optional<size_t> const& force_uid, Optional<size_t> const& force_gid)
{
    if (force_uid.has_value())
        statbuf.st_uid = force_uid.value();
    if (force_gid.has_value())
        statbuf.st_uid = force_gid.value();
}

static ErrorOr<struct stat> handle_lstat_with_possible_forced_owners(EntryType entry_type, StringView path, Optional<size_t> const& force_uid, Optional<size_t> const& force_gid)
{
    auto statbuf_or_error = Core::System::lstat(path);
    if (statbuf_or_error.is_error()) {
        print_warning(Error::copy(statbuf_or_error.error()), entry_type, path);
        return statbuf_or_error.release_error();
    }
    auto statbuf = statbuf_or_error.release_value();
    handle_statbuf_with_force_owners(statbuf, force_uid, force_gid);
    return statbuf;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView output_file_path;
    StringView directory_to_handle;
    Optional<size_t> possible_alignment_power;
    Optional<size_t> force_uid;
    Optional<size_t> force_gid;
    bool force = false;
    Core::ArgsParser parser;
    parser.add_option(possible_alignment_power, "Data block alignment power", "data-block-alignment", 'a', "Alignment Power");
    parser.add_option(force, "Overwrite existing output file", "force", 'f');
    parser.add_option(force_uid, "Force UID", "force-uid", 'u', "Forced User Owner ID");
    parser.add_option(force_gid, "Force GID", "force-gid", 'g', "Forced Group Owner ID");
    parser.add_positional_argument(output_file_path, "Output file path", "outputfile", Core::ArgsParser::Required::Yes);
    parser.add_positional_argument(directory_to_handle, "Directory Path", "directory_path", Core::ArgsParser::Required::Yes);
    parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    auto cwd = TRY(Core::System::getcwd());
    TRY(Core::System::unveil(LexicalPath::absolute_path(cwd, output_file_path), "wc"sv));
    TRY(Core::System::unveil(LexicalPath::absolute_path(cwd, directory_to_handle), "r"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    if (Core::File::exists(output_file_path)) {
        if (force) {
            outln("{} already exists, overwriting...", output_file_path);
        } else {
            warnln("{} already exists, aborting!", output_file_path);
            return 1;
        }
    }

    outln("Archive: {}", output_file_path);

    size_t alignment_power = possible_alignment_power.value_or(12);
    if (alignment_power < 12 || alignment_power > 24)
        return Error::from_string_literal("InitRAMFSTool: Invalid alignment power being specified!");

    size_t block_size = 1 << alignment_power;

    initramfs_image_header header {};
    memset(&header, 0, sizeof(initramfs_image_header));
    memcpy(header.magic, "SERECPIO", 8);
    header.data_block_alignment_size_power_2 = alignment_power;
    // NOTE: We start by writing the data blocks section first, so we already
    // know the offset of this section which is 1 << alignment_power representing
    // the mathematical expression of 2 to the poewr of alignment_power.
    // Other section will be written after that section is done being written to.
    header.data_blocks_section_start = block_size;
    auto output_file_stream = TRY(Core::Stream::File::open(output_file_path, Core::Stream::OpenMode::Write));
    TRY(output_file_stream->truncate(block_size));

    // NOTE: Write the file header first. This already includes the magic bytes
    // and the alignment of data blocks as well as the start offset of the data
    // blocks section because we know these values.
    // Other important values will be written in the final stage when we "finalize"
    // processing the file.
    TRY(output_file_stream->write(ReadonlyBytes { &header, sizeof(initramfs_image_header) }));

    u32 current_data_blocks_count = 0;
    u32 current_inode_name_offset = 0;
    Vector<initramfs_inode> inodes;
    Vector<String> inodes_paths;

    // NOTE: Start writing data blocks section directly to the file now!
    TRY(output_file_stream->seek(block_size, SeekMode::SetPosition));

    auto add_link = [&](initramfs_inode& result_inode, struct stat& statbuf, StringView relative_root_child_path, StringView linked_path) -> ErrorOr<void> {
        size_t needed_padding_size = (1 << alignment_power) - (linked_path.length() % (block_size));
        size_t total_aligned_buffer_size = linked_path.length() + needed_padding_size;
        size_t inode_blocks_count = total_aligned_buffer_size / block_size;

        create_inode(result_inode, current_data_blocks_count, statbuf, linked_path.length(), current_inode_name_offset, relative_root_child_path.length(), inode_blocks_count);
        TRY(write_inode_data_to_file(*output_file_stream, linked_path.bytes()));

        // NOTE: Add the padding size to the location we are writing to, to
        // ensure we meet the alignment requirement always!
        TRY(output_file_stream->seek(needed_padding_size, SeekMode::FromCurrentPosition));
        return {};
    };

    auto add_file = [&](initramfs_inode& result_inode, struct stat& statbuf, Core::Stream::File& file, StringView relative_root_child_path) -> ErrorOr<void> {
        auto file_buffer = TRY(file.read_until_eof());
        size_t needed_padding_size = (1 << alignment_power) - (file_buffer.size() % (block_size));
        size_t total_aligned_buffer_size = file_buffer.size() + needed_padding_size;
        size_t inode_blocks_count = total_aligned_buffer_size / block_size;

        create_inode(result_inode, current_data_blocks_count, statbuf, file_buffer.size(), current_inode_name_offset, relative_root_child_path.length(), inode_blocks_count);
        TRY(write_inode_data_to_file(*output_file_stream, file_buffer.bytes()));

        // NOTE: Add the padding size to the location we are writing to, to
        // ensure we meet the alignment requirement always!
        TRY(output_file_stream->seek(needed_padding_size, SeekMode::FromCurrentPosition));
        return {};
    };

    auto add_directory = [&](DeprecatedString path, auto handle_directory) -> ErrorOr<void> {
        Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
        while (it.has_next()) {
            auto child_path = it.next_full_path();
            auto relative_root_child_path = child_path.substring_view(directory_to_handle.length());
            if (Core::File::is_directory(child_path)) {
                auto statbuf = TRY(handle_lstat_with_possible_forced_owners(EntryType::Directory, child_path, force_uid, force_gid));
                initramfs_inode inode_buf;
                memset(&inode_buf, 0, sizeof(initramfs_inode));
                inode_buf.name_offset = current_inode_name_offset;
                inode_buf.name_length = relative_root_child_path.length();
                inode_buf.file_size = 0;
                inode_buf.mode = statbuf.st_mode;
                inode_buf.uid = statbuf.st_uid;
                inode_buf.gid = statbuf.st_gid;
                if (force_uid.has_value())
                    inode_buf.uid = force_uid.value();
                if (force_gid.has_value())
                    inode_buf.gid = force_gid.value();
                inode_buf.mtime_seconds = statbuf.st_mtime;
                // FIXME: Find a way to put the nanoseconds value in platform-agnostic way.
                inode_buf.mtime_nanoseconds = 0;
                TRY(inodes.try_append(inode_buf));
                TRY(inodes_paths.try_append(TRY(String::from_utf8(relative_root_child_path))));
                current_inode_name_offset += relative_root_child_path.length();
                auto result = handle_directory(child_path, handle_directory);
                if (result.is_error())
                    warnln("Couldn't add directory '{}': {}", child_path, result.error());
            } else {
                initramfs_inode inode_buf;
                if (Core::File::is_link(child_path)) {
                    auto linked_path = TRY(Core::System::readlink(child_path));
                    auto statbuf = TRY(handle_lstat_with_possible_forced_owners(EntryType::Link, child_path, force_uid, force_gid));
                    TRY(add_link(inode_buf, statbuf, relative_root_child_path, linked_path));
                } else {
                    auto statbuf = TRY(handle_lstat_with_possible_forced_owners(EntryType::File, child_path, force_uid, force_gid));
                    auto file_stream = TRY(Core::Stream::File::open(child_path, Core::Stream::OpenMode::Read));
                    TRY(add_file(inode_buf, statbuf, *file_stream, relative_root_child_path));
                }
                TRY(inodes.try_append(inode_buf));
                TRY(inodes_paths.try_append(TRY(String::from_utf8(relative_root_child_path))));
                current_data_blocks_count += inode_buf.blocks_count;
                current_inode_name_offset += relative_root_child_path.length();
            }
        }
        return {};
    };

    if (Core::File::is_directory(directory_to_handle)) {
        auto result = add_directory(directory_to_handle, add_directory);
        if (result.is_error())
            warnln("Couldn't add directory '{}': {}", directory_to_handle, result.error());
    } else {
        warnln("Couldn't add non-directory for output file content '{}'", directory_to_handle);
        return 1;
    }

    // NOTE: Ensure file is truncated to have aligned size after finishing writing the
    // data blocks section.
    size_t inodes_section_start = block_size + current_data_blocks_count * block_size;
    TRY(output_file_stream->truncate(inodes_section_start));
    TRY(output_file_stream->seek(inodes_section_start, SeekMode::SetPosition));

    // NOTE: After the data blocks section, we put the inodes section.
    // Then, the inodes paths section is placed afterwards.
    header.inodes_section_start = inodes_section_start;
    header.inodes_names_section_start = inodes_section_start + sizeof(initramfs_inode) * inodes.size();

    header.inodes_count = inodes.size();
    header.data_blocks_count = current_data_blocks_count;
    for (auto& inode : inodes) {
        TRY(output_file_stream->write(ReadonlyBytes { &inode, sizeof(initramfs_inode) }));
    }

    for (auto& inode_path : inodes_paths) {
        TRY(output_file_stream->write(inode_path.bytes()));
    }

    TRY(output_file_stream->seek(0, SeekMode::SetPosition));
    TRY(output_file_stream->write(ReadonlyBytes { &header, sizeof(initramfs_image_header) }));
    return 0;
}
