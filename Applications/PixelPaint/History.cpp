/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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

#include "History.h"
#include "Image.h"
#include "Layer.h"
#include <AK/LogStream.h>
#include <utility>

namespace PixelPaint {

void History::on_action(const Image& image)
{
    m_snapshots.shrink(m_snapshots.size() - m_current_index_back_into_history);
    m_current_index_back_into_history = 0;
    m_snapshots.append(image.take_snapshot());
    if (m_snapshots.size() > s_max_size)
        m_snapshots.take_first();
}

bool History::undo(Image& image)
{
    if (m_snapshots.size() - m_current_index_back_into_history - 1 <= 0)
        return false;

    m_current_index_back_into_history += 1;
    const Image& last_snapshot = *m_snapshots[m_snapshots.size() - m_current_index_back_into_history - 1];
    image.restore_snapshot(last_snapshot);
    return true;
}

bool History::redo(Image& image)
{
    if (m_current_index_back_into_history <= 0)
        return false;

    const Image& last_snapshot = *m_snapshots[m_snapshots.size() - m_current_index_back_into_history];
    m_current_index_back_into_history -= 1;
    image.restore_snapshot(last_snapshot);
    return true;
}

void History::reset(const Image& image)
{
    m_snapshots.clear();
    m_current_index_back_into_history = 0;
    on_action(image);
}

}
