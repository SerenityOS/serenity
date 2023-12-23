/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/sysmacros.h>
#include <unistd.h>

#define ENUMERATE_COLUMN_DESCRIPTIONS                         \
    COLUMN(UserId, "uid", "UID", Alignment::Left)             \
    COLUMN(ProcessId, "pid", "PID", Alignment::Right)         \
    COLUMN(ParentProcessId, "ppid", "PPID", Alignment::Right) \
    COLUMN(ProcessGroupId, "pgid", "PGID", Alignment::Right)  \
    COLUMN(SessionId, "sid", "SID", Alignment::Right)         \
    COLUMN(State, "state", "STATE", Alignment::Left)          \
    COLUMN(StartTime, "stime", "STIME", Alignment::Left)      \
    COLUMN(TTY, "tty", "TTY", Alignment::Left)                \
    COLUMN(Command, "cmd", "CMD", Alignment::Left)

enum class ColumnId {
#define COLUMN(column_id, lookup_name, default_title, alignment) column_id,
    ENUMERATE_COLUMN_DESCRIPTIONS
#undef COLUMN
        __Count
};

enum class Alignment {
    Left,
    Right,
};

struct ColumnDescription {
    StringView lookup_name;
    StringView default_title;
    Alignment alignment;
};

struct Column {
    ColumnId id;
    String title;
    Alignment alignment;
    int width { 0 };
    String buffer {};
};

static Optional<ColumnId> column_name_to_id(StringView column_name)
{
#define COLUMN(column_id, lookup_name, default_title, alignment) \
    if (column_name == lookup_name)                              \
        return ColumnId::column_id;
    ENUMERATE_COLUMN_DESCRIPTIONS
#undef COLUMN

    return {};
}

static ErrorOr<Column> column_from_id(ColumnId column_id, Optional<String> const& custom_title = {})
{
    constexpr Array<ColumnDescription, static_cast<size_t>(ColumnId::__Count)> available_columns { {
#define COLUMN(column_id, lookup_name, default_title, alignment) { lookup_name##sv, default_title##sv, alignment },
        ENUMERATE_COLUMN_DESCRIPTIONS
#undef COLUMN
    } };

    auto const& column_description = available_columns[static_cast<size_t>(column_id)];
    auto title = custom_title.has_value()
        ? custom_title.value()
        : TRY(String::from_utf8(column_description.default_title));

    return Column { column_id, title, column_description.alignment };
}

static ErrorOr<String> column_to_string(ColumnId column_id, Core::ProcessStatistics process)
{
    switch (column_id) {
    case ColumnId::UserId:
        return String::from_byte_string(process.username);
    case ColumnId::ProcessId:
        return String::number(process.pid);
    case ColumnId::ParentProcessId:
        return String::number(process.ppid);
    case ColumnId::ProcessGroupId:
        return String::number(process.pgid);
    case ColumnId::SessionId:
        return String::number(process.sid);
    case ColumnId::TTY:
        return process.tty == "" ? "n/a"_string : String::from_byte_string(process.tty);
    case ColumnId::State:
        return process.threads.is_empty()
            ? "Zombie"_string
            : String::from_byte_string(process.threads.first().state);
    case ColumnId::StartTime: {
        auto now = Core::DateTime::now();
        auto today_start = Core::DateTime::now();
        today_start.set_time(now.year(), now.month(), now.day(), 0, 0, 0);
        auto process_creation_time = Core::DateTime::from_timestamp(process.creation_time.seconds_since_epoch());
        if (today_start < process_creation_time || today_start == process_creation_time)
            return process_creation_time.to_string("%H:%M"sv);
        return process_creation_time.to_string("%b%d"sv);
    }
    case ColumnId::Command:
        return String::from_byte_string(process.name);
    default:
        VERIFY_NOT_REACHED();
    }
}

static ErrorOr<Column> parse_column_format_specifier(StringView column_format_specifier)
{
    auto column_specification_parts = column_format_specifier.split_view('=', SplitBehavior::KeepEmpty);
    if (column_specification_parts.size() > 2)
        return Error::from_string_literal("Invalid column specifier format");

    auto column_name = column_specification_parts[0];
    auto maybe_column_id = column_name_to_id(column_name);
    if (!maybe_column_id.has_value())
        return Error::from_string_literal("Unknown column");

    Optional<String> column_title;
    if (column_specification_parts.size() == 2)
        column_title = TRY(String::from_utf8(column_specification_parts[1]));

    return column_from_id(maybe_column_id.value(), column_title);
}

static ErrorOr<Optional<String>> tty_stat_to_pseudo_name(struct stat tty_stat)
{
    int tty_device_major = major(tty_stat.st_rdev);
    int tty_device_minor = minor(tty_stat.st_rdev);

    if (tty_device_major == 201)
        return String::formatted("pts:{}", tty_device_minor);

    if (tty_device_major == 4)
        return String::formatted("tty:{}", tty_device_minor);

    return OptionalNone {};
}

