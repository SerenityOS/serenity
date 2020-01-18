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

#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>

GModel::GModel()
{
}

GModel::~GModel()
{
}

void GModel::register_view(Badge<GAbstractView>, GAbstractView& view)
{
    m_views.set(&view);
}

void GModel::unregister_view(Badge<GAbstractView>, GAbstractView& view)
{
    m_views.remove(&view);
}

void GModel::for_each_view(Function<void(GAbstractView&)> callback)
{
    for (auto* view : m_views)
        callback(*view);
}

void GModel::did_update()
{
    if (on_update)
        on_update();
    for_each_view([](auto& view) {
        view.did_update_model();
    });
}

GModelIndex GModel::create_index(int row, int column, const void* data) const
{
    return GModelIndex(*this, row, column, const_cast<void*>(data));
}

GModelIndex GModel::sibling(int row, int column, const GModelIndex& parent) const
{
    if (!parent.is_valid())
        return index(row, column, {});
    int row_count = this->row_count(parent);
    if (row < 0 || row > row_count)
        return {};
    return index(row, column, parent);
}
