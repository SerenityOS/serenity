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
    CFile file("/proc/all");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "CProcessStatisticsReader: Failed to open /proc/all: %s\n", file.error_string());
        return {};
    }

    HashMap<pid_t, CProcessStatistics> map;

    auto file_contents = file.read_all();
    auto json = JsonValue::from_string({ file_contents.data(), file_contents.size() });
    json.as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        CProcessStatistics process;
        process.pid = process_object.get("pid").to_u32();
        process.nsched = process_object.get("times_scheduled").to_u32();
        process.uid = process_object.get("uid").to_u32();
        process.username = username_from_uid(process.uid);
        process.priority = process_object.get("priority").to_string();
        process.syscalls = process_object.get("syscall_count").to_u32();
        process.state = process_object.get("state").to_string();
        process.name = process_object.get("name").to_string();
        process.virtual_size = process_object.get("amount_virtual").to_u32();
        process.physical_size = process_object.get("amount_resident").to_u32();
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

