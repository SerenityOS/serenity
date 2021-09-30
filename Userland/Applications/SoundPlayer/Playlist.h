/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "M3UParser.h"
#include "PlaylistWidget.h"
#include <AK/StringView.h>
#include <AK/Vector.h>

class Playlist {
public:
    Playlist()
        : m_model(adopt_ref(*new PlaylistModel()))
    {
    }

    bool load(StringView);

    RefPtr<PlaylistModel> model() { return m_model; }
    int size() { return m_model->items().size(); }

    StringView next();
    StringView previous();

    void set_looping(bool looping) { m_looping = looping; }
    bool looping() const { return m_looping; }

private:
    void try_fill_missing_info(Vector<M3UEntry>&, StringView);

    RefPtr<PlaylistModel> m_model;
    bool m_looping { false };

    int m_next_index_to_play { 0 };
};
