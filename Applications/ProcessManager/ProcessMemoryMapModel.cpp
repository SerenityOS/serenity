#include "ProcessMemoryMapModel.h"
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>

void ProcessMemoryMapModel::update()
{
    CFile file(String::format("/proc/%d/vm", m_pid));
    if (!file.open(CIODevice::ReadOnly)) {
        dbg() << "Unable to open " << file.filename();
        return;
    }

    auto json = JsonValue::from_string(file.read_all());

    ASSERT(json.is_array());
    m_process_vm = json.as_array();

    did_update();
}

int ProcessMemoryMapModel::row_count(const GModelIndex&) const
{
    return m_process_vm.size();
}

String ProcessMemoryMapModel::column_name(int column) const
{
    switch (column) {
    case Column::Address:
        return "Address";
    case Column::Size:
        return "Size";
    case Column::AmountResident:
        return "Resident";
    case Column::Access:
        return "Access";
    case Column::Name:
        return "Name";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata ProcessMemoryMapModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Address:
        return { 80 };
    case Column::Size:
        return { 60, TextAlignment::CenterRight };
    case Column::AmountResident:
        return { 60, TextAlignment::CenterRight };
    case Column::Access:
        return { 50 };
    case Column::Name:
        return { 200 };
    default:
        ASSERT_NOT_REACHED();
    }
    return {};
}

GVariant ProcessMemoryMapModel::data(const GModelIndex& index, Role role) const
{
    auto& region_object = m_process_vm.at(index.row()).as_object();
    if (role == GModel::Role::Display) {
        switch (index.column()) {
        case Column::Address:
            return String::format("%#x", region_object.get("address").to_u32());
        case Column::Size:
            return region_object.get("size").to_int();
        case Column::AmountResident:
            return region_object.get("amount_resident").to_int();
        case Column::Access: {
            StringBuilder builder;
            if (region_object.get("readable").to_bool())
                builder.append('R');
            if (region_object.get("writable").to_bool())
                builder.append('W');
            return builder.to_string();
        }
        case Column::Name:
            return region_object.get("name").to_string();
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return {};
}

void ProcessMemoryMapModel::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    update();
}
