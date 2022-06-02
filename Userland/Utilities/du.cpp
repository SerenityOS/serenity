/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <limits.h>
#include <string.h>
#include <sys/stat.h>

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
    int threshold = 0;
    unsigned max_depth = UINT_MAX;
    TimeType time_type = TimeType::NotUsed;
    Vector<String> excluded_patterns;
    Vector<StringView> files;
};

static DuOption s_option;

static ErrorOr<void> parse_args(Main::Arguments const& arguments);
static ErrorOr<off_t> print_space_usage(String const& path, unsigned depth);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(parse_args(arguments));

    for (auto const& file : s_option.files)
        TRY(print_space_usage(file, 0));

    return 0;
}

ErrorOr<void> parse_args(Main::Arguments const& arguments)
{
    bool summarize = false;
    char const* exclude_pattern = nullptr;
    char const* exclude_from = nullptr;

    Core::ArgsParser::Option time_option {
        true,
        "Show time of type time-type of any file in the directory, or any of its subdirectories. "
        "Available choices: mtime, modification, ctime, status, use, atime, access",
        "time",
        0,
        "time-type",
        [](StringView s) {
            if (s == "mtime"sv || s == "modification"sv)
                s_option.time_type = DuOption::TimeType::Modification;
            else if (s == "ctime"sv || s == "status"sv || s == "use"sv)
                s_option.time_type = DuOption::TimeType::Status;
            else if (s == "atime"sv || s == "access"sv)
                s_option.time_type = DuOption::TimeType::Access;
            else
                return false;

            return true;
        }
    };

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display actual or apparent disk usage of files or directories.");
    args_parser.add_option(s_option.all, "Write counts for all files, not just directories", "all", 'a');
    args_parser.add_option(s_option.apparent_size, "Print apparent sizes, rather than disk usage", "apparent-size", 0);
    args_parser.add_option(s_option.human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_option(s_option.max_depth, "Print the total for a directory or file only if it is N or fewer levels below the command line argument", "max-depth", 'd', "N");
    args_parser.add_option(summarize, "Display only a total for each argument", "summarize", 's');
    args_parser.add_option(s_option.threshold, "Exclude entries smaller than size if positive, or entries greater than size if negative", "threshold", 't', "size");
    args_parser.add_option(move(time_option));
    args_parser.add_option(exclude_pattern, "Exclude files that match pattern", "exclude", 0, "pattern");
    args_parser.add_option(exclude_from, "Exclude files that match any pattern in file", "exclude_from", 'X', "file");
    args_parser.add_positional_argument(s_option.files, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (summarize)
        s_option.max_depth = 0;
    if (exclude_pattern)
        s_option.excluded_patterns.append(exclude_pattern);
    if (exclude_from) {
        auto file = TRY(Core::File::open(exclude_from, Core::OpenMode::ReadOnly));
        auto buff = file->read_all();
        if (!buff.is_empty()) {
            auto patterns = String::copy(buff, Chomp);
            s_option.excluded_patterns.extend(patterns.split('\n'));
        }
    }
    if (s_option.files.is_empty())
        s_option.files.append(".");

    return {};
}

ErrorOr<off_t> print_space_usage(String const& path, unsigned depth)
{
    auto path_stat = TRY(Core::System::lstat(path.characters()));
    bool is_directory = S_ISDIR(path_stat.st_mode);
    off_t size = 0;
    if (is_directory) {
        Core::DirIterator di { path, Core::DirIterator::SkipParentAndBaseDir };
        if (di.has_error()) {
            outln("du: cannot read directory '{}': {}", path, di.error_string());
            return Error::from_string_literal("An error occurred. See previous error."sv);
        }
        while (di.has_next()) {
            auto child_path = di.next_full_path();
            size += TRY(print_space_usage(child_path, depth + 1));
        }
    } else {
        if (s_option.apparent_size)
            size = path_stat.st_blocks * 512;
        else
            size = path_stat.st_size;
    }

    for (auto const& pattern : s_option.excluded_patterns) {
        if (path.matches(pattern, CaseSensitivity::CaseSensitive))
            return 0;
    }

    if ((s_option.threshold > 0 && size < s_option.threshold) || (s_option.threshold < 0 && size > -s_option.threshold))
        return { 0 };

    if (s_option.human_readable) {
        out("{}", human_readable_size(size));
    } else {
        constexpr long long block_size = 1024;
        size = size / block_size + (size % block_size != 0);
        out("{}", size);
    }

    if (s_option.time_type == DuOption::TimeType::NotUsed) {
        outln("\t{}", path);
    } else {
        auto time = path_stat.st_mtime;
        switch (s_option.time_type) {
        case DuOption::TimeType::Access:
            time = path_stat.st_atime;
            break;
        case DuOption::TimeType::Status:
            time = path_stat.st_ctime;
            break;
        default:
            break;
        }

        auto const formatted_time = Core::DateTime::from_timestamp(time).to_string();
        outln("\t{}\t{}", formatted_time, path);
    }

    return { size };
}