static ErrorOr<String> determine_tty_pseudo_name()
{
    auto tty_stat = TRY(Core::System::fstat(STDIN_FILENO));
    auto maybe_tty_pseudo_name = TRY(tty_stat_to_pseudo_name(tty_stat));
    return maybe_tty_pseudo_name.value_or("n/a"_string);
}

static ErrorOr<String> parse_tty_pseudo_name(StringView tty_name)
{
    auto tty_name_parts = tty_name.split_view(":"sv, AK::SplitBehavior::KeepEmpty);
    String tty_full_name;
    StringView tty_full_name_view;
    if (tty_name_parts.size() == 1) {
        tty_full_name_view = tty_name;
    } else {
        if (tty_name_parts.size() != 2)
            return Error::from_errno(ENOTTY);

        auto tty_device_type = tty_name_parts[0];
        auto tty_number = tty_name_parts[1];
        if (tty_device_type == "tty"sv)
            tty_full_name = TRY(String::formatted("/dev/tty{}", tty_number));
        else if (tty_device_type == "pts"sv)
            tty_full_name = TRY(String::formatted("/dev/pts/{}", tty_number));
        else
            return Error::from_errno(ENOTTY);

        tty_full_name_view = tty_full_name.bytes_as_string_view();
    }

    auto tty_stat = TRY(Core::System::stat(tty_full_name_view));
    auto maybe_tty_pseudo_name = TRY(tty_stat_to_pseudo_name(tty_stat));
    if (!maybe_tty_pseudo_name.has_value())
        return Error::from_errno(ENOTTY);

    return maybe_tty_pseudo_name.release_value();
}

