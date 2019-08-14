#include "ProcessModel.h"
#include "GraphWidget.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibC/SharedBuffer.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <fcntl.h>
#include <stdio.h>

ProcessModel::ProcessModel(GraphWidget& graph)
    : m_graph(graph)
{
    m_generic_process_icon = GraphicsBitmap::load_from_file("/res/icons/gear16.png");
    m_high_priority_icon = GraphicsBitmap::load_from_file("/res/icons/highpriority16.png");
    m_low_priority_icon = GraphicsBitmap::load_from_file("/res/icons/lowpriority16.png");
    m_normal_priority_icon = GraphicsBitmap::load_from_file("/res/icons/normalpriority16.png");
}

ProcessModel::~ProcessModel()
{
}

int ProcessModel::row_count(const GModelIndex&) const
{
    return m_pids.size();
}

int ProcessModel::column_count(const GModelIndex&) const
{
    return Column::__Count;
}

String ProcessModel::column_name(int column) const
{
    switch (column) {
    case Column::Icon:
        return "";
    case Column::PID:
        return "PID";
    case Column::State:
        return "State";
    case Column::User:
        return "User";
    case Column::Priority:
        return "Pr";
    case Column::Virtual:
        return "Virtual";
    case Column::Physical:
        return "Physical";
    case Column::CPU:
        return "CPU";
    case Column::Name:
        return "Name";
    case Column::Syscalls:
        return "Syscalls";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata ProcessModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Icon:
        return { 16, TextAlignment::CenterLeft };
    case Column::PID:
        return { 32, TextAlignment::CenterRight };
    case Column::State:
        return { 75, TextAlignment::CenterLeft };
    case Column::Priority:
        return { 16, TextAlignment::CenterLeft };
    case Column::User:
        return { 50, TextAlignment::CenterLeft };
    case Column::Virtual:
        return { 65, TextAlignment::CenterRight };
    case Column::Physical:
        return { 65, TextAlignment::CenterRight };
    case Column::CPU:
        return { 32, TextAlignment::CenterRight };
    case Column::Name:
        return { 140, TextAlignment::CenterLeft };
    case Column::Syscalls:
        return { 60, TextAlignment::CenterRight };
    default:
        ASSERT_NOT_REACHED();
    }
}

static String pretty_byte_size(size_t size)
{
    return String::format("%uK", size / 1024);
}

GVariant ProcessModel::data(const GModelIndex& index, Role role) const
{
    ASSERT(is_valid(index));

    auto it = m_processes.find(m_pids[index.row()]);
    auto& process = *(*it).value;

    if (role == Role::Sort) {
        switch (index.column()) {
        case Column::Icon:
            return 0;
        case Column::PID:
            return process.current_state.pid;
        case Column::State:
            return process.current_state.state;
        case Column::User:
            return process.current_state.user;
        case Column::Priority:
            if (process.current_state.priority == "Idle")
                return 0;
            if (process.current_state.priority == "Low")
                return 1;
            if (process.current_state.priority == "Normal")
                return 2;
            if (process.current_state.priority == "High")
                return 3;
            ASSERT_NOT_REACHED();
            return 3;
        case Column::Virtual:
            return (int)process.current_state.amount_virtual;
        case Column::Physical:
            return (int)process.current_state.amount_resident;
        case Column::CPU:
            return process.current_state.cpu_percent;
        case Column::Name:
            return process.current_state.name;
        // FIXME: GVariant with unsigned?
        case Column::Syscalls:
            return (int)process.current_state.syscall_count;
        }
        ASSERT_NOT_REACHED();
        return {};
    }

    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Icon:
            if (process.current_state.icon_id != -1) {
                auto icon_buffer = SharedBuffer::create_from_shared_buffer_id(process.current_state.icon_id);
                if (icon_buffer) {
                    auto icon_bitmap = GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, *icon_buffer, { 16, 16 });
                    if (icon_bitmap)
                        return *icon_bitmap;
                }
            }
            return *m_generic_process_icon;
        case Column::PID:
            return process.current_state.pid;
        case Column::State:
            return process.current_state.state;
        case Column::User:
            return process.current_state.user;
        case Column::Priority:
            if (process.current_state.priority == "Idle")
                return String::empty();
            if (process.current_state.priority == "High")
                return *m_high_priority_icon;
            if (process.current_state.priority == "Low")
                return *m_low_priority_icon;
            if (process.current_state.priority == "Normal")
                return *m_normal_priority_icon;
            return process.current_state.priority;
        case Column::Virtual:
            return pretty_byte_size(process.current_state.amount_virtual);
        case Column::Physical:
            return pretty_byte_size(process.current_state.amount_resident);
        case Column::CPU:
            return process.current_state.cpu_percent;
        case Column::Name:
            return process.current_state.name;
        // FIXME: It's weird that GVariant doesn't support unsigned ints. Should it?
        case Column::Syscalls:
            return (int)process.current_state.syscall_count;
        }
    }

    return {};
}

void ProcessModel::update()
{
    auto all_processes = CProcessStatisticsReader::get_all();

    unsigned last_sum_times_scheduled = 0;
    for (auto& it : m_processes)
        last_sum_times_scheduled += it.value->current_state.times_scheduled;

    HashTable<pid_t> live_pids;
    unsigned sum_times_scheduled = 0;
    for (auto& it : all_processes) {
        ProcessState state;
        state.pid = it.value.pid;
        state.times_scheduled = it.value.times_scheduled;
        state.user = it.value.username;
        state.priority = it.value.priority;
        state.syscall_count = it.value.syscall_count;
        state.state = it.value.state;
        state.name = it.value.name;
        state.amount_virtual = it.value.amount_virtual;
        state.amount_resident = it.value.amount_resident;
        state.icon_id = it.value.icon_id;
        sum_times_scheduled += it.value.times_scheduled;
        {
            auto pit = m_processes.find(it.value.pid);
            if (pit == m_processes.end())
                m_processes.set(it.value.pid, make<Process>());
        }
        auto pit = m_processes.find(it.value.pid);
        ASSERT(pit != m_processes.end());
        (*pit).value->previous_state = (*pit).value->current_state;
        (*pit).value->current_state = state;

        live_pids.set(it.value.pid);
    }

    m_pids.clear();
    float total_cpu_percent = 0;
    Vector<pid_t, 16> pids_to_remove;
    for (auto& it : m_processes) {
        if (!live_pids.contains(it.key)) {
            pids_to_remove.append(it.key);
            continue;
        }
        auto& process = *it.value;
        u32 times_scheduled_diff = process.current_state.times_scheduled - process.previous_state.times_scheduled;
        process.current_state.cpu_percent = ((float)times_scheduled_diff * 100) / (float)(sum_times_scheduled - last_sum_times_scheduled);
        if (it.key != 0) {
            total_cpu_percent += process.current_state.cpu_percent;
            m_pids.append(it.key);
        }
    }
    for (auto pid : pids_to_remove)
        m_processes.remove(pid);

    m_graph.add_value(total_cpu_percent);

    did_update();
}
