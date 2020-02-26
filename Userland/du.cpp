/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <AK/FileSystemPath.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/Object.h>
#include <LibM/math.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

struct DuOption {
    enum class TimeType {
        NotUsed,
        Modification,
        Access,
        Status
    };

    bool all = false;
    bool apparent_size = false;
    int threshold = 0;
    TimeType time_type = TimeType::NotUsed;
    Vector<String> excluded_patterns;
};

static int parse_args(int argc, char** argv, Vector<String>& files, DuOption& du_option, int& max_depth);
static int print_space_usage(const String& path, const DuOption& du_option, int max_depth);

int main(int argc, char** argv)
{
    Vector<String> files;
    DuOption du_option;
    int max_depth = INT_MAX;

    if (parse_args(argc, argv, files, du_option, max_depth))
        return 1;

    for (const auto& file : files) {
        if (print_space_usage(file, du_option, max_depth))
            return 1;
    }

    return 0;
}

int parse_args(int argc, char** argv, Vector<String>& files, DuOption& du_option, int& max_depth)
{
    bool summarize = false;
    const char* pattern = nullptr;
    const char* exclude_from = nullptr;
    Vector<const char*> files_to_process;

    Core::ArgsParser::Option time_option {
        true,
        "Show time of type time-type of any file in the directory, or any of its subdirectories. "
        "Available choices: mtime, modification, ctime, status, use, atime, access",
        "time",
        0,
        "time-type",
        [&du_option](const char* s) {
            if (!strcmp(s, "mtime") || !strcmp(s, "modification"))
                du_option.time_type = DuOption::TimeType::Modification;
            else if (!strcmp(s, "ctime") || !strcmp(s, "status") || !strcmp(s, "use"))
                du_option.time_type = DuOption::TimeType::Status;
            else if (!strcmp(s, "atime") || !strcmp(s, "access"))
                du_option.time_type = DuOption::TimeType::Access;
            else
                return false;

            return true;
        }
    };

    Core::ArgsParser args_parser;
    args_parser.add_option(du_option.all, "Write counts for all files, not just directories", "all", 'a');
    args_parser.add_option(du_option.apparent_size, "Print apparent sizes, rather than disk usage", "apparent-size", 0);
    args_parser.add_option(max_depth, "Print the total for a directory or file only if it is N or fewer levels below the command line argument", "max-depth", 'd', "N");
    args_parser.add_option(summarize, "Display only a total for each argument", "summarize", 's');
    args_parser.add_option(du_option.threshold, "Exclude entries smaller than size if positive, or entries greater than size if negative", "threshold", 't', "size");
    args_parser.add_option(move(time_option));
    args_parser.add_option(pattern, "Exclude files that match pattern", "exclude", 0, "pattern");
    args_parser.add_option(exclude_from, "Exclude files that match any pattern in file", "exclude_from", 'X', "file");
    args_parser.add_positional_argument(files_to_process, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (summarize)
        max_depth = 0;

    if (pattern)
        du_option.excluded_patterns.append(pattern);
    if (exclude_from) {
        auto file = Core::File::construct(exclude_from);
        bool success = file->open(Core::IODevice::ReadOnly);
        ASSERT(success);
        if (const auto buff = file->read_all()) {
            String patterns = String::copy(buff, Chomp);
            du_option.excluded_patterns.append(patterns.split('\n'));
        }
    }

    for (auto* file : files_to_process) {
        files.append(file);
    }

    if (files.is_empty()) {
        files.append(".");
    }

    return 0;
}

int print_space_usage(const String& path, const DuOption& du_option, int max_depth)
{
    struct stat path_stat;
    if (lstat(path.characters(), &path_stat) < 0) {
        perror("lstat");
        return 1;
    }

    if (--max_depth >= 0 && S_ISDIR(path_stat.st_mode)) {
        auto di = Core::DirIterator(path, Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error()) {
            fprintf(stderr, "DirIterator: %s\n", di.error_string());
            return 1;
        }
        while (di.has_next()) {
            const auto child_path = di.next_full_path();
            if (du_option.all || Core::File::is_directory(child_path)) {
                if (print_space_usage(child_path, du_option, max_depth))
                    return 1;
            }
        }
    }

    const auto basename = FileSystemPath(path).basename();
    for (const auto& pattern : du_option.excluded_patterns) {
        if (basename.matches(pattern, CaseSensitivity::CaseSensitive))
            return 0;
    }

    long long size = path_stat.st_size;
    if (du_option.apparent_size) {
        const auto block_size = 512;
        size = path_stat.st_blocks * block_size;
    }

    if ((du_option.threshold > 0 && size < du_option.threshold) || (du_option.threshold < 0 && size > -du_option.threshold))
        return 0;

    const long long block_size = 1024;
    size = size / block_size + (size % block_size != 0);

    if (du_option.time_type == DuOption::TimeType::NotUsed)
        printf("%lld\t%s\n", size, path.characters());
    else {
        auto time = path_stat.st_mtime;
        switch (du_option.time_type) {
        case DuOption::TimeType::Access:
            time = path_stat.st_atime;
            break;
        case DuOption::TimeType::Status:
            time = path_stat.st_ctime;
        default:
            break;
        }

        const auto formatted_time = Core::DateTime::from_timestamp(time).to_string();
        printf("%lld\t%s\t%s\n", size, formatted_time.characters(), path.characters());
    }

    return 0;
}
