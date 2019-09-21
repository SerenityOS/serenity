#include "BoardListModel.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CHttpRequest.h>
#include <LibCore/CNetworkJob.h>
#include <LibCore/CNetworkResponse.h>
#include <stdio.h>

BoardListModel::BoardListModel()
{
    update();
}

BoardListModel::~BoardListModel()
{
}

void BoardListModel::update()
{
    CHttpRequest request;
    request.set_url("http://a.4cdn.org/boards.json");

    if (m_pending_job)
        m_pending_job->cancel();
    m_pending_job = request.schedule();

    m_pending_job->on_finish = [this](bool success) {
        auto* response = m_pending_job->response();
        dbg() << "Board list download finished, success=" << success << ", response=" << response;

        if (!success)
            return;

        dbg() << "Board list payload size: " << response->payload().size();

        auto json = JsonValue::from_string(response->payload());

        if (json.is_object()) {
            auto new_boards = json.as_object().get("boards");
            if (new_boards.is_array())
                m_boards = move(new_boards.as_array());
        }

        did_update();
    };
}

int BoardListModel::row_count(const GModelIndex&) const
{
    return m_boards.size();
}

String BoardListModel::column_name(int column) const
{
    switch (column) {
    case Column::Board:
        return "Board";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata BoardListModel::column_metadata([[maybe_unused]] int column) const
{
    return {};
}

GVariant BoardListModel::data(const GModelIndex& index, Role role) const
{
    auto& board = m_boards.at(index.row()).as_object();
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Board:
            return String::format("/%s/ - %s",
                board.get("board").to_string().characters(),
                board.get("title").to_string().characters());
        default:
            ASSERT_NOT_REACHED();
        }
    }
    if (role == Role::Custom) {
        switch (index.column()) {
        case Column::Board:
            return board.get("board").to_string();
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return {};
}
