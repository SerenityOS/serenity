/*
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/FilePermissionsMask.h>
#include <LibTest/TestCase.h>

TEST_CASE(file_permission_mask_from_symbolic_notation)
{
    auto mask = Core::FilePermissionsMask::from_symbolic_notation(""sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0);
    EXPECT_EQ(mask.value().write_mask(), 0);
    EXPECT_EQ(mask.value().apply(0), 0);
    EXPECT_EQ(mask.value().apply(0664), 0664);

    mask = Core::FilePermissionsMask::from_symbolic_notation("u+rwx"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0);
    EXPECT_EQ(mask.value().write_mask(), 0700);
    EXPECT_EQ(mask.value().apply(0), 0700);
    EXPECT_EQ(mask.value().apply(0664), 0764);

    mask = Core::FilePermissionsMask::from_symbolic_notation("g+rwx"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0);
    EXPECT_EQ(mask.value().write_mask(), 0070);
    EXPECT_EQ(mask.value().apply(0), 0070);
    EXPECT_EQ(mask.value().apply(0664), 0674);

    mask = Core::FilePermissionsMask::from_symbolic_notation("o+rwx"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0);
    EXPECT_EQ(mask.value().write_mask(), 0007);
    EXPECT_EQ(mask.value().apply(0), 0007);
    EXPECT_EQ(mask.value().apply(0664), 0667);

    mask = Core::FilePermissionsMask::from_symbolic_notation("a=rx"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0777);
    EXPECT_EQ(mask.value().write_mask(), 0555);
    EXPECT_EQ(mask.value().apply(0), 0555);
    EXPECT_EQ(mask.value().apply(0664), 0555);

    mask = Core::FilePermissionsMask::from_symbolic_notation("ugo=rx"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0777);
    EXPECT_EQ(mask.value().write_mask(), 0555);
    EXPECT_EQ(mask.value().apply(0), 0555);
    EXPECT_EQ(mask.value().apply(0664), 0555);

    mask = Core::FilePermissionsMask::from_symbolic_notation("u+rw,g=rx,o-rwx"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0077);
    EXPECT_EQ(mask.value().write_mask(), 0650);
    EXPECT_EQ(mask.value().apply(0), 0650);
    EXPECT_EQ(mask.value().apply(0177), 0750);

    mask = Core::FilePermissionsMask::from_symbolic_notation("+r"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0);
    EXPECT_EQ(mask.value().write_mask(), 0444);
    EXPECT_EQ(mask.value().apply(0), 0444);
    EXPECT_EQ(mask.value().apply(0123), 0567);

    mask = Core::FilePermissionsMask::from_symbolic_notation("=rx"sv);
    EXPECT(!mask.is_error());
    EXPECT_EQ(mask.value().clear_mask(), 0777);
    EXPECT_EQ(mask.value().write_mask(), 0555);
    EXPECT_EQ(mask.value().apply(0), 0555);
    EXPECT_EQ(mask.value().apply(0664), 0555);

    mask = Core::FilePermissionsMask::from_symbolic_notation("z+rw"sv);
    EXPECT(mask.is_error());
    EXPECT(mask.error().string_literal().starts_with("invalid class"));

    mask = Core::FilePermissionsMask::from_symbolic_notation("u*rw"sv);
    EXPECT(mask.is_error());
    EXPECT(mask.error().string_literal().starts_with("invalid operation"));

    mask = Core::FilePermissionsMask::from_symbolic_notation("u+rz"sv);
    EXPECT(mask.is_error());
    EXPECT(mask.error().string_literal().starts_with("invalid symbolic permission"));

    mask = Core::FilePermissionsMask::from_symbolic_notation("u+rw;g+rw"sv);
    EXPECT(mask.is_error());
    EXPECT(mask.error().string_literal().starts_with("invalid symbolic permission"));
}

TEST_CASE(file_permission_mask_parse)
{
    auto numeric_mask = Core::FilePermissionsMask::parse("750"sv);
    auto symbolic_mask = Core::FilePermissionsMask::parse("u=rwx,g=rx,o-rwx"sv);

    EXPECT_EQ(numeric_mask.value().apply(0), 0750);
    EXPECT_EQ(symbolic_mask.value().apply(0), 0750);

    EXPECT_EQ(numeric_mask.value().clear_mask(), symbolic_mask.value().clear_mask());
    EXPECT_EQ(numeric_mask.value().write_mask(), symbolic_mask.value().write_mask());

    auto mask = Core::FilePermissionsMask::parse("888");
    EXPECT(mask.is_error());

    mask = Core::FilePermissionsMask::parse("z+rw");
    EXPECT(mask.is_error());
}
