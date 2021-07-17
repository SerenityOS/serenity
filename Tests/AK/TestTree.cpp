/*
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Tree.h>

TEST_CASE(construct)
{
    EXPECT(Tree<int>().root().is_empty());
    EXPECT(Tree<int>().root().num_children() == 0);

    Tree<int> tree;

    tree.root().add_child(10);
    tree.root().add_child(15);
    tree.root().find(10)->add_child(20);

    Tree<int> tree2(tree); // Copy
    EXPECT_EQ(tree.root().num_children(), 2u);
    EXPECT_EQ(tree2.root().num_children(), 2u);
    EXPECT(tree.root().find(10) != nullptr);
    EXPECT(tree2.root().find(10) != nullptr);
    EXPECT_EQ(tree.root().find(10)->num_children(), 1u);
    EXPECT_EQ(tree2.root().find(10)->num_children(), 1u);

    Tree<int> tree3(move(tree2)); // Move
    EXPECT_EQ(tree3.root().num_children(), 2u);
    EXPECT(tree3.root().find(10) != nullptr);
    EXPECT_EQ(tree3.root().find(10)->num_children(), 1u);
}

TEST_CASE(ints)
{
    Tree<int> ints;

    ints.root().set(5);
    ints.root().add_child(10);
    ints.root().add_child(8);
    ints.root().add_child(6);

    ints.root().find(6)->add_child(3);
    ints.root().find(6)->add_child(4);
    ints.root().find(10)->add_child(5);

    EXPECT_EQ(ints.root().find(6)->num_children(), 2u);
    EXPECT_EQ(ints.root().find(8)->num_children(), 0u);
    EXPECT_EQ(ints.root().find(10)->num_children(), 1u);

    EXPECT_EQ(ints.root().child_at(0)->value(), 10);
    EXPECT_EQ(ints.root().child_at(1)->value(), 8);

    EXPECT_EQ(ints.root().find(10)->value(), 10);
    EXPECT_EQ(ints.root().size(), 7u);

    ints.root().sort([&](auto& a, auto& b) { return a < b; });

    EXPECT_EQ(ints.root().child_at(0)->value(), 6);
    EXPECT_EQ(ints.root().child_at(1)->value(), 8);

    ints.root().clear();

    EXPECT_EQ(ints.root().num_children(), 0u);
    EXPECT_EQ(ints.root().size(), 1u);
}

TEST_CASE(strings)
{
    Tree<String> strings;

    strings.root().set("abc");
    strings.root().add_child("def");
    strings.root().add_child("ghi");
    strings.root().add_child("jkl");

    strings.root().find("def")->add_child("mno");
    strings.root().find("def")->add_child("pqr");
    strings.root().find("jkl")->add_child("stu");

    EXPECT_EQ(strings.root().find("def")->num_children(), 2u);
    EXPECT_EQ(strings.root().find("ghi")->num_children(), 0u);
    EXPECT_EQ(strings.root().find("jkl")->num_children(), 1u);

    EXPECT_EQ(strings.root().child_at(1)->value(), "ghi");

    EXPECT_EQ(strings.root().find("jkl")->value(), "jkl");

    strings.root().clear();

    EXPECT_EQ(strings.root().num_children(), 0u);
}

TEST_CASE(equals)
{
    Tree<int> ints;

    ints.root().set(5);
    ints.root().add_child(6);
    ints.root().add_child(8);
    ints.root().add_child(10);

    ints.root().find(6)->add_child(3);
    ints.root().find(6)->add_child(4);
    ints.root().find(10)->add_child(5);

    // Comparing equality on a copy
    Tree<int> ints2(ints);
    EXPECT(ints == ints2);

    // Shouldn't be equal anymore
    ints.root().find(6)->set(12);
    EXPECT(ints != ints2);
}

TEST_CASE(search)
{
    Tree<int> ints;

    ints.root().set(1);
    ints.root().add_child(2);
    ints.root().add_child(3);
    ints.root().add_child(4);
    ints.root().add_child(5);
    ints.root().add_child(6);

    ints.root().find(4)->add_child(7);
    ints.root().find(4)->add_child(8);
    ints.root().find(4)->add_child(9);
    ints.root().find(4)->add_child(10);
    ints.root().find(4)->add_child(11);
    ints.root().find(4)->add_child(12);
    ints.root().find(4)->add_child(13);

    ints.root().find(4)->find(12)->add_child(14);
    ints.root().find(4)->find(12)->add_child(15);

    EXPECT_EQ(ints.root().search(12)->num_children(), 2u);
    EXPECT_EQ(ints.root().search(4)->num_children(), 7u);
    EXPECT_EQ(ints.root().search(1)->num_children(), 5u);
}
