/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibBitTorrent/TorrentView.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Widget.h>

class PeerListWidget : public GUI::Widget {
    C_OBJECT(PeerListWidget)
public:
    PeerListWidget();
    void update(Vector<BitTorrent::PeerView>);

private:
    RefPtr<GUI::TableView> m_peers_table_view;
};
