/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCards/Card.h>
#include <LibCards/CardGame.h>
#include <LibCards/CardPainter.h>
#include <LibCards/CardStack.h>
#include <LibConfig/Client.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGfx/Palette.h>

namespace GamesSettings {

class CardGamePreview final : public Cards::CardGame {
    C_OBJECT_ABSTRACT(CardGamePreview)

public:
    static ErrorOr<NonnullRefPtr<CardGamePreview>> try_create();

private:
    CardGamePreview() = default;

    virtual void paint_event(GUI::PaintEvent& event) override;
};

}
