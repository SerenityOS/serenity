/*
htop - serenity/SerenityJSON.cpp
(C) 2026 SerenityOS contributors
Released under the GNU GPLv2+, see the COPYING file
in the source distribution for its full text.
*/

#include "serenity/SerenityJSON.h"

#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/StringView.h>

#include <fcntl.h>
#include <string.h>
#include <unistd.h>

static void safe_copy(char* dst, AK::StringView src, size_t dstsz)
{
    size_t len = src.length();
    if (len >= dstsz)
        len = dstsz - 1;
    memcpy(dst, src.characters_without_null_termination(), len);
    dst[len] = '\0';
}

extern "C" int serenity_read_system_stats(SerenitySystemStats* out)
{
    int fd = open("/sys/kernel/stats", O_RDONLY);
    if (fd < 0)
        return -1;

    char buf[4096];
    size_t total = 0;
    for (;;) {
        ssize_t n = read(fd, buf + total, sizeof(buf) - total - 1);
        if (n <= 0)
            break;
        total += static_cast<size_t>(n);
    }
    close(fd);
    if (total == 0)
        return -1;
    buf[total] = '\0';

    auto parse_result = AK::JsonValue::from_string(AK::StringView(buf, total));
    if (parse_result.is_error())
        return -1;

    auto const& obj = parse_result.value().as_object();
    out->total_time = obj.get_u64("total_time"sv).value_or(0);
    out->kernel_time = obj.get_u64("kernel_time"sv).value_or(0);
    out->user_time = obj.get_u64("user_time"sv).value_or(0);
    out->idle_time = obj.get_u64("idle_time"sv).value_or(0);
    return 0;
}

extern "C" int serenity_read_memory_status(SerenityMemoryStats* out)
{
    int fd = open("/sys/kernel/memstat", O_RDONLY);
    if (fd < 0)
        return -1;

    char buf[4096];
    size_t total = 0;
    for (;;) {
        ssize_t n = read(fd, buf + total, sizeof(buf) - total - 1);
        if (n <= 0)
            break;
        total += static_cast<size_t>(n);
    }
    close(fd);
    if (total == 0)
        return -1;
    buf[total] = '\0';

    auto parse_result = AK::JsonValue::from_string(AK::StringView(buf, total));
    if (parse_result.is_error())
        return -1;

    auto const& obj = parse_result.value().as_object();
    out->physical_allocated_pages = obj.get_u64("physical_allocated"sv).value_or(0);
    out->physical_available_pages = obj.get_u64("physical_available"sv).value_or(0);
    return 0;
}

extern "C" int serenity_parse_processes(char const* json, size_t len,
    SerenityProcCallback cb, void* userdata)
{
    auto string_view = AK::StringView(json, len);
    auto parse_result = AK::JsonParser(string_view).parse();
    if (parse_result.is_error())
        return -1;

    auto const& root = parse_result.value().as_object();
    auto processes = root.get_array("processes"sv);
    if (!processes.has_value())
        return -1;

    processes->for_each([&](auto& value) {
        auto const& proc_obj = value.as_object();
        SerenityProcEntry entry;
        memset(&entry, 0, sizeof(entry));
        entry.priority = 99;

        entry.pid = proc_obj.get_i32("pid"sv).value_or(0);
        entry.ppid = proc_obj.get_i32("ppid"sv).value_or(0);
        /* "pgp" = process group; "pgid" = TTY foreground process group */
        entry.pgid = proc_obj.get_i32("pgp"sv).value_or(0);
        entry.sid = proc_obj.get_i32("sid"sv).value_or(0);
        entry.tpgid = proc_obj.get_i32("pgid"sv).value_or(0);
        entry.uid = proc_obj.get_u32("uid"sv).value_or(0);
        entry.kernel = proc_obj.get_bool("kernel"sv).value_or(false);

        safe_copy(entry.name, proc_obj.get_byte_string("name"sv).value_or(""), sizeof(entry.name));
        safe_copy(entry.executable, proc_obj.get_byte_string("executable"sv).value_or(""), sizeof(entry.executable));
        safe_copy(entry.tty, proc_obj.get_byte_string("tty"sv).value_or(""), sizeof(entry.tty));
        safe_copy(entry.pledge, proc_obj.get_byte_string("pledge"sv).value_or(""), sizeof(entry.pledge));
        safe_copy(entry.veil, proc_obj.get_byte_string("veil"sv).value_or(""), sizeof(entry.veil));

        entry.amount_virtual = proc_obj.get_u64("amount_virtual"sv).value_or(0);
        entry.amount_resident = proc_obj.get_u64("amount_resident"sv).value_or(0);
        entry.creation_time_ns = proc_obj.get_u64("creation_time"sv).value_or(0);

        /* Aggregate thread data */
        auto threads = proc_obj.get_array("threads"sv);
        if (threads.has_value()) {
            bool found_running = false;
            threads->for_each([&](auto& tval) {
                auto const& tobj = tval.as_object();

                entry.time_user += tobj.get_u64("time_user"sv).value_or(0);
                entry.time_kernel += tobj.get_u64("time_kernel"sv).value_or(0);
                entry.inode_faults += tobj.get_u64("inode_faults"sv).value_or(0);
                entry.zero_faults += tobj.get_u64("zero_faults"sv).value_or(0);
                entry.cow_faults += tobj.get_u64("cow_faults"sv).value_or(0);
                entry.nlwp++;

                auto state = tobj.get_byte_string("state"sv).value_or("");
                /* Prefer "Running" state; otherwise keep first thread's state */
                if (!found_running) {
                    safe_copy(entry.state, state, sizeof(entry.state));
                    entry.cpu = tobj.get_u32("cpu"sv).value_or(0);
                }
                if (state == "Running") {
                    found_running = true;
                    safe_copy(entry.state, state, sizeof(entry.state));
                    entry.cpu = tobj.get_u32("cpu"sv).value_or(0);
                }
                int tprio = tobj.get_i32("priority"sv).value_or(30);
                if (tprio < entry.priority)
                    entry.priority = tprio;
            });
        }

        cb(&entry, userdata);
    });

    return 0;
}
