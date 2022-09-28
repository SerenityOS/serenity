/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCards/CardStack.h>
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

    NonnullRefPtrVector<CardStack>& stacks() { return m_stacks; }
    NonnullRefPtrVector<CardStack> const& stacks() const { return m_stacks; }
    CardStack& stack_at_location(int location) { return m_stacks[location]; }
    void add_stack(NonnullRefPtr<CardStack>);
    void mark_intersecting_stacks_dirty(Card const& intersecting_card);

    void dump_layout() const;

protected:
    CardGame();

private:
    virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value) override;

    NonnullRefPtrVector<CardStack> m_stacks;
};

}
