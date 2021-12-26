#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CFile.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <pwd.h>
#include <stdio.h>

HashMap<uid_t, String> CProcessStatisticsReader::s_usernames;

HashMap<pid_t, CProcessStatistics> CProcessStatisticsReader::get_all()
{
    auto file = CFile::construct("/proc/all");
    if (!file->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "CProcessStatisticsReader: Failed to open /proc/all: %s\n", file->error_string());
        return {};
    }

    HashMap<pid_t, CProcessStatistics> map;

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string({ file_contents.data(), file_contents.size() });
    json.as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        CProcessStatistics process;

        // kernel data first
        process.pid = process_object.get("pid").to_u32();
        process.times_scheduled = process_object.get("times_scheduled").to_u32();
        process.pgid = process_object.get("pgid").to_u32();
        process.pgp = process_object.get("pgp").to_u32();
        process.sid = process_object.get("sid").to_u32();
        process.uid = process_object.get("uid").to_u32();
        process.gid = process_object.get("gid").to_u32();
        process.state = process_object.get("state").to_string();
        process.ppid = process_object.get("ppid").to_u32();
        process.nfds = process_object.get("nfds").to_u32();
        process.name = process_object.get("name").to_string();
        process.tty = process_object.get("tty").to_string();
        process.amount_virtual = process_object.get("amount_virtual").to_u32();
        process.amount_resident = process_object.get("amount_resident").to_u32();
        process.amount_shared = process_object.get("amount_shared").to_u32();
        process.ticks = process_object.get("ticks").to_u32();
        process.priority = process_object.get("priority").to_string();
        process.syscall_count = process_object.get("syscall_count").to_u32();
        process.inode_faults = process_object.get("inode_faults").to_u32();
        process.zero_faults = process_object.get("zero_faults").to_u32();
        process.cow_faults = process_object.get("cow_faults").to_u32();
        process.icon_id = process_object.get("icon_id").to_int();

        // and synthetic data last
        process.username = username_from_uid(process.uid);
        map.set(process.pid, process);
    });

    return map;
}

String CProcessStatisticsReader::username_from_uid(uid_t uid)
{
    if (s_usernames.is_empty()) {
        setpwent();
        while (auto* passwd = getpwent())
            s_usernames.set(passwd->pw_uid, passwd->pw_name);
        endpwent();
    }

    auto it = s_usernames.find(uid);
    if (it != s_usernames.end())
        return (*it).value;
    return String::number(uid);
}

