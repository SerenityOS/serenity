/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibBitTorrent/BitField.h>
#include <LibBitTorrent/TorrentView.h>
#include <LibGUI/Button.h>
#include <LibGUI/Widget.h>

class TorrentProgressBar : public GUI::Widget {
    C_OBJECT(TorrentProgressBar)
public:
    TorrentProgressBar();
    void update(Optional<BitTorrent::BitField>);

protected:
    virtual void paint_event(GUI::PaintEvent& event) override;

private:
    Optional<BitTorrent::BitField> m_bitfield;
};

class GeneralTorrentInfoWidget : public GUI::Widget {
    C_OBJECT(GeneralTorrentInfoWidget)
public:
    GeneralTorrentInfoWidget();
    void update(Optional<BitTorrent::TorrentView>);

private:
    RefPtr<TorrentProgressBar> m_progress_bar;
};
