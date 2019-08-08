#include "NetworkAdapterModel.h"
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>

void NetworkAdapterModel::update()
{
    CFile file("/proc/netadapters");
    if (!file.open(CIODevice::ReadOnly)) {
        dbg() << "Unable to open " << file.filename();
        return;
    }

    auto json = JsonValue::from_string(file.read_all());

    ASSERT(json.is_array());
    m_netadapters = json.as_array();

    did_update();
}

int NetworkAdapterModel::row_count(const GModelIndex&) const
{
    return m_netadapters.size();
}

String NetworkAdapterModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    case Column::ClassName:
        return "Class";
    case Column::MacAddress:
        return "MAC";
    case Column::IpAddress:
        return "IP";
    case Column::PacketsIn:
        return "Pkt In";
    case Column::PacketsOut:
        return "Pkt Out";
    case Column::BytesIn:
        return "Bytes In";
    case Column::BytesOut:
        return "Bytes Out";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata NetworkAdapterModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Name:
        return { 32, TextAlignment::CenterLeft };
    case Column::ClassName:
        return { 120, TextAlignment::CenterLeft };
    case Column::MacAddress:
        return { 90, TextAlignment::CenterLeft };
    case Column::IpAddress:
        return { 80, TextAlignment::CenterLeft };
    case Column::PacketsIn:
        return { 60, TextAlignment::CenterRight };
    case Column::PacketsOut:
        return { 60, TextAlignment::CenterRight };
    case Column::BytesIn:
        return { 60, TextAlignment::CenterRight };
    case Column::BytesOut:
        return { 60, TextAlignment::CenterRight };
    default:
        ASSERT_NOT_REACHED();
    }
    return {};
}

GVariant NetworkAdapterModel::data(const GModelIndex& index, Role role) const
{
    auto& adapter_object = m_netadapters.at(index.row()).as_object();
    if (role == GModel::Role::Display) {
        switch (index.column()) {
        case Column::Name:
            return adapter_object.get("name").to_string();
        case Column::ClassName:
            return adapter_object.get("class_name").to_string();
        case Column::MacAddress:
            return adapter_object.get("mac_address").to_string();
        case Column::IpAddress:
            return adapter_object.get("ipv4_address").to_string();
        case Column::PacketsIn:
            return adapter_object.get("packets_in").to_u32();
        case Column::PacketsOut:
            return adapter_object.get("packets_out").to_u32();
        case Column::BytesIn:
            return adapter_object.get("bytes_in").to_u32();
        case Column::BytesOut:
            return adapter_object.get("bytes_out").to_u32();
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return {};
}
