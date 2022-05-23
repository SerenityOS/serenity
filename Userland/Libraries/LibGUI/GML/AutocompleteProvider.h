/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../AutocompleteProvider.h"

namespace GUI::GML {

class AutocompleteProvider final : public virtual GUI::AutocompleteProvider {
public:
    AutocompleteProvider() = default;
    virtual ~AutocompleteProvider() override = default;

private:
    static bool can_have_declared_layout(StringView class_name)
    {
        return class_name.is_one_of("GUI::Widget", "GUI::Frame");
    }

    virtual void provide_completions(Function<void(Vector<CodeComprehension::AutocompleteResultEntry>)> callback) override;
};

}
