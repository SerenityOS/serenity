#include "ProcessTableModel.h"
#include <fcntl.h>
#include <stdio.h>
#include <pwd.h>

ProcessTableModel::ProcessTableModel()
{
    setpwent();
    while (auto* passwd = getpwent())
        m_usernames.set(passwd->pw_uid, passwd->pw_name);
    endpwent();

    m_generic_process_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/gear16.rgb", { 16, 16 });
    m_high_priority_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/highpriority16.rgb", { 16, 16 });
    m_low_priority_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/lowpriority16.rgb", { 16, 16 });
    m_normal_priority_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/normalpriority16.rgb", { 16, 16 });
}

ProcessTableModel::~ProcessTableModel()
{
}

int ProcessTableModel::row_count() const
{
    return m_processes.size();
}

int ProcessTableModel::column_count() const
{
    return Column::__Count;
}

String ProcessTableModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon: return "";
    case Column::PID: return "PID";
    case Column::State: return "State";
    case Column::User: return "User";
    case Column::Priority: return "Pr";
    case Column::Linear: return "Linear";
    case Column::Physical: return "Physical";
    case Column::CPU: return "CPU";
    case Column::Name: return "Name";
    default: ASSERT_NOT_REACHED();
    }
}

GTableModel::ColumnMetadata ProcessTableModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Icon: return { 16, TextAlignment::CenterLeft };
    case Column::PID: return { 25, TextAlignment::CenterRight };
    case Column::State: return { 75, TextAlignment::CenterLeft };
    case Column::Priority: return { 16, TextAlignment::CenterLeft };
    case Column::User: return { 50, TextAlignment::CenterLeft };
    case Column::Linear: return { 65, TextAlignment::CenterRight };
    case Column::Physical: return { 65, TextAlignment::CenterRight };
    case Column::CPU: return { 25, TextAlignment::CenterRight };
    case Column::Name: return { 140, TextAlignment::CenterLeft };
    default: ASSERT_NOT_REACHED();
    }
}

static String pretty_byte_size(size_t size)
{
    return String::format("%uK", size / 1024);
}

GVariant ProcessTableModel::data(const GModelIndex& index, Role role) const
{
    ASSERT(is_valid(index));

    auto it = m_processes.find(m_pids[index.row()]);
    auto& process = *(*it).value;

    if (role == Role::Sort) {
        switch (index.column()) {
        case Column::Icon: return 0;
        case Column::PID: return process.current_state.pid;
        case Column::State: return process.current_state.state;
        case Column::User: return process.current_state.user;
        case Column::Priority:
            if (process.current_state.priority == "Low")
                return 0;
            if (process.current_state.priority == "Normal")
                return 1;
            if (process.current_state.priority == "High")
                return 2;
            ASSERT_NOT_REACHED();
            return 3;
        case Column::Linear: return (int)process.current_state.linear;
        case Column::Physical: return (int)process.current_state.physical;
        case Column::CPU: return process.current_state.cpu_percent;
        case Column::Name: return process.current_state.name;
        }
        ASSERT_NOT_REACHED();
        return { };
    }

    switch (index.column()) {
    case Column::Icon: return *m_generic_process_icon;
    case Column::PID: return process.current_state.pid;
    case Column::State: return process.current_state.state;
    case Column::User: return process.current_state.user;
    case Column::Priority:
        if (process.current_state.priority == "High")
            return *m_high_priority_icon;
        if (process.current_state.priority == "Low")
            return *m_low_priority_icon;
        if (process.current_state.priority == "Normal")
            return *m_normal_priority_icon;
        return process.current_state.priority;
    case Column::Linear: return pretty_byte_size(process.current_state.linear);
    case Column::Physical: return pretty_byte_size(process.current_state.physical);
    case Column::CPU: return process.current_state.cpu_percent;
    case Column::Name: return process.current_state.name;
    }
    ASSERT_NOT_REACHED();
}

void ProcessTableModel::update()
{
    FILE* fp = fopen("/proc/all", "r");
    if (!fp) {
        perror("failed to open /proc/all");
        exit(1);
    }

    unsigned last_sum_nsched = 0;
    for (auto& it : m_processes)
        last_sum_nsched += it.value->current_state.nsched;

    HashTable<pid_t> live_pids;
    unsigned sum_nsched = 0;
    for (;;) {
        char buf[BUFSIZ];
        char* ptr = fgets(buf, sizeof(buf), fp);
        if (!ptr)
            break;
        auto parts = String(buf, Chomp).split(',');
        if (parts.size() < 17)
            break;
        bool ok;
        pid_t pid = parts[0].to_uint(ok);
        ASSERT(ok);
        unsigned nsched = parts[1].to_uint(ok);
        ASSERT(ok);
        ProcessState state;
        state.pid = pid;
        state.nsched = nsched;
        unsigned uid = parts[5].to_uint(ok);
        ASSERT(ok);
        {
            auto it = m_usernames.find((uid_t)uid);
            if (it != m_usernames.end())
                state.user = String::format("%s", (*it).value.characters());
            else
                state.user = String::format("%u", uid);
        }
        state.priority = parts[16];
        state.state = parts[7];
        state.name = parts[11];
        state.linear = parts[12].to_uint(ok);
        ASSERT(ok);
        state.physical = parts[13].to_uint(ok);
        ASSERT(ok);
        sum_nsched += nsched;
        {
            auto it = m_processes.find(pid);
            if (it == m_processes.end())
                m_processes.set(pid, make<Process>());
        }
        auto it = m_processes.find(pid);
        ASSERT(it != m_processes.end());
        (*it).value->previous_state = (*it).value->current_state;
        (*it).value->current_state = state;

        live_pids.set(pid);
    }
    int rc = fclose(fp);
    ASSERT(rc == 0);

    m_pids.clear();
    Vector<pid_t> pids_to_remove;
    for (auto& it : m_processes) {
        if (!live_pids.contains(it.key)) {
            pids_to_remove.append(it.key);
            continue;
        }

        auto& process = *it.value;
        dword nsched_diff = process.current_state.nsched - process.previous_state.nsched;
        process.current_state.cpu_percent = ((float)nsched_diff * 100) / (float)(sum_nsched - last_sum_nsched);
        m_pids.append(it.key);
    }
    for (auto pid : pids_to_remove)
        m_processes.remove(pid);

    did_update();
}

pid_t ProcessTableModel::selected_pid() const
{
    if (!selected_index().is_valid())
        return -1;
    return m_pids[selected_index().row()];
}
