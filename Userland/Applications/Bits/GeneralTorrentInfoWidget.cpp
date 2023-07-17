/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneralTorrentInfoWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>

TorrentProgressBar::TorrentProgressBar()
{
    set_fixed_height(20);
}

void TorrentProgressBar::update(Optional<BitTorrent::BitField> bitfield)
{
    m_bitfield = bitfield;
    GUI::Widget::update();
}

void TorrentProgressBar::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    painter.clear_rect(rect(), Color::White);

    if (!m_bitfield.has_value())
        return;

    int piece_width = max(rect().width() / m_bitfield->size(), 1);
    for (int i = 0; i < (int)m_bitfield->size(); i++) {
        if (m_bitfield->get(i))
            painter.fill_rect({ i * piece_width, 0, piece_width, height() }, Color::Blue);
    }
}

GeneralTorrentInfoWidget::GeneralTorrentInfoWidget()
{
    set_layout<GUI::VerticalBoxLayout>(4);
    m_progress_bar = add<TorrentProgressBar>();
}
void GeneralTorrentInfoWidget::update(Optional<BitTorrent::TorrentView> torrent)
{
    if (torrent.has_value())
        m_progress_bar->update(torrent.value().bitfield);
    else
        m_progress_bar->update({});
}
