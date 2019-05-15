#include "CProcessHelper.h"
#include "CFile.h"

#include <stdio.h>
#include <pwd.h>

CProcessHelper::CProcessHelper()
{
    setpwent();
    while (auto* passwd = getpwent())
	m_usernames.set(passwd->pw_uid, passwd->pw_name);
    endpwent();
}

HashMap<pid_t, RetainPtr<CProcessInfo>> CProcessHelper::get_map()
{
    HashMap<pid_t, RetainPtr<CProcessInfo>> res;
    int error = 0;
  
    if ((error = update_map(res)) != 0) {
	fprintf(stderr, "CProcessHelper::update_map failed with code %d\n", error);
    }

    return res;
}

int CProcessHelper::update_map(HashMap<pid_t, RetainPtr<CProcessInfo>>& map)
{
    CFile file("/proc/all");
    if (!file.open(CIODevice::ReadOnly)) {
	fprintf(stderr, "CProcessHelper : failed to open /proc/all: %s\n", file.error_string());
	return 1;
    }

    for (;;) {
	auto line = file.read_line(1024);

	if (line.is_empty())
	    break;

	auto chomped = String((const char*)line.pointer(), line.size() - 1, Chomp);
	auto parts = chomped.split_view(',');

	if (parts.size() < 18)
	    break;

	bool ok = false;
	RetainPtr<CProcessInfo> process = RetainPtr<CProcessInfo>(new CProcessInfo);

	process->pid = parts[0].to_uint(ok);
	if (!ok) {
	    fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid pid\n", parts[0].characters());
	    return 2;
	}

	process->nsched = parts[1].to_uint(ok);
	if (!ok) {
	    fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid nsched value\n", parts[1].characters());
	    return 3;
	}

	uid_t uid = parts[5].to_uint(ok);
	if (!ok) {
	    fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid uid value\n", parts[5].characters());
	    return 4;
	}

	process->uid = uid;
	process->username = get_username_from_uid(uid);

	process->priority = parts[16];
    
	process->syscalls = parts[17].to_uint(ok);
	if (!ok) {
	    fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid syscalls count value\n", parts[17].characters());
	    return 5;
	}

	process->state = parts[7];

	process->name = parts[11];
	process->linear = parts[12].to_uint(ok);
	if (!ok) {
	    fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid linear address\n", parts[12].characters());
	    return 6;
	}

	process->physical = parts[13].to_uint(ok);
	if (!ok) {
	    fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid physical address\n", parts[13].characters());
	    return 7;
	}

	map.set(process->pid, process);
    }

    return 0;
}

String CProcessHelper::get_username_from_uid(const uid_t uid)
{
    auto it = m_usernames.find(uid);
    if (it != m_usernames.end())
	return (*it).value;
    else
	return String::format("%u", uid);
}
