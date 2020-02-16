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

#include <AK/Badge.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelSelection.h>

namespace GUI {

void ModelSelection::set(const ModelIndex& index)
{
    ASSERT(index.is_valid());
    if (m_indexes.size() == 1 && m_indexes.contains(index))
        return;
    m_indexes.clear();
    m_indexes.set(index);
    m_view.notify_selection_changed({});
}

void ModelSelection::add(const ModelIndex& index)
{
    ASSERT(index.is_valid());
    if (m_indexes.contains(index))
        return;
    m_indexes.set(index);
    m_view.notify_selection_changed({});
}

void ModelSelection::toggle(const ModelIndex& index)
{
    ASSERT(index.is_valid());
    if (m_indexes.contains(index))
        m_indexes.remove(index);
    else
        m_indexes.set(index);
    m_view.notify_selection_changed({});
}

bool ModelSelection::remove(const ModelIndex& index)
{
    ASSERT(index.is_valid());
    if (!m_indexes.contains(index))
        return false;
    m_indexes.remove(index);
    m_view.notify_selection_changed({});
    return true;
}

void ModelSelection::clear()
{
    if (m_indexes.is_empty())
        return;
    m_indexes.clear();
    m_view.notify_selection_changed({});
}

}
