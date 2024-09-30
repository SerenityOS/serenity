/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <limits.h>
#include <string.h>

struct DuOption {
    enum class TimeType {
        NotUsed,
        Modification,
        Access,
        Status
    };

    bool human_readable = false;
    bool human_readable_si = false;
    bool all = false;
    bool apparent_size = false;
    bool one_file_system = false;
    bool print_total_size = false;
    TimeType time_type = TimeType::NotUsed;
    Vector<ByteString> excluded_patterns;
    u64 block_size = 1024;
    size_t max_depth = SIZE_MAX;
    u64 maximum_size_threshold = UINT64_MAX;
    u64 minimum_size_threshold = 0;
};

struct VisitedFile {
    dev_t device;
    ino_t inode;
};

template<>
struct AK::Traits<VisitedFile> : public DefaultTraits<VisitedFile> {
    static unsigned hash(VisitedFile const& visited_file)
    {
        return pair_int_hash(u64_hash(visited_file.device), u64_hash(visited_file.inode));
    }

    static bool equals(VisitedFile const& a, VisitedFile const& b)
    {
        return a.device == b.device && a.inode == b.inode;
    }
};

static HashTable<VisitedFile> s_visited_files;

static ErrorOr<void> parse_args(Main::Arguments arguments, Vector<ByteString>& files, DuOption& du_option);
static u64 print_space_usage(ByteString const& path, DuOption const& du_option, size_t current_depth, Optional<dev_t> root_device = {});

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<ByteString> files;
    DuOption du_option;

    TRY(parse_args(arguments, files, du_option));

    size_t total_size_count = 0;
    for (auto const& file : files)
        total_size_count += print_space_usage(file, du_option, 0);

    if (du_option.print_total_size) {
        if (du_option.human_readable) {
            outln("{}\ttotal", human_readable_size(total_size_count));
        } else if (du_option.human_readable_si) {
            outln("{}\ttotal", human_readable_size(total_size_count, AK::HumanReadableBasedOn::Base10));
        } else {
            outln("{}\ttotal", total_size_count);
        }
    }

    return 0;
}