template<typename Value, typename ParseValue>
Core::ArgsParser::Option make_list_option(Vector<Value>& value_list, char const* help_string, char const* long_name, char short_name, char const* value_name, ParseValue parse_value)
{
    return Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = help_string,
        .long_name = long_name,
        .short_name = short_name,
        .value_name = value_name,
        .accept_value = [&](StringView s) {
            auto parts = s.split_view_if([](char c) { return c == ',' || c == ' '; });
            for (auto const& part : parts) {
                auto value = parse_value(part);
                if (!value.has_value())
                    return false;
                value_list.append(value.value());
            }
            return true;
        },
    };
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty"));

    auto this_pseudo_tty_name = TRY(determine_tty_pseudo_name());

    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil("/dev/", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool every_process_flag = false;
    bool every_terminal_process_flag = false;
    bool full_format_flag = false;
    bool provided_filtering_option = false;
    bool provided_quick_pid_list = false;
    Vector<Column> columns;
    Vector<pid_t> pid_list;
    Vector<pid_t> parent_pid_list;
    Vector<ByteString> tty_list;
    Vector<uid_t> uid_list;

    Core::ArgsParser args_parser;
    args_parser.add_option(every_terminal_process_flag, "Show every process associated with terminals", nullptr, 'a');
    args_parser.add_option(every_process_flag, "Show every process", nullptr, 'A');
    args_parser.add_option(every_process_flag, "Show every process (Equivalent to -A)", nullptr, 'e');
    args_parser.add_option(full_format_flag, "Full format", nullptr, 'f');
    args_parser.add_option(make_list_option(columns, "Specify a user-defined format.", nullptr, 'o', "column-format", [&](StringView column_format_specifier) -> Optional<Column> {
        auto column_or_error = parse_column_format_specifier(column_format_specifier);
        if (column_or_error.is_error()) {
            warnln("Could not parse '{}' as a column format specifier", column_format_specifier);
            return {};
        }
        return column_or_error.release_value();
    }));
    args_parser.add_option(make_list_option(pid_list, "Show processes with a matching PID. (Comma- or space-separated list)", nullptr, 'p', "pid-list", [&](StringView pid_string) {
        provided_filtering_option = true;
        auto pid = pid_string.to_number<int>();
        if (!pid.has_value())
            warnln("Could not parse '{}' as a PID.", pid_string);
        return pid;
    }));
    args_parser.add_option(make_list_option(parent_pid_list, "Show processes with a matching PPID. (Comma- or space-separated list.)", "ppid", {}, "pid-list", [&](StringView pid_string) {
        provided_filtering_option = true;
        auto pid = pid_string.to_number<int>();
        if (!pid.has_value())
            warnln("Could not parse '{}' as a PID.", pid_string);
        return pid;
    }));
    args_parser.add_option(make_list_option(pid_list, "Show processes with a matching PID. (Comma- or space-separated list.) Processes will be listed in the order given.", nullptr, 'q', "pid-list", [&](StringView pid_string) {
        provided_quick_pid_list = true;
        auto pid = pid_string.to_number<int>();
        if (!pid.has_value())
            warnln("Could not parse '{}' as a PID.", pid_string);
        return pid;
    }));
    args_parser.add_option(make_list_option(tty_list, "Show processes associated with the given terminal. (Comma- or space-separated list.) The short TTY name or the full device path may be used.", "tty", 't', "tty-list", [&](StringView tty_string) -> Optional<ByteString> {
        provided_filtering_option = true;
        auto tty_pseudo_name_or_error = parse_tty_pseudo_name(tty_string);
        if (tty_pseudo_name_or_error.is_error()) {
            warnln("Could not parse '{}' as a TTY", tty_string);
            return {};
        }
        return tty_pseudo_name_or_error.release_value().to_byte_string();
    }));
    args_parser.add_option(make_list_option(uid_list, "Show processes with a matching user ID or login name. (Comma- or space-separated list.)", nullptr, 'u', "user-list", [&](StringView user_string) -> Optional<uid_t> {
        provided_filtering_option = true;
        if (auto uid = user_string.to_number<uid_t>(); uid.has_value()) {
            return uid.value();
        }

        auto maybe_account = Core::Account::from_name(user_string, Core::Account::Read::PasswdOnly);
        if (maybe_account.is_error()) {
            warnln("Could not find user '{}': {}", user_string, maybe_account.error());
            return {};
        }
        return maybe_account.value().uid();
    }));
    args_parser.parse(arguments);

    if (provided_filtering_option && provided_quick_pid_list) {
        warnln("The -q option cannot be combined with other filtering options.");
        return 1;
    }

    if (columns.is_empty()) {
        auto add_default_column = [&](ColumnId column_id) -> ErrorOr<void> {
            auto column = TRY(column_from_id(column_id));
            columns.unchecked_append(move(column));
            return {};
        };

        if (full_format_flag) {
            TRY(columns.try_ensure_capacity(9));
            TRY(add_default_column(ColumnId::UserId));
            TRY(add_default_column(ColumnId::ProcessId));
            TRY(add_default_column(ColumnId::ParentProcessId));
            TRY(add_default_column(ColumnId::ProcessGroupId));
            TRY(add_default_column(ColumnId::SessionId));
            TRY(add_default_column(ColumnId::State));
            TRY(add_default_column(ColumnId::StartTime));
            TRY(add_default_column(ColumnId::TTY));
            TRY(add_default_column(ColumnId::Command));
        } else {
            TRY(columns.try_ensure_capacity(3));
            TRY(add_default_column(ColumnId::ProcessId));
            TRY(add_default_column(ColumnId::TTY));
            TRY(add_default_column(ColumnId::Command));
        }
    }

    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    auto& processes = all_processes.processes;

    // Filter
    Vector<Core::ProcessStatistics> filtered_processes;
    if (provided_quick_pid_list) {
        for (auto pid : pid_list) {
            auto maybe_process = processes.first_matching([=](auto const& process) { return process.pid == pid; });
            if (maybe_process.has_value())
                filtered_processes.append(maybe_process.release_value());
        }

        processes = move(filtered_processes);
    } else if (!every_process_flag) {
        for (auto const& process : processes) {
            // Default is to show processes from the current TTY
            if ((!provided_filtering_option && process.tty == this_pseudo_tty_name.bytes_as_string_view())
                || (!pid_list.is_empty() && pid_list.contains_slow(process.pid))
                || (!parent_pid_list.is_empty() && parent_pid_list.contains_slow(process.ppid))
                || (!uid_list.is_empty() && uid_list.contains_slow(process.uid))
                || (!tty_list.is_empty() && tty_list.contains_slow(process.tty))
                || (every_terminal_process_flag && !process.tty.is_empty())) {
                filtered_processes.append(process);
            }
        }

        processes = move(filtered_processes);
    }

    // Sort
    if (!provided_quick_pid_list)
        quick_sort(processes, [](auto& a, auto& b) { return a.pid < b.pid; });

    Vector<Vector<String>> rows;
    TRY(rows.try_ensure_capacity(1 + processes.size()));

    Vector<String> header;
    TRY(header.try_ensure_capacity(columns.size()));
    auto header_is_empty = true;
    for (auto& column : columns) {
        if (!column.title.is_empty())
            header_is_empty = false;
        header.unchecked_append(column.title);
    }

    if (!header_is_empty)
        rows.unchecked_append(move(header));

    for (auto const& process : processes) {
        Vector<String> row;
        TRY(row.try_ensure_capacity(columns.size()));
        for (auto const& column : columns)
            row.unchecked_append(TRY(column_to_string(column.id, process)));

        rows.unchecked_append(move(row));
    }

    for (size_t i = 0; i < columns.size(); i++) {
        auto& column = columns[i];
        for (auto& row : rows)
            column.width = max(column.width, static_cast<int>(row[i].code_points().length()));
    }

    for (auto& row : rows) {
        for (size_t i = 0; i < columns.size(); i++) {
            auto& column = columns[i];
            auto& cell_text = row[i];
            if (!column.width) {
                out("{}", cell_text);
                continue;
            }
            if (column.alignment == Alignment::Right)
                out("{1:>{0}} ", column.width, cell_text);
            else
                out("{1:{0}} ", column.width, cell_text);
            if (i != columns.size() - 1)
                out(" ");
        }
        outln();
    }

    return 0;
}
