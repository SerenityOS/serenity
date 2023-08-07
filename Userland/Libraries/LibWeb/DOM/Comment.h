/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <LibWeb/DOM/CharacterData.h>

namespace Web::DOM {

class Comment final : public CharacterData {
    WEB_PLATFORM_OBJECT(Comment, CharacterData);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Comment>> construct_impl(JS::Realm&, DeprecatedString const& data);
    virtual ~Comment() override = default;

    virtual DeprecatedFlyString node_name() const override { return "#comment"; }

private:
    Comment(Document&, DeprecatedString const&);

    virtual void initialize(JS::Realm&) override;
};

template<>
inline bool Node::fast_is<Comment>() const { return is_comment(); }

}