ErrorOr<void> parse_args(Main::Arguments arguments, Vector<ByteString>& files, DuOption& du_option)
{
    bool summarize = false;
    StringView pattern;
    StringView exclude_from;
    Vector<StringView> files_to_process;

    Core::ArgsParser::Option time_option {
        Core::ArgsParser::OptionArgumentMode::Required,
        "Show time of type time-type of any file in the directory, or any of its subdirectories. "
        "Available choices: mtime, modification, ctime, status, use, atime, access",
        "time",
        0,
        "time-type",
        [&du_option](StringView option) {
            if (option == "mtime"sv || option == "modification"sv)
                du_option.time_type = DuOption::TimeType::Modification;
            else if (option == "ctime"sv || option == "status"sv || option == "use"sv)
                du_option.time_type = DuOption::TimeType::Status;
            else if (option == "atime"sv || option == "access"sv)
                du_option.time_type = DuOption::TimeType::Access;
            else
                return false;

            return true;
        }
    };

    Core::ArgsParser::Option block_size_1k_option {
        Core::ArgsParser::OptionArgumentMode::None,
        "Equivalent to `--block-size 1024`",
        nullptr,
        'k',
        nullptr,
        [&du_option](StringView) {
            du_option.block_size = 1024;
            return true;
        }
    };

    i64 threshold = 0;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display actual or apparent disk usage of files or directories.");
    args_parser.add_option(du_option.all, "Write counts for all files, not just directories", "all", 'a');
    args_parser.add_option(du_option.apparent_size, "Print apparent sizes, rather than disk usage", "apparent-size");
    args_parser.add_option(du_option.print_total_size, "Print total count in the end", "total", 'c');
    args_parser.add_option(du_option.human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_option(du_option.human_readable_si, "Print human-readable sizes in SI units", "si");
    args_parser.add_option(du_option.max_depth, "Print the total for a directory or file only if it is N or fewer levels below the command line argument", "max-depth", 'd', "N");
    args_parser.add_option(summarize, "Display only a total for each argument", "summarize", 's');
    args_parser.add_option(threshold, "Exclude entries smaller than size if positive, or entries greater than size if negative", "threshold", 't', "size");
    args_parser.add_option(move(time_option));
    args_parser.add_option(pattern, "Exclude files that match pattern", "exclude", 0, "pattern");
    args_parser.add_option(exclude_from, "Exclude files that match any pattern in file", "exclude-from", 'X', "file");
    args_parser.add_option(du_option.one_file_system, "Don't traverse directories on different file systems", "one-file-system", 'x');
    args_parser.add_option(du_option.block_size, "Outputs file sizes as the required blocks with the given size (defaults to 1024)", "block-size", 'B', "size");
    args_parser.add_option(move(block_size_1k_option));
    args_parser.add_option(du_option.maximum_size_threshold, "Exclude files with size above a specified size (defaults to uint64_t max value)", "max-size", 0, "size");
    args_parser.add_option(du_option.minimum_size_threshold, "Exclude files with size below a specified size (defaults to 0)", "min-size", 0, "size");
    args_parser.add_positional_argument(files_to_process, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (threshold > 0)
        du_option.minimum_size_threshold = static_cast<u64>(threshold);
    else if (threshold < 0)
        du_option.maximum_size_threshold = static_cast<u64>(-threshold);

    if (du_option.maximum_size_threshold < du_option.minimum_size_threshold)
        return Error::from_string_literal("Invalid minimum size exclusion is above maximum size exclusion");

    if (summarize)
        du_option.max_depth = 0;

    if (!pattern.is_empty())
        du_option.excluded_patterns.append(pattern);
    if (!exclude_from.is_empty()) {
        auto file = TRY(Core::File::open(exclude_from, Core::File::OpenMode::Read));
        auto const buff = TRY(file->read_until_eof());
        if (!buff.is_empty()) {
            ByteString patterns = ByteString::copy(buff, Chomp);
            du_option.excluded_patterns.extend(patterns.split('\n'));
        }
    }

    for (auto const& file : files_to_process) {
        files.append(file);
    }

    if (files.is_empty()) {
        files.append(".");
    }

    return {};
}

u64 print_space_usage(ByteString const& path, DuOption const& du_option, size_t current_depth, Optional<dev_t> root_device)
{
    u64 size = 0;
    auto path_stat_or_error = Core::System::lstat(path);
    if (path_stat_or_error.is_error()) {
        warnln("du: cannot stat '{}': {}", path, path_stat_or_error.release_error());
        return 0;
    }

    auto path_stat = path_stat_or_error.release_value();

    if (!root_device.has_value()) {
        root_device = path_stat.st_dev;
    }

    if (du_option.one_file_system && root_device.value() != path_stat.st_dev) {
        return 0;
    }

    VisitedFile visited_file { path_stat.st_dev, path_stat.st_ino };
    if (s_visited_files.contains(visited_file)) {
        return 0;
    }
    s_visited_files.set(visited_file);

    bool const is_directory = S_ISDIR(path_stat.st_mode);
    if (is_directory) {
        auto di = Core::DirIterator(path, Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error()) {
            auto error = di.error();
            warnln("du: cannot read directory '{}': {}", path, error);
            return 0;
        }

        while (di.has_next()) {
            auto const child_path = di.next_full_path();
            size += print_space_usage(child_path, du_option, current_depth + 1, root_device);
        }
    }

    auto const basename = LexicalPath::basename(path);
    for (auto const& pattern : du_option.excluded_patterns) {
        if (basename.matches(pattern, CaseSensitivity::CaseSensitive))
            return 0;
    }

    // If the underlying FS reports 0 used blocks, apparent size may be more accurate
    if (du_option.apparent_size || path_stat.st_blocks == 0) {
        size += path_stat.st_size;
    } else {
        constexpr auto block_size = 512;
        size += path_stat.st_blocks * block_size;
    }

    bool is_beyond_depth = current_depth > du_option.max_depth;
    bool is_inner_file = current_depth > 0 && !is_directory;
    bool is_outside_size_range = (du_option.minimum_size_threshold > size) || (du_option.maximum_size_threshold < size);

    // All of these still count towards the full size, they are just not reported on individually.
    if (is_beyond_depth || (is_inner_file && !du_option.all) || is_outside_size_range)
        return size;

    if (du_option.human_readable) {
        out("{:10s}", human_readable_size(size));
    } else if (du_option.human_readable_si) {
        out("{:10s}", human_readable_size(size, AK::HumanReadableBasedOn::Base10));
    } else {
        out("{:06d}", ceil_div(size, du_option.block_size));
    }

    if (du_option.time_type == DuOption::TimeType::NotUsed) {
        outln("\t{}", path);
    } else {
        auto time = path_stat.st_mtime;
        switch (du_option.time_type) {
        case DuOption::TimeType::Access:
            time = path_stat.st_atime;
            break;
        case DuOption::TimeType::Status:
            time = path_stat.st_ctime;
            break;
        default:
            break;
        }

        auto const formatted_time = Core::DateTime::from_timestamp(time).to_byte_string();
        outln("\t{}\t{}", formatted_time, path);
    }

    return size;
}
