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

    bool is_moving_cards() const { return !m_moving_cards.is_empty(); }
    NonnullRefPtrVector<Card>& moving_cards() { return m_moving_cards; }
    NonnullRefPtrVector<Card> const& moving_cards() const { return m_moving_cards; }
    Gfx::IntRect moving_cards_bounds() const;
    RefPtr<CardStack> moving_cards_source_stack() const { return m_moving_cards_source_stack; }
    void pick_up_cards_from_stack(CardStack&, Gfx::IntPoint click_location, CardStack::MovementRule);
    RefPtr<CardStack> find_stack_to_drop_on(CardStack::MovementRule) const;
    void drop_cards_on_stack(CardStack&, CardStack::MovementRule);
    void clear_moving_cards();

    void dump_layout() const;

protected:
    CardGame();

private:
    virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value) override;

    NonnullRefPtrVector<CardStack> m_stacks;

    NonnullRefPtrVector<Card> m_moving_cards;
    RefPtr<CardStack> m_moving_cards_source_stack;
};

}
