/*
 * Copyright (c) 2022, Eli Youngs <eli.m.youngs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/HashMap.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

enum NodeStatus {
    NotSeen,
    Seen,
    Prioritized,
};

struct Node {
    StringView name;
    OrderedHashTable<StringView> ancestors;
    NodeStatus status;
};

using NodeMap = OrderedHashMap<StringView, Node>;
using NodeStack = Vector<Node&>;

static void handle_cycle(NodeStack& stack, Node& duplicated_node, bool quiet)
{
    // Report on a cycle by moving down the stack of dependencies, logging every node
    // between the implicit top of the stack (represented by duplicate_node) and that
    // node's first appearance.

    if (!quiet)
        warnln("tsort: The following nodes form a cycle");

    for (auto it = stack.rbegin(); it != stack.rend(); ++it) {
        auto node = *it;
        node.status = NodeStatus::NotSeen;
        if (!quiet)
            warnln("tsort: {}", node.name);
        if (node.name == duplicated_node.name)
            return;
    }
}

static void prioritize_nodes(Node& start, NodeMap& node_map, NodeStack& stack, bool quiet)
{
    // Prioritize (topologically sort) a subset of a directed graph using a depth first
    // search. The "deepest" nodes are the earliest ancestors of all other nodes and
    // have no dependencies. To avoid a stack overflow when processing deep dependency
    // chains, this function does not call itself recursively. Instead, the recursive
    // algorithm is implemented on a provided stack.

    VERIFY(stack.is_empty());
    stack.append(start);

    while (!stack.is_empty()) {
        auto& node = stack.last();

        // If a node has already been prioritized, it can be ignored.
        if (node.status == NodeStatus::Prioritized) {
            stack.take_last();
            continue;
        }

        // Keep track of which nodes have been seen to detect cycles.
        node.status = NodeStatus::Seen;

        if (node.ancestors.is_empty()) {
            // If a node has no remaining ancestors (dependencies), it either never had
            // ancestors, or its ancestors have already been prioritized. In either case,
            // this is now the deepest un-prioritized node, which makes it the next
            // highest priority.
            node.status = NodeStatus::Prioritized;
            outln("{}", stack.take_last().name);
        } else {
            auto next_ancestor_name = node.ancestors.take_last();
            auto& next_ancestor = node_map.get(next_ancestor_name).release_value();
            if (next_ancestor.status == NodeStatus::Seen)
                // If the same node is seen multiple times, this represents a cycle in
                // the graph. To avoid an infinite loop, the duplicate node is not added
                // to the stack a second time. Instead, the edge is deliberately ignored,
                // and the topological sort proceeds as though the cycle did not exist.
                handle_cycle(stack, next_ancestor, quiet);
            else
                // Recursively prioritize all ancestors.
                stack.append(next_ancestor);
        }
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView path;
    bool quiet = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to file", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(quiet, "Suppress warnings about cycles", "quiet", 'q');
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));
    auto input_bytes = TRY(file->read_until_eof());
    auto inputs = StringView(input_bytes).split_view_if(is_ascii_space);

    if (inputs.is_empty())
        return 0;

    if (inputs.size() % 2 != 0) {
        warnln("tsort: the number of inputs must be even");
        return 1;
    }

    NodeMap node_map;

    // Each pair of inputs (e.g. "a b") represents an edge of a directed acyclic graph.
    // If the same input is repeated (e.g. "a a"), this defines a single node with no
    // connection to any other nodes. Otherwise, the first input is interpreted as an
    // ancestor of the second.
    for (size_t i = 0; i < inputs.size(); i += 2) {
        auto ancestor_name = inputs[i];
        auto descendent_name = inputs[i + 1];

        TRY(node_map.try_ensure(descendent_name, [&]() { return Node { descendent_name, OrderedHashTable<StringView> {}, NodeStatus::NotSeen }; }));
        if (descendent_name != ancestor_name) {
            TRY(node_map.try_ensure(ancestor_name, [&]() { return Node { ancestor_name, OrderedHashTable<StringView> {}, NodeStatus::NotSeen }; }));
            // Creating the ancestor_node might cause the node_map to expand, re-hash
            // its contents, and invalidate existing references to its values. To handle
            // this, we always get a new reference to the descendent_node.
            auto& descendent_node = node_map.get(descendent_name).release_value();
            TRY(descendent_node.ancestors.try_set(ancestor_name));
        }
    }

    // Each node must be checked individually, since any node could be disconnected from
    // the rest of the graph.
    NodeStack stack;
    for (auto& entry : node_map) {
        auto& node_to_prioritize = entry.value;
        if (node_to_prioritize.status == NodeStatus::NotSeen)
            prioritize_nodes(node_to_prioritize, node_map, stack, quiet);
    }

    return 0;
}
