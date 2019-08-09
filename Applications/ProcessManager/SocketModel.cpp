#include "SocketModel.h"
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>

void SocketModel::update()
{
    CFile file("/proc/net/tcp");
    if (!file.open(CIODevice::ReadOnly)) {
        dbg() << "Unable to open " << file.filename();
        return;
    }

    auto json = JsonValue::from_string(file.read_all());

    ASSERT(json.is_array());
    m_sockets = json.as_array();

    did_update();
}

int SocketModel::row_count(const GModelIndex&) const
{
    return m_sockets.size();
}

String SocketModel::column_name(int column) const
{
    switch (column) {
    case Column::PeerAddress:
        return "Peer";
    case Column::PeerPort:
        return "Port";
    case Column::LocalAddress:
        return "Local";
    case Column::LocalPort:
        return "Port";
    case Column::State:
        return "State";
    case Column::SeqNumber:
        return "Seq#";
    case Column::AckNumber:
        return "Ack#";
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

GModel::ColumnMetadata SocketModel::column_metadata(int column) const
{
    switch (column) {
    case Column::PeerAddress:
        return { 80, TextAlignment::CenterLeft };
    case Column::PeerPort:
        return { 30, TextAlignment::CenterRight };
    case Column::LocalAddress:
        return { 80, TextAlignment::CenterLeft };
    case Column::LocalPort:
        return { 30, TextAlignment::CenterRight };
    case Column::State:
        return { 80, TextAlignment::CenterLeft };
    case Column::AckNumber:
        return { 60, TextAlignment::CenterRight };
    case Column::SeqNumber:
        return { 60, TextAlignment::CenterRight };
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

GVariant SocketModel::data(const GModelIndex& index, Role role) const
{
    auto& socket_object = m_sockets.at(index.row()).as_object();
    if (role == GModel::Role::Display) {
        switch (index.column()) {
        case Column::PeerAddress:
            return socket_object.get("peer_address").to_string();
        case Column::PeerPort:
            return socket_object.get("peer_port").to_u32();
        case Column::LocalAddress:
            return socket_object.get("local_address").to_string();
        case Column::LocalPort:
            return socket_object.get("local_port").to_u32();
        case Column::State:
            return socket_object.get("state").to_string();
        case Column::AckNumber:
            return socket_object.get("ack_number").to_u32();
        case Column::SeqNumber:
            return socket_object.get("sequence_number").to_u32();
        case Column::PacketsIn:
            return socket_object.get("packets_in").to_u32();
        case Column::PacketsOut:
            return socket_object.get("packets_out").to_u32();
        case Column::BytesIn:
            return socket_object.get("bytes_in").to_u32();
        case Column::BytesOut:
            return socket_object.get("bytes_out").to_u32();
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return {};
}
