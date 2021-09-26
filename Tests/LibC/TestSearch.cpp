/*
 * Copyright (c) 2021, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <bits/search.h>
#include <search.h>
#include <string.h>

#define NODE(node) static_cast<struct search_tree_node*>(node)
#define ROOTP(root) reinterpret_cast<void**>(root)
#define COMP(func) reinterpret_cast<int (*)(const void*, const void*)>(func)

TEST_CASE(tsearch)
{
    struct search_tree_node* root = nullptr;
    void* ret;
    const char* key;
    char* search;

    // Try a nullptr rootp.
    ret = tsearch("buggie", nullptr, COMP(strcmp));
    EXPECT_EQ(ret, nullptr);

    // Try creating a new tree.
    key = "5";
    ret = tsearch(key, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root);
    EXPECT_EQ(NODE(ret)->key, key);

    // Insert an element on the left side.
    key = "3";
    ret = tsearch(key, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->left);
    EXPECT_EQ(NODE(ret)->key, key);

    // Insert an element on the right side.
    key = "7";
    ret = tsearch(key, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->right);
    EXPECT_EQ(NODE(ret)->key, key);

    // Add another layer for testing.
    ret = tsearch("2", ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->left->left);
    ret = tsearch("4", ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->left->right);
    ret = tsearch("6", ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->right->left);
    ret = tsearch("8", ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->right->right);

    // Find the root element.
    // strdup ensures that we are using the comparator.
    search = strdup("5");
    ret = tsearch(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root);
    free(search);

    // Find the lowest-level elements.
    search = strdup("2");
    ret = tsearch(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->left->left);
    free(search);

    search = strdup("4");
    ret = tsearch(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->left->right);
    free(search);

    search = strdup("6");
    ret = tsearch(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->right->left);
    free(search);

    search = strdup("8");
    ret = tsearch(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->right->right);
    free(search);

    delete_node_recursive(root);
}

TEST_CASE(tfind)
{
    struct search_tree_node* root = nullptr;
    void* ret;
    char* search;

    // Try a nullptr rootp.
    ret = tfind("buggie", nullptr, COMP(strcmp));
    EXPECT_EQ(ret, nullptr);

    // Search for something that doesn't exist.
    ret = tfind("buggie", ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, nullptr);

    // Construct a tree for testing.
    root = new_tree_node("5");
    root->left = new_tree_node("3");
    root->right = new_tree_node("7");
    root->left->left = new_tree_node("2");
    root->left->right = new_tree_node("4");
    root->right->left = new_tree_node("6");
    root->right->right = new_tree_node("8");

    // Find the root element.
    // strdup ensures that we are using the comparator.
    search = strdup("5");
    ret = tfind(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root);
    free(search);

    // Find the lowest-level elements.
    search = strdup("2");
    ret = tfind(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->left->left);
    free(search);

    search = strdup("4");
    ret = tfind(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->left->right);
    free(search);

    search = strdup("6");
    ret = tfind(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->right->left);
    free(search);

    search = strdup("8");
    ret = tfind(search, ROOTP(&root), COMP(strcmp));
    EXPECT_EQ(ret, root->right->right);
    free(search);

    delete_node_recursive(root);
}
