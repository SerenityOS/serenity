/*
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/FilePermissionsMask.h>
#include <LibTest/TestCase.h>

TEST_CASE(file_permission_mask_from_symbolic_notation)
{
    auto mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation(""sv));
    EXPECT_EQ(mask.clear_mask(), 0);
    EXPECT_EQ(mask.write_mask(), 0);
    EXPECT_EQ(mask.apply(0), 0);
    EXPECT_EQ(mask.apply(0664), 0664);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("u+rwx"sv));
    EXPECT_EQ(mask.clear_mask(), 0);
    EXPECT_EQ(mask.write_mask(), 0700);
    EXPECT_EQ(mask.apply(0), 0700);
    EXPECT_EQ(mask.apply(0664), 0764);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("g+rwx"sv));
    EXPECT_EQ(mask.clear_mask(), 0);
    EXPECT_EQ(mask.write_mask(), 0070);
    EXPECT_EQ(mask.apply(0), 0070);
    EXPECT_EQ(mask.apply(0664), 0674);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("o+rwx"sv));
    EXPECT_EQ(mask.clear_mask(), 0);
    EXPECT_EQ(mask.write_mask(), 0007);
    EXPECT_EQ(mask.apply(0), 0007);
    EXPECT_EQ(mask.apply(0664), 0667);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("a=rx"sv));
    EXPECT_EQ(mask.clear_mask(), 0777);
    EXPECT_EQ(mask.write_mask(), 0555);
    EXPECT_EQ(mask.apply(0), 0555);
    EXPECT_EQ(mask.apply(0664), 0555);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("ugo=rx"sv));
    EXPECT_EQ(mask.clear_mask(), 0777);
    EXPECT_EQ(mask.write_mask(), 0555);
    EXPECT_EQ(mask.apply(0), 0555);
    EXPECT_EQ(mask.apply(0664), 0555);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("u+rw,g=rx,o-rwx"sv));
    EXPECT_EQ(mask.clear_mask(), 0077);
    EXPECT_EQ(mask.write_mask(), 0650);
    EXPECT_EQ(mask.apply(0), 0650);
    EXPECT_EQ(mask.apply(0177), 0750);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("+r"sv));
    EXPECT_EQ(mask.clear_mask(), 0);
    EXPECT_EQ(mask.write_mask(), 0444);
    EXPECT_EQ(mask.apply(0), 0444);
    EXPECT_EQ(mask.apply(0123), 0567);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("=rx"sv));
    EXPECT_EQ(mask.clear_mask(), 0777);
    EXPECT_EQ(mask.write_mask(), 0555);
    EXPECT_EQ(mask.apply(0), 0555);
    EXPECT_EQ(mask.apply(0664), 0555);

    mask = TRY_OR_FAIL(Core::FilePermissionsMask::from_symbolic_notation("a+X"sv));
    EXPECT_EQ(mask.clear_mask(), 0);
    EXPECT_EQ(mask.write_mask(), 0);
    EXPECT_EQ(mask.directory_or_executable_mask().clear_mask(), 0);
    EXPECT_EQ(mask.directory_or_executable_mask().write_mask(), 0111);
    EXPECT_EQ(mask.apply(0), 0);
    EXPECT_EQ(mask.apply(0100), 0111);
    EXPECT_EQ(mask.apply(S_IFDIR | 0), S_IFDIR | 0111);

    auto mask_error = Core::FilePermissionsMask::from_symbolic_notation("z+rw"sv);
    EXPECT(mask_error.is_error());
    EXPECT(mask_error.error().string_literal().starts_with("invalid class"sv));

    mask_error = Core::FilePermissionsMask::from_symbolic_notation("u*rw"sv);
    EXPECT(mask_error.is_error());
    EXPECT(mask_error.error().string_literal().starts_with("invalid operation"sv));

    mask_error = Core::FilePermissionsMask::from_symbolic_notation("u+rz"sv);
    EXPECT(mask_error.is_error());
    EXPECT(mask_error.error().string_literal().starts_with("invalid symbolic permission"sv));

    mask_error = Core::FilePermissionsMask::from_symbolic_notation("u+rw;g+rw"sv);
    EXPECT(mask_error.is_error());
    EXPECT(mask_error.error().string_literal().starts_with("invalid symbolic permission"sv));
}

TEST_CASE(file_permission_mask_parse)
{
    auto numeric_mask = TRY_OR_FAIL(Core::FilePermissionsMask::parse("750"sv));
    auto symbolic_mask = TRY_OR_FAIL(Core::FilePermissionsMask::parse("u=rwx,g=rx,o-rwx"sv));

    EXPECT_EQ(numeric_mask.apply(0), 0750);
    EXPECT_EQ(symbolic_mask.apply(0), 0750);

    EXPECT_EQ(numeric_mask.clear_mask(), symbolic_mask.clear_mask());
    EXPECT_EQ(numeric_mask.write_mask(), symbolic_mask.write_mask());

    auto mask = Core::FilePermissionsMask::parse("888"sv);
    EXPECT(mask.is_error());

    mask = Core::FilePermissionsMask::parse("z+rw"sv);
    EXPECT(mask.is_error());
}

TEST_CASE(numeric_mask_special_bits)
{
    {
        auto mask = TRY_OR_FAIL(Core::FilePermissionsMask::parse("750"sv));
        EXPECT_EQ(mask.apply(07000), 07750);
    }

    {
        auto mask = TRY_OR_FAIL(Core::FilePermissionsMask::parse("7750"sv));
        EXPECT_EQ(mask.apply(0), 07750);
    }

    {
        auto mask = TRY_OR_FAIL(Core::FilePermissionsMask::parse("0750"sv));
        EXPECT_EQ(mask.apply(07000), 0750);
    }
}
