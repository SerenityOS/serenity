/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/DeprecatedString.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>

TEST_CASE(destroy_self_owning_object)
{
    // This test is a little convoluted because SelfOwning can't own itself
    // through a NonnullOwnPtr directly. We have to use an intermediate object ("Inner").
    struct SelfOwning {
        SelfOwning()
        {
        }
        struct Inner {
            explicit Inner(NonnullOwnPtr<SelfOwning> self)
                : self(move(self))
            {
            }
            NonnullOwnPtr<SelfOwning> self;
        };
        OwnPtr<Inner> inner;
    };
    OwnPtr<SelfOwning> object = make<SelfOwning>();
    auto* object_ptr = object.ptr();
    object_ptr->inner = make<SelfOwning::Inner>(object.release_nonnull());
    object_ptr->inner = nullptr;
}
