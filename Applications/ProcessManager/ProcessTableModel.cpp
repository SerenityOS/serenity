#include "ProcessTableModel.h"
#include <fcntl.h>
#include <stdio.h>

enum Column {
    PID = 0,
    State,
    Priority,
    Linear,
    Physical,
    CPU,
    Name,
    __Count
};

ProcessTableModel::ProcessTableModel()
{
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
    case Column::PID: return "PID";
    case Column::State: return "State";
    case Column::Priority: return "Priority";
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
    case Column::PID: return { 30, TextAlignment::CenterRight };
    case Column::State: return { 80, TextAlignment::CenterLeft };
    case Column::Priority: return { 75, TextAlignment::CenterLeft };
    case Column::Linear: return { 70, TextAlignment::CenterRight };
    case Column::Physical: return { 70, TextAlignment::CenterRight };
    case Column::CPU: return { 30, TextAlignment::CenterRight };
    case Column::Name: return { 200, TextAlignment::CenterLeft };
    default: ASSERT_NOT_REACHED();
    }
}

GModelIndex ProcessTableModel::selected_index() const
{
    return { m_selected_row, 0 };
}

void ProcessTableModel::set_selected_index(GModelIndex index)
{
    if (index.row() >= 0 && index.row() < m_pids.size())
        m_selected_row = index.row();
}

static String pretty_byte_size(size_t size)
{
    return String::format("%uK", size / 1024);
}

String ProcessTableModel::data(int row, int column) const
{
    ASSERT(is_valid({ row, column }));
    auto it = m_processes.find(m_pids[row]);
    auto& process = *(*it).value;
    switch (column) {
    case Column::PID: return String::format("%d", process.current_state.pid);
    case Column::State: return process.current_state.state;
    case Column::Priority: return process.current_state.priority;
    case Column::Linear: return pretty_byte_size(process.current_state.linear);
    case Column::Physical: return pretty_byte_size(process.current_state.physical);
    case Column::CPU: return String::format("%d", (int)process.current_state.cpu_percent);
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
        //state.user = s_usernames->get(uid);
        state.user = String::format("%u", uid);
        state.priority = parts[16];
        state.state = parts[7];
        state.name = parts[11];
        state.linear = parts[12].to_uint(ok);
        ASSERT(ok);
        state.physical = parts[13].to_uint(ok);
        ASSERT(ok);

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

        sum_nsched += nsched;
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
    if (m_selected_row == -1)
        return -1;
    return m_pids[m_selected_row];
}
