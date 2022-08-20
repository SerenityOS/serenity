/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibConfig/Listener.h>
#include <LibGUI/Frame.h>

namespace Cards {

class CardGame
    : public GUI::Frame
    , public Config::Listener {
public:
    virtual ~CardGame() = default;

    Gfx::Color background_color() const;
    void set_background_color(Gfx::Color const&);

protected:
    CardGame();

private:
    virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value) override;
};

}
