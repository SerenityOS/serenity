/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Widget.h>

class Pattern;

class PatternWidget final : public GUI::Widget {
    C_OBJECT(PatternWidget)
public:
    virtual ~PatternWidget() override;

    Function<void(Pattern*)> on_pattern_selection;

    template<typename Callback>
    void for_each_pattern(Callback callback)
    {
        for (auto& pattern : m_patterns)
            callback(pattern);
    }

private:
    friend class PatternButton;

    void setup_patterns();

    explicit PatternWidget();
    RefPtr<GUI::Toolbar> m_toolbar;
    GUI::ActionGroup m_action_group;
    NonnullOwnPtrVector<Pattern> m_patterns;
};
