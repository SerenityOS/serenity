/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Stream.h>
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
    bool all = false;
    bool apparent_size = false;
    i64 threshold = 0;
    TimeType time_type = TimeType::NotUsed;
    Vector<DeprecatedString> excluded_patterns;
    u64 block_size = 1024;
    size_t max_depth = SIZE_MAX;
};

static ErrorOr<void> parse_args(Main::Arguments arguments, Vector<DeprecatedString>& files, DuOption& du_option);
static ErrorOr<u64> print_space_usage(DeprecatedString const& path, DuOption const& du_option, size_t current_depth, bool inside_dir = false);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<DeprecatedString> files;
    DuOption du_option;

    TRY(parse_args(arguments, files, du_option));

    for (auto const& file : files)
        TRY(print_space_usage(file, du_option, 0));

    return 0;
}

ErrorOr<void> parse_args(Main::Arguments arguments, Vector<DeprecatedString>& files, DuOption& du_option)
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
        [&du_option](auto const* option_ptr) {
            StringView option { option_ptr, strlen(option_ptr) };
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
        [&du_option](auto const*) {
            du_option.block_size = 1024;
            return true;
        }
    };

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display actual or apparent disk usage of files or directories.");
    args_parser.add_option(du_option.all, "Write counts for all files, not just directories", "all", 'a');
    args_parser.add_option(du_option.apparent_size, "Print apparent sizes, rather than disk usage", "apparent-size", 0);
    args_parser.add_option(du_option.human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_option(du_option.max_depth, "Print the total for a directory or file only if it is N or fewer levels below the command line argument", "max-depth", 'd', "N");
    args_parser.add_option(summarize, "Display only a total for each argument", "summarize", 's');
    args_parser.add_option(du_option.threshold, "Exclude entries smaller than size if positive, or entries greater than size if negative", "threshold", 't', "size");
    args_parser.add_option(move(time_option));
    args_parser.add_option(pattern, "Exclude files that match pattern", "exclude", 0, "pattern");
    args_parser.add_option(exclude_from, "Exclude files that match any pattern in file", "exclude-from", 'X', "file");
    args_parser.add_option(du_option.block_size, "Outputs file sizes as the required blocks with the given size (defaults to 1024)", "block-size", 'B', "size");
    args_parser.add_option(move(block_size_1k_option));
    args_parser.add_positional_argument(files_to_process, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (summarize)
        du_option.max_depth = 0;

    if (!pattern.is_empty())
        du_option.excluded_patterns.append(pattern);
    if (!exclude_from.is_empty()) {
        auto file = TRY(Core::Stream::File::open(exclude_from, Core::Stream::OpenMode::Read));
        auto const buff = TRY(file->read_all());
        if (!buff.is_empty()) {
            DeprecatedString patterns = DeprecatedString::copy(buff, Chomp);
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

ErrorOr<u64> print_space_usage(DeprecatedString const& path, DuOption const& du_option, size_t current_depth, bool inside_dir)
{
    u64 size = 0;
    struct stat path_stat = TRY(Core::System::lstat(path));
    bool const is_directory = S_ISDIR(path_stat.st_mode);
    if (is_directory) {
        auto di = Core::DirIterator(path, Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error()) {
            outln("du: cannot read directory '{}': {}", path, di.error_string());
            return Error::from_string_literal("An error occurred. See previous error.");
        }

        while (di.has_next()) {
            auto const child_path = di.next_full_path();
            size += TRY(print_space_usage(child_path, du_option, current_depth + 1, true));
        }
    }

    auto const basename = LexicalPath::basename(path);
    for (auto const& pattern : du_option.excluded_patterns) {
        if (basename.matches(pattern, CaseSensitivity::CaseSensitive))
            return { 0 };
    }

    if (!du_option.apparent_size) {
        constexpr auto block_size = 512;
        size += path_stat.st_blocks * block_size;
    } else {
        size += path_stat.st_size;
    }

    bool is_beyond_depth = current_depth > du_option.max_depth;
    bool is_inner_file = inside_dir && !is_directory;
    bool is_outside_threshold = (du_option.threshold > 0 && size < static_cast<u64>(du_option.threshold)) || (du_option.threshold < 0 && size > static_cast<u64>(-du_option.threshold));

    // All of these still count towards the full size, they are just not reported on individually.
    if (is_beyond_depth || (is_inner_file && !du_option.all) || is_outside_threshold)
        return size;

    if (du_option.human_readable) {
        out("{}", human_readable_size(size));
    } else {
        out("{}", ceil_div(size, du_option.block_size));
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

        auto const formatted_time = Core::DateTime::from_timestamp(time).to_deprecated_string();
        outln("\t{}\t{}", formatted_time, path);
    }

    return { size };
}
