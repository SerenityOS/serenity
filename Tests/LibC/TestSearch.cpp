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
#define U8(value) static_cast<u8>(value)

static int comparison_function(void const* node1, void const* node2)
{
    return strcmp(reinterpret_cast<char const*>(node1), reinterpret_cast<char const*>(node2));
}

struct twalk_test_entry {
    void const* node;
    VISIT order;
    int depth;
};

#define TWALK_SET_DATA (-2)
#define TWALK_CHECK_END (-3)
#define TWALK_END_MARKER (-4)

TEST_CASE(tsearch)
{
    struct search_tree_node* root = nullptr;
    void* ret;
    char const* key;
    char* search;

    // Try a nullptr rootp.
    ret = tsearch("buggie", nullptr, comparison_function);
    EXPECT_EQ(ret, nullptr);

    // Try creating a new tree.
    key = "5";
    ret = tsearch(key, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root);
    EXPECT_EQ(NODE(ret)->key, key);

    // Insert an element on the left side.
    key = "3";
    ret = tsearch(key, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->left);
    EXPECT_EQ(NODE(ret)->key, key);

    // Insert an element on the right side.
    key = "7";
    ret = tsearch(key, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->right);
    EXPECT_EQ(NODE(ret)->key, key);

    // Add another layer for testing.
    ret = tsearch("2", ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->left->left);
    ret = tsearch("4", ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->left->right);
    ret = tsearch("6", ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->right->left);
    ret = tsearch("8", ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->right->right);

    // Find the root element.
    // strdup ensures that we are using the comparator.
    search = strdup("5");
    ret = tsearch(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root);
    free(search);

    // Find the lowest-level elements.
    search = strdup("2");
    ret = tsearch(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->left->left);
    free(search);

    search = strdup("4");
    ret = tsearch(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->left->right);
    free(search);

    search = strdup("6");
    ret = tsearch(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->right->left);
    free(search);

    search = strdup("8");
    ret = tsearch(search, ROOTP(&root), comparison_function);
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
    ret = tfind("buggie", nullptr, comparison_function);
    EXPECT_EQ(ret, nullptr);

    // Search for something that doesn't exist.
    ret = tfind("buggie", ROOTP(&root), comparison_function);
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
    ret = tfind(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root);
    free(search);

    // Find the lowest-level elements.
    search = strdup("2");
    ret = tfind(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->left->left);
    free(search);

    search = strdup("4");
    ret = tfind(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->left->right);
    free(search);

    search = strdup("6");
    ret = tfind(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->right->left);
    free(search);

    search = strdup("8");
    ret = tfind(search, ROOTP(&root), comparison_function);
    EXPECT_EQ(ret, root->right->right);
    free(search);

    delete_node_recursive(root);
}

void twalk_action(void const* node, VISIT order, int depth);
void twalk_action(void const* node, VISIT order, int depth)
{
    static int count = 0;
    static const struct twalk_test_entry* tests = nullptr;

    // Special case: Set test data.
    if (depth == TWALK_SET_DATA) {
        count = 0;
        tests = static_cast<const struct twalk_test_entry*>(node);
        return;
    }

    // Special case: End signaled by tester.
    if (depth == TWALK_CHECK_END) {
        if (tests[count].depth != TWALK_END_MARKER) {
            FAIL(ByteString::formatted("Expected action (node={:#x}, order={}, depth={}), but twalk ended early.",
                tests[count].node, U8(tests[count].order), tests[count].depth));
        }
        return;
    }

    // Special case: End marker reached.
    if (tests[count].depth == TWALK_END_MARKER) {
        FAIL(ByteString::formatted("Expected end, but twalk sent another action (node={:#x}, order={}, depth={}).",
            node, U8(order), depth));
        return;
    }

    EXPECT_EQ(node, tests[count].node);
    EXPECT_EQ(U8(order), U8(tests[count].order));
    EXPECT_EQ(depth, tests[count].depth);

    count++;
}

TEST_CASE(twalk)
{
    struct search_tree_node* root = nullptr;

    // Try an empty tree.
    struct twalk_test_entry tests1[] = {
        { nullptr, leaf, TWALK_END_MARKER },
    };
    twalk_action(tests1, leaf, TWALK_SET_DATA);
    twalk(nullptr, twalk_action);
    twalk_action(nullptr, leaf, TWALK_CHECK_END);

    // Try a single node.
    root = new_tree_node("5");
    struct twalk_test_entry tests2[] = {
        { root, leaf, 0 },
        { nullptr, leaf, TWALK_END_MARKER },
    };
    twalk_action(tests2, leaf, TWALK_SET_DATA);
    twalk(root, twalk_action);
    twalk_action(nullptr, leaf, TWALK_CHECK_END);

    // Try two layers of nodes.
    root->left = new_tree_node("3");
    root->right = new_tree_node("7");
    struct twalk_test_entry tests3[] = {
        { root, preorder, 0 },
        { root->left, leaf, 1 },
        { root, postorder, 0 },
        { root->right, leaf, 1 },
        { root, endorder, 0 },
        { nullptr, leaf, TWALK_END_MARKER },
    };
    twalk_action(tests3, leaf, TWALK_SET_DATA);
    twalk(root, twalk_action);
    twalk_action(nullptr, leaf, TWALK_CHECK_END);

    // Try three layers of nodes.
    root->left->left = new_tree_node("2");
    root->left->right = new_tree_node("4");
    root->right->left = new_tree_node("6");
    root->right->right = new_tree_node("8");
    struct twalk_test_entry tests4[] = {
        { root, preorder, 0 },
        { root->left, preorder, 1 },
        { root->left->left, leaf, 2 },
        { root->left, postorder, 1 },
        { root->left->right, leaf, 2 },
        { root->left, endorder, 1 },
        { root, postorder, 0 },
        { root->right, preorder, 1 },
        { root->right->left, leaf, 2 },
        { root->right, postorder, 1 },
        { root->right->right, leaf, 2 },
        { root->right, endorder, 1 },
        { root, endorder, 0 },
        { nullptr, leaf, TWALK_END_MARKER },
    };
    twalk_action(tests4, leaf, TWALK_SET_DATA);
    twalk(root, twalk_action);
    twalk_action(nullptr, leaf, TWALK_CHECK_END);

    delete_node_recursive(root);
}
