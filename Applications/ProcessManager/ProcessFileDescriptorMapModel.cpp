#include "ProcessFileDescriptorMapModel.h"
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>

void ProcessFileDescriptorMapModel::update()
{
    CFile file(String::format("/proc/%d/fds", m_pid));
    if (!file.open(CIODevice::ReadOnly)) {
        dbg() << "Unable to open " << file.filename();
        return;
    }

    auto json = JsonValue::from_string(file.read_all());

    ASSERT(json.is_array());
    m_process_fds = json.as_array();

    did_update();
}

int ProcessFileDescriptorMapModel::row_count(const GModelIndex&) const
{
    return m_process_fds.size();
}

String ProcessFileDescriptorMapModel::column_name(int column) const
{
    switch (column) {
    case Column::FileDescriptor:
        return "FD";
    case Column::Path:
        return "Path";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata ProcessFileDescriptorMapModel::column_metadata(int column) const
{
    switch (column) {
    case Column::FileDescriptor:
        return { 32, TextAlignment::CenterRight };
    case Column::Path:
        return { 300, TextAlignment::CenterLeft };
    default:
        ASSERT_NOT_REACHED();
    }
    return {};
}

GVariant ProcessFileDescriptorMapModel::data(const GModelIndex& index, Role role) const
{
    auto& fd_object = m_process_fds.at(index.row()).as_object();
    if (role == GModel::Role::Display) {
        switch (index.column()) {
        case Column::FileDescriptor:
            return fd_object.get("fd").to_int();
        case Column::Path:
            return fd_object.get("absolute_path").to_string();
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return {};
}

void ProcessFileDescriptorMapModel::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    update();
}
