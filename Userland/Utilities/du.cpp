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
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/Object.h>
#include <limits.h>
#include <math.h>
#include <numeric>
#include <optional>
#include <ratio>
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

    enum class DisplayFormat {
        Block,
        Human
    };

    bool all = false;
    bool apparent_size = false;
    int threshold = 0;
    TimeType time_type = TimeType::NotUsed;
    Vector<String> excluded_patterns;
    int max_depth { std::numeric_limits<int>::max() };
    DisplayFormat display_format { DisplayFormat::Block };
};

static int parse_args(int argc, char** argv, Vector<String>& files, DuOption& du_option);
static std::optional<int> get_space_usage(const String& path, const DuOption& du_option, int max_depth);
void print_space(const String& path, const DuOption& du_option, long long size, struct stat& path_stat);

static constexpr long long s_block_size = 1024;

int main(int argc, char** argv)
{
    Vector<String> files;
    DuOption du_option;

    if (parse_args(argc, argv, files, du_option))
        return 1;

    auto max_depth = du_option.max_depth;
    for (const auto& file : files) {
        if (!get_space_usage(file, du_option, max_depth))
            return 1;
    }

    return 0;
}

int parse_args(int argc, char** argv, Vector<String>& files, DuOption& du_option)
{
    bool summarize = false;
    const char* pattern = nullptr;
    const char* exclude_from = nullptr;
    Vector<const char*> files_to_process;
    bool display_human_format { false };

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
    args_parser.set_general_help("Display actual or apparent disk usage of files or directories.");
    args_parser.add_option(du_option.all, "Write counts for all files, not just directories", "all", 'a');
    args_parser.add_option(du_option.apparent_size, "Print apparent sizes, rather than disk usage", "apparent-size", 0);
    args_parser.add_option(du_option.max_depth, "Print the total for a directory or file only if it is N or fewer levels below the command line argument", "max-depth", 'd', "N");
    args_parser.add_option(summarize, "Display only a total for each argument", "summarize", 's');
    args_parser.add_option(du_option.threshold, "Exclude entries smaller than size if positive, or entries greater than size if negative", "threshold", 't', "size");
    args_parser.add_option(move(time_option));
    args_parser.add_option(pattern, "Exclude files that match pattern", "exclude", 0, "pattern");
    args_parser.add_option(exclude_from, "Exclude files that match any pattern in file", "exclude_from", 'X', "file");
    args_parser.add_option(display_human_format, "Print in human readable format", "human_readable", 'h');
    args_parser.add_positional_argument(files_to_process, "File to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (summarize)
        du_option.max_depth = 0;

    if (display_human_format)
        du_option.display_format = DuOption::DisplayFormat::Human;

    if (pattern)
        du_option.excluded_patterns.append(pattern);
    if (exclude_from) {
        auto file = Core::File::construct(exclude_from);
        bool success = file->open(Core::IODevice::ReadOnly);
        VERIFY(success);
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

std::optional<int> get_space_usage(const String& path, const DuOption& du_option, int max_depth)
{
    struct stat path_stat;
    if (lstat(path.characters(), &path_stat) < 0) {
        perror("lstat");
        return {};
    }

    auto get_size_rounded_to_block_size = [](long long size) {
        return (size / s_block_size) * s_block_size + (size % s_block_size != 0) * s_block_size;
    };

    const auto basename = LexicalPath(path).basename();
    for (const auto& pattern : du_option.excluded_patterns) {
        if (basename.matches(pattern, CaseSensitivity::CaseSensitive))
            return 0;
    }

    int total_size { 0 };
    if (--max_depth >= 0 && S_ISDIR(path_stat.st_mode)) {

        auto di = Core::DirIterator(path, Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error()) {
            fprintf(stderr, "DirIterator: %s\n", di.error_string());
            return {};
        }
        while (di.has_next()) {
            const auto child_path = di.next_full_path();
            struct stat child_stat;
            if (lstat(child_path.characters(), &child_stat)) {
                perror("lstat");
                return {};
            }

            auto size = get_space_usage(child_path, du_option, max_depth);
            if (!size)
                return {};
            total_size += get_size_rounded_to_block_size(size.value());

            if (du_option.all || Core::File::is_directory(child_path)) {
                print_space(child_path, du_option, size.value(), child_stat);
            }
        }
    }

    long long size = get_size_rounded_to_block_size(path_stat.st_size) + total_size;
    if (max_depth + 1 == du_option.max_depth) {
        print_space(path, du_option, size, path_stat);
    }

    return size;
}

void print_space(const String& path, const DuOption& du_option, long long size, struct stat& path_stat)
{
    if (du_option.apparent_size) {
        const auto block_size = 512;
        size = path_stat.st_blocks * block_size;
    }

    if ((du_option.threshold > 0 && size < du_option.threshold) || (du_option.threshold < 0 && size > -du_option.threshold))
        return;

    static constexpr long long block_size = 1024;
    auto convert_to_display_format = [](long long& size, String& size_desc, DuOption::DisplayFormat format, long long block_size) {
        using KiloByte = std::ratio<1, 1024>;
        using MegaByte = std::ratio<1, 1024 * 1024>;
        using GigaByte = std::ratio<1, 1024 * 1024 * 1024>;
        if (format == DuOption::DisplayFormat::Human) {
            if (size > GigaByte::den) {
                size = (double)size / GigaByte::den;
                size_desc = "GB";
            } else if (size > MegaByte::den) {
                size = (double)size / MegaByte::den;
                size_desc = "MB";
            } else if (size > KiloByte::den) {
                size = (double)size / KiloByte::den;
                size_desc = "KB";
            } else {
                // min block
                size = 1;
                size_desc = "KB";
            }
        } else if (format == DuOption::DisplayFormat::Block) {
            size = size / block_size + (size % block_size != 0);
        }
    };

    String size_desc_str {};
    long long size_to_print { size };
    convert_to_display_format(size_to_print, size_desc_str, du_option.display_format, block_size);

    if (du_option.time_type == DuOption::TimeType::NotUsed)
        printf("%lld %s\t%s\n", size_to_print, size_desc_str.characters(), path.characters());
    else {
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
        printf("%lld %s\t%s\t%s\n", size_to_print, size_desc_str.characters(), formatted_time.characters(), path.characters());
    }
}
