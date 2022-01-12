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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tsearch.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tfind.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tdelete.html
void* tdelete(const void*, void**, int (*)(const void*, const void*))
{
    dbgln("FIXME: Implement tdelete()");
    return nullptr;
}

static void twalk_internal(const struct search_tree_node* node, void (*action)(const void*, VISIT, int), int depth)
{
    if (!node)
        return;

    if (!node->right && !node->left) {
        action(node, leaf, depth);
        return;
    }

    action(node, preorder, depth);
    twalk_internal(node->left, action, depth + 1);
    action(node, postorder, depth);
    twalk_internal(node->right, action, depth + 1);
    action(node, endorder, depth);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/twalk.html
void twalk(const void* rootp, void (*action)(const void*, VISIT, int))
{
    auto node = static_cast<const struct search_tree_node*>(rootp);

    twalk_internal(node, action, 0);
}
}
