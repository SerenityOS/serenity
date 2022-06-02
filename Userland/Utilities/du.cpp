/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumberFormat.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/param.h>

struct DuOption {
    enum class TimeType {
        NotUsed,
        Modification,
        Access,
        Change
    };

    bool human_readable = false;
    bool all = false;
    bool apparent_size = false;
    bool total = false;
    int threshold = 0;
    int block_size = 512;
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

    off_t total = 0;
    for (auto const& file : s_option.files)
        total += TRY(print_space_usage(file, 0));

    if (s_option.total) {
        if (s_option.human_readable)
            outln("{}\ttotal", human_readable_size(total));
        else
            outln("{}\ttotal", howmany(total, s_option.block_size));
    }
    return 0;
}

ErrorOr<void> parse_args(Main::Arguments const& arguments)
{
    bool summarize = false;
    char const* exclude_pattern = nullptr;
    char const* exclude_from = nullptr;

    Core::ArgsParser::Option time_option {
        true,
        "Show timestamp of type time-type for each entry. Available choices are: mtime, modification (modification timestamp), ctime, status, use (change timestamp) and atime, access (access timestamp)",
        "time",
        0,
        "time-type",
        [](StringView s) {
            if (s.is_one_of("mtime"sv, "modification"sv))
                s_option.time_type = DuOption::TimeType::Modification;
            else if (s.is_one_of("ctime"sv, "status"sv, "use"sv))
                s_option.time_type = DuOption::TimeType::Change;
            else if (s.is_one_of("atime"sv, "access"sv))
                s_option.time_type = DuOption::TimeType::Access;
            else
                return false;

            return true;
        }
    };

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display actual or apparent disk usage of files or directories.");
    args_parser.add_option(s_option.all, "Print the sizes of both files and directories", "all", 'a');
    args_parser.add_option(s_option.apparent_size, "Print apparent sizes, rather than disk usage", "apparent-size", 0);
    args_parser.add_option(s_option.block_size, "Print the sizes in a unit of size bytes", "block-size", 'B', "size");
    args_parser.add_option(s_option.total, "Print the total size of all arguments", "total", 'c');
    args_parser.add_option(s_option.max_depth, "Print the size of an entry only if it is N or fewer levels below the root of the file hierarchy", "max-depth", 'd', "N");
    args_parser.add_option(exclude_pattern, "Exclude entries that match pattern", "exclude", 0, "pattern");
    args_parser.add_option(s_option.human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_option(summarize, "Print only the size of each argument", "summarize", 's');
    args_parser.add_option(s_option.threshold, "Exclude entries smaller than size if positive, or greater than size if negative", "threshold", 't', "size");
    args_parser.add_option(move(time_option));
    args_parser.add_option(exclude_from, "Exclude entries that match any pattern in file", "exclude-from", 'X', "file");
    args_parser.add_positional_argument(s_option.files, "Directories or files to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (s_option.all + summarize + (s_option.max_depth != UINT_MAX) > 1)
        return Error::from_string_literal("Only one of -a, -s or -d can be specified"sv);
    if (s_option.block_size <= 0)
        return Error::from_string_literal("Block size must be greater than zero"sv);

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
        size = path_stat.st_size; // According to POSIX, we include the size of the directory itself.
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
            size = path_stat.st_size;
        else
            size = path_stat.st_blocks * 512;
    }

    for (auto const& pattern : s_option.excluded_patterns) {
        if (path.matches(pattern, CaseSensitivity::CaseSensitive))
            return 0;
    }

    bool in_threshold = s_option.threshold == 0 || ((s_option.threshold > 0 && size < s_option.threshold) || (s_option.threshold < 0 && size > -s_option.threshold));
    if ((is_directory || (s_option.all || depth == 0)) && depth <= s_option.max_depth && in_threshold) {
        if (s_option.human_readable)
            out("{}", human_readable_size(size));
        else
            out("{}", howmany(size, s_option.block_size));

        if (s_option.time_type == DuOption::TimeType::NotUsed) {
            outln("\t{}", path);
        } else {
            time_t time = 0;
            switch (s_option.time_type) {
            case DuOption::TimeType::Access:
                time = path_stat.st_atime;
                break;
            case DuOption::TimeType::Change:
                time = path_stat.st_ctime;
                break;
            case DuOption::TimeType::Modification:
                time = path_stat.st_mtime;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
            auto formatted_time = Core::DateTime::from_timestamp(time).to_string();
            outln("\t{}\t{}", formatted_time, path);
        }
    }
    return size;
}
