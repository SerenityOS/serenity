#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CFile.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <pwd.h>
#include <stdio.h>

CProcessStatisticsReader::CProcessStatisticsReader()
{
    setpwent();
    while (auto* passwd = getpwent())
        m_usernames.set(passwd->pw_uid, passwd->pw_name);
    endpwent();
}

HashMap<pid_t, CProcessStatistics> CProcessStatisticsReader::get_map()
{
    HashMap<pid_t, CProcessStatistics> res;
    update_map(res);
    return res;
}

void CProcessStatisticsReader::update_map(HashMap<pid_t, CProcessStatistics>& map)
{
    CFile file("/proc/all");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "CProcessHelper : failed to open /proc/all: %s\n", file.error_string());
        return;
    }

    auto file_contents = file.read_all();
    auto json = JsonValue::from_string({ file_contents.data(), file_contents.size() });
    json.as_array().for_each([&](auto& value) {
        const JsonObject& process_object = value.as_object();
        CProcessStatistics process;
        process.pid = process_object.get("pid").to_dword();
        process.nsched = process_object.get("times_scheduled").to_dword();
        process.uid = process_object.get("uid").to_dword();
        process.username = get_username_from_uid(process.uid);
        process.priority = process_object.get("priority").to_string();
        process.syscalls = process_object.get("syscall_count").to_dword();
        process.state = process_object.get("state").to_string();
        process.name = process_object.get("name").to_string();
        process.virtual_size = process_object.get("amount_virtual").to_dword();
        process.physical_size = process_object.get("amount_resident").to_dword();
        map.set(process.pid, process);
    });
}

String CProcessStatisticsReader::get_username_from_uid(const uid_t uid)
{
    auto it = m_usernames.find(uid);
    if (it != m_usernames.end())
        return (*it).value;
    else
        return String::format("%u", uid);
}
