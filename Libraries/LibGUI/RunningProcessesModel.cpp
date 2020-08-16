/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/SharedBuffer.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibGUI/RunningProcessesModel.h>

namespace GUI {

NonnullRefPtr<RunningProcessesModel> RunningProcessesModel::create()
{
    return adopt(*new RunningProcessesModel);
}

RunningProcessesModel::RunningProcessesModel()
{
}

RunningProcessesModel::~RunningProcessesModel()
{
}

void RunningProcessesModel::update()
{
    m_processes.clear();

    Core::ProcessStatisticsReader reader;
    auto processes = reader.get_all();

    for (auto& it : processes) {
        Process process;
        process.pid = it.value.pid;
        process.uid = it.value.uid;
        if (it.value.icon_id != -1) {
            if (auto icon_buffer = SharedBuffer::create_from_shbuf_id(it.value.icon_id)) {
                process.icon = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, *icon_buffer, { 16, 16 });
            }
        }
        process.name = it.value.name;

        m_processes.append(move(process));
    }

    did_update();
}

int RunningProcessesModel::row_count(const GUI::ModelIndex&) const
{
    return m_processes.size();
}

int RunningProcessesModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

String RunningProcessesModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::Icon:
        return {};
    case Column::PID:
        return "PID";
    case Column::UID:
        return "UID";
    case Column::Name:
        return "Name";
    }
    ASSERT_NOT_REACHED();
}

GUI::Variant RunningProcessesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& process = m_processes[index.row()];

    if (role == ModelRole::Custom) {
        return process.pid;
    }

    if (role == ModelRole::Display) {
        switch (index.column()) {
        case Column::Icon:
            if (!process.icon)
                return GUI::Icon();
            return GUI::Icon(*process.icon);
        case Column::PID:
            return process.pid;
        case Column::UID:
            return process.uid;
        case Column::Name:
            return process.name;
        }
        ASSERT_NOT_REACHED();
    }
    return {};
}

}
