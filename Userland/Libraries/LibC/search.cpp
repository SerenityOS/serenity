/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <bits/search.h>
#include <search.h>

struct search_tree_node* new_tree_node(const void* key)
{
    auto* node = static_cast<struct search_tree_node*>(malloc(sizeof(struct search_tree_node)));

    if (!node)
        return nullptr;

    node->key = key;
    node->left = nullptr;
    node->right = nullptr;

    return node;
}

void delete_node_recursive(struct search_tree_node* node)
{
    if (!node)
        return;

    delete_node_recursive(node->left);
    delete_node_recursive(node->right);

    free(node);
}

extern "C" {

void* tsearch(const void* key, void** rootp, int (*comparator)(const void*, const void*))
{
    if (!rootp)
        return nullptr;

    if (!*rootp) {
        *rootp = new_tree_node(key);
        return *rootp;
    }

    auto node = static_cast<struct search_tree_node*>(*rootp);

    while (node != nullptr) {
        auto comp = comparator(key, node->key);

        if (comp < 0 && node->left) {
            node = node->left;
        } else if (comp < 0 && !node->left) {
            node->left = new_tree_node(key);
            return node->left;
        } else if (comp > 0 && node->right) {
            node = node->right;
        } else if (comp > 0 && !node->right) {
            node->right = new_tree_node(key);
            return node->right;
        } else {
            return node;
        }
    }

    VERIFY_NOT_REACHED();
}

void* tfind(const void* key, void* const* rootp, int (*comparator)(const void*, const void*))
{
    if (!rootp)
        return nullptr;

    auto node = static_cast<struct search_tree_node*>(*rootp);

    while (node != nullptr) {
        auto comp = comparator(key, node->key);

        if (comp < 0)
            node = node->left;
        else if (comp > 0)
            node = node->right;
        else
            return node;
    }

    return nullptr;
}
}
