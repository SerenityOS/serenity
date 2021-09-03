/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/FlyString.h>
#include <LibWeb/DOM/CharacterData.h>

namespace Web::DOM {

class Comment final : public CharacterData {
public:
    using WrapperType = Bindings::CommentWrapper;

    explicit Comment(Document&, const String&);
    virtual ~Comment() override;

    virtual FlyString node_name() const override { return "#comment"; }
};

}
