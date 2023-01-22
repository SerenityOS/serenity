/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibCards/CardStack.h>
#include <LibConfig/Listener.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Frame.h>

namespace Cards {

ErrorOr<NonnullRefPtr<GUI::Action>> make_cards_settings_action(GUI::Window* parent = nullptr);

class CardGame
    : public GUI::Frame
    , public Config::Listener {
public:
    virtual ~CardGame() = default;

    Gfx::Color background_color() const;
    void set_background_color(Gfx::Color);

    NonnullRefPtrVector<CardStack>& stacks() { return m_stacks; }
    NonnullRefPtrVector<CardStack> const& stacks() const { return m_stacks; }
    CardStack& stack_at_location(int location) { return m_stacks[location]; }

    template<class... Args>
    ErrorOr<void> add_stack(Args&&... args)
    {
        auto stack = TRY(try_make_ref_counted<CardStack>(forward<Args>(args)...));
        return m_stacks.try_append(move(stack));
    }
    void mark_intersecting_stacks_dirty(Card const& intersecting_card);

    bool is_moving_cards() const { return !m_moving_cards.is_empty(); }
    NonnullRefPtrVector<Card>& moving_cards() { return m_moving_cards; }
    NonnullRefPtrVector<Card> const& moving_cards() const { return m_moving_cards; }
    Gfx::IntRect moving_cards_bounds() const;
    RefPtr<CardStack> moving_cards_source_stack() const { return m_moving_cards_source_stack; }
    void pick_up_cards_from_stack(CardStack&, Gfx::IntPoint click_location, CardStack::MovementRule);
    RefPtr<CardStack> find_stack_to_drop_on(CardStack::MovementRule) const;
    ErrorOr<void> drop_cards_on_stack(CardStack&, CardStack::MovementRule);
    void clear_moving_cards();

    bool is_previewing_card() const { return !m_previewed_card_stack.is_null(); }
    void preview_card(CardStack&, Gfx::IntPoint click_location);
    void clear_card_preview();

    void dump_layout() const;

protected:
    CardGame();

private:
    virtual void config_string_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value) override;

    NonnullRefPtrVector<CardStack> m_stacks;

    NonnullRefPtrVector<Card> m_moving_cards;
    RefPtr<CardStack> m_moving_cards_source_stack;
    RefPtr<CardStack> m_previewed_card_stack;
};

}
