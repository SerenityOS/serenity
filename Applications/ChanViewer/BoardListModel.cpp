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

#include "BoardListModel.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/HttpRequest.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/NetworkResponse.h>
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
    Core::HttpRequest request;
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

int BoardListModel::row_count(const GUI::ModelIndex&) const
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

GUI::Model::ColumnMetadata BoardListModel::column_metadata([[maybe_unused]] int column) const
{
    return {};
}

GUI::Variant BoardListModel::data(const GUI::ModelIndex& index, Role role) const
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
