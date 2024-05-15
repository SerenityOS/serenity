/*
 * Copyright (c) 2020-2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ChessWidget.h"
#include "PromotionWidget.h"
#include <LibGUI/Dialog.h>

namespace Chess {

class PromotionDialog final : public GUI::Dialog {
    C_OBJECT_ABSTRACT(PromotionDialog)
public:
    static ErrorOr<NonnullRefPtr<PromotionDialog>> try_create(ChessWidget& chess_widget);
    Chess::Type selected_piece() const { return m_selected_piece; }

private:
    PromotionDialog(NonnullRefPtr<Chess::PromotionWidget> promotion_widget, ChessWidget& chess_widget);
    virtual void event(Core::Event&) override;

    Chess::Type m_selected_piece;
};

}
