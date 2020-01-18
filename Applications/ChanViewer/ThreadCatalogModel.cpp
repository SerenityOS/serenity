/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
    request.set_url(String::format("http://a.4cdn.org/%s/catalog.json", m_board.characters()));

    if (m_pending_job)
        m_pending_job->cancel();
    m_pending_job = request.schedule();

    if (on_load_started)
        on_load_started();

    m_pending_job->on_finish = [this](bool success) {
        auto* response = m_pending_job->response();
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
