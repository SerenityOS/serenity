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
    TimeType time_type = TimeType::NotUsed;
    Vector<String> excluded_patterns;
};

static ErrorOr<void> parse_args(Main::Arguments arguments, Vector<String>& files, DuOption& du_option, int& max_depth);
static ErrorOr<off_t> print_space_usage(const String& path, const DuOption& du_option, int max_depth);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<String> files;
    DuOption du_option;
    int max_depth = INT_MAX;

    TRY(parse_args(arguments, files, du_option, max_depth));

    for (const auto& file : files)
        TRY(print_space_usage(file, du_option, max_depth));

    return 0;
}

ErrorOr<void> parse_args(Main::Arguments arguments, Vector<String>& files, DuOption& du_option, int& max_depth)
{
    bool summarize = false;
    const char* pattern = nullptr;
    const char* exclude_from = nullptr;
    Vector<StringView> files_to_process;

    Core::ArgsParser::Option time_option {
        true,
        "Show time of type time-type of any file in the directory, or any of its subdirectories. "
        "Available choices: mtime, modification, ctime, status, use, atime, access",
        "time",
        0,
        "time-type",
        [&du_option](StringView s) {
            if (s == "mtime"sv || s == "modification"sv)
                du_option.time_type = DuOption::TimeType::Modification;
            else if (s == "ctime"sv || s == "status"sv || s == "use"sv)
                du_option.time_type = DuOption::TimeType::Status;
            else if (s == "atime"sv || s == "access"sv)
                du_option.time_type = DuOption::TimeType::Access;
            else
                return false;

            return true;
        }
    };

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display actual or apparent disk usage of files or directories.");
    args_parser.add_option(du_option.all, "Write counts for all files, not just directories", "all", 'a');
    args_parser.add_option(du_option.apparent_size, "Print apparent sizes, rather than disk usage", "apparent-size", 0);
    args_parser.add_option(du_option.human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_option(max_depth, "Print the total for a directory or file only if it is N or fewer levels below the command line argument", "max-depth", 'd', "N");
    args_parser.add_option(summarize, "Display only a total for each argument", "summarize", 's');
    args_parser.add_option(du_option.threshold, "Exclude entries smaller than size if positive, or entries greater than size if negative", "threshold", 't', "size");
    args_parser.add_option(move(time_option));
    args_parser.add_option(pattern, "Exclude files that match pattern", "exclude", 0, "pattern");
    args_parser.add_option(exclude_from, "Exclude files that match any pattern in file", "exclude_from", 'X', "file");
    args_parser.add_positional_argument(files_to_process, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (summarize)
        max_depth = 0;

    if (pattern)
        du_option.excluded_patterns.append(pattern);
    if (exclude_from) {
        auto file = TRY(Core::File::open(exclude_from, Core::OpenMode::ReadOnly));
        const auto buff = file->read_all();
        if (!buff.is_empty()) {
            String patterns = String::copy(buff, Chomp);
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

ErrorOr<off_t> print_space_usage(const String& path, const DuOption& du_option, int max_depth)
{
    struct stat path_stat = TRY(Core::System::lstat(path.characters()));
    off_t directory_size = 0;
    const bool is_directory = S_ISDIR(path_stat.st_mode);
    if (--max_depth >= 0 && is_directory) {
        auto di = Core::DirIterator(path, Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error()) {
            outln("du: cannot read directory '{}': {}", path, di.error_string());
            return Error::from_string_literal("An error occurred. See previous error."sv);
        }

        while (di.has_next()) {
            const auto child_path = di.next_full_path();
            directory_size += TRY(print_space_usage(child_path, du_option, max_depth));
        }
    }

    const auto basename = LexicalPath::basename(path);
    for (const auto& pattern : du_option.excluded_patterns) {
        if (basename.matches(pattern, CaseSensitivity::CaseSensitive))
            return { 0 };
    }

    off_t size = path_stat.st_size;
    if (du_option.apparent_size) {
        constexpr auto block_size = 512;
        size = path_stat.st_blocks * block_size;
    }

    if (!du_option.all && !is_directory)
        return size;

    if (is_directory)
        size = directory_size;

    if ((du_option.threshold > 0 && size < du_option.threshold) || (du_option.threshold < 0 && size > -du_option.threshold))
        return { 0 };

    if (du_option.human_readable) {
        out("{}", human_readable_size(size));
    } else {
        constexpr long long block_size = 1024;
        size = size / block_size + (size % block_size != 0);
        out("{}", size);
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

        const auto formatted_time = Core::DateTime::from_timestamp(time).to_string();
        outln("\t{}\t{}", formatted_time, path);
    }

    return { size };
}
