#include "ThreadCatalogModel.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CHttpRequest.h>
#include <LibCore/CNetworkJob.h>
#include <LibCore/CNetworkResponse.h>
#include <stdio.h>

ThreadCatalogModel::ThreadCatalogModel()
{
    update();
}

ThreadCatalogModel::~ThreadCatalogModel()
{
}

void ThreadCatalogModel::set_board(const String& board)
{
    if (m_board == board)
        return;
    m_board = board;
    update();
}

void ThreadCatalogModel::update()
{
    CHttpRequest request;
    request.set_hostname("a.4cdn.org");
    request.set_path(String::format("/%s/catalog.json", m_board.characters()));

    auto* job = request.schedule();

    if (on_load_started)
        on_load_started();

    job->on_finish = [job, this](bool success) {
        auto* response = job->response();
        dbg() << "Catalog download finished, success=" << success << ", response=" << response;

        if (!success) {
            if (on_load_finished)
                on_load_finished(false);
            return;
        }

        dbg() << "Catalog payload size: " << response->payload().size();

        auto json = JsonValue::from_string(response->payload());

        if (json.is_array()) {
            JsonArray new_catalog;

            for (auto& page : json.as_array().values()) {
                if (!page.is_object())
                    continue;
                auto threads_value = page.as_object().get("threads");
                if (!threads_value.is_array())
                    continue;
                for (auto& thread : threads_value.as_array().values()) {
                    new_catalog.append(thread);
                }
            }

            m_catalog = move(new_catalog);
        }

        did_update();

        if (on_load_finished)
            on_load_finished(true);
    };
}

int ThreadCatalogModel::row_count(const GModelIndex&) const
{
    return m_catalog.size();
}

String ThreadCatalogModel::column_name(int column) const
{
    switch (column) {
    case Column::ThreadNumber:
        return "#";
    case Column::Subject:
        return "Subject";
    case Column::Text:
        return "Text";
    case Column::ReplyCount:
        return "Replies";
    case Column::ImageCount:
        return "Images";
    case Column::PostTime:
        return "Time";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata ThreadCatalogModel::column_metadata(int column) const
{
    switch (column) {
    case Column::ThreadNumber:
        return { 70, TextAlignment::CenterRight };
    case Column::Subject:
        return { 170, TextAlignment::CenterLeft };
    case Column::Text:
        return { 270, TextAlignment::CenterLeft };
    case Column::ReplyCount:
        return { 45, TextAlignment::CenterRight };
    case Column::ImageCount:
        return { 40, TextAlignment::CenterRight };
    case Column::PostTime:
        return { 120, TextAlignment::CenterLeft };
    default:
        ASSERT_NOT_REACHED();
    }
}

GVariant ThreadCatalogModel::data(const GModelIndex& index, Role role) const
{
    auto& thread = m_catalog.at(index.row()).as_object();
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::ThreadNumber:
            return thread.get("no").to_u32();
        case Column::Subject:
            return thread.get("sub").as_string_or({});
        case Column::Text:
            return thread.get("com").as_string_or({});
        case Column::ReplyCount:
            return thread.get("replies").to_u32();
        case Column::ImageCount:
            return thread.get("images").to_u32();
        case Column::PostTime:
            return thread.get("now").to_string();
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return {};
}
