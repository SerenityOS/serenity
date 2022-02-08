/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../AutocompleteProvider.h"

namespace GUI::GML {

class AutocompleteProvider final : public virtual GUI::AutocompleteProvider {
public:
    AutocompleteProvider() { }
    virtual ~AutocompleteProvider() override { }

private:
    static bool can_have_declared_layout(StringView class_name)
    {
        return class_name.is_one_of("GUI::Widget", "GUI::Frame");
    }

    virtual void provide_completions(Function<void(Vector<Entry>)> callback) override;
};

}
