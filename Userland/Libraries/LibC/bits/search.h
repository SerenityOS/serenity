/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// This is technically an implementation detail, but we require this for testing.
// The key always has to be the first struct member.
struct search_tree_node {
    void const* key;
    struct search_tree_node* left;
    struct search_tree_node* right;
};

struct search_tree_node* new_tree_node(void const* key);
void delete_node_recursive(struct search_tree_node* node);
