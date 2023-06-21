/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ChessWidget.h"
#include <LibGUI/Dialog.h>

class PromotionDialog final : public GUI::Dialog {
    C_OBJECT(PromotionDialog)
public:
    Chess::Type selected_piece() const { return m_selected_piece; }

private:
    explicit PromotionDialog(ChessWidget& chess_widget);
    virtual void event(Core::Event&) override;

    Chess::Type m_selected_piece;
};
