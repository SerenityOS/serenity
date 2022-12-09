/*
 * Copyright (c) 2022, Eli Youngs <eli.m.youngs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/HashMap.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

enum NodeStatus {
    NotSeen,
    Seen,
    Prioritized,
};

struct Node {
    StringView name;
    HashTable<StringView> ancestors;
    NodeStatus status;

    unsigned hash() const { return name.hash(); }
};

using NodeMap = HashMap<StringView, Node>;
using NodeStack = Vector<Node&>;

static void handle_cycle(NodeStack& stack, Node& duplicated_node)
{
    // Report on a cycle by moving down the stack of dependencies, logging every node
    // between the implicit top of the stack (represented by duplicate_node) and that
    // node's first appearance.

    warnln("tsort: The following nodes form a cycle");

    for (auto it = stack.rbegin(); it != stack.rend(); ++it) {
        auto node = *it;
        node.status = NodeStatus::NotSeen;
        warnln("tsort: {}", node.name);
        if (node.name == duplicated_node.name)
            return;
    }
}

static void prioritize_nodes(Node& start, NodeMap const& node_map)
{
    // Prioritize (topologically sort) a subset of a directed graph using a depth first
    // search. The "deepest" nodes are the earliest ancestors of all other nodes and
    // have no dependencies. To avoid a stack overflow when processing deep dependency
    // chains, this function does not call itself recursively. Instead, the recursive
    // algorithm is implemented on a heap-allocated stack.

    static NodeStack stack;

    assert(stack.is_empty());
    stack.append(start);

    while (!stack.is_empty()) {
        auto node = stack.last();

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
            auto next_ancestor_name = node.ancestors.pop();
            auto next_ancestor = node_map.get(next_ancestor_name).release_value();
            if (next_ancestor.status == NodeStatus::Seen)
                // If the same node is seen multiple times, this represents a cycle in
                // the graph. To avoid an infinite loop, the duplicate node is not added
                // to the stack a second time. Instead, the edge is deliberately ignored,
                // and the topological sort proceeds as though the cycle did not exist.
                handle_cycle(stack, next_ancestor);
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

    auto file = TRY(Core::Stream::File::open_file_or_standard_stream(path, Core::Stream::OpenMode::Read));
    auto input_bytes = TRY(file->read_all());
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

        auto& descendent_node = node_map.ensure(descendent_name, [&]() { return Node { descendent_name, HashTable<StringView> {}, NodeStatus::NotSeen }; });
        if (descendent_name != ancestor_name) {
            node_map.ensure(ancestor_name, [&]() { return Node { ancestor_name, HashTable<StringView> {}, NodeStatus::NotSeen }; });
            descendent_node.ancestors.set(ancestor_name);
        }
    }

    // Each node must be checked individually, since any node could be disconnected from
    // the rest of the graph.
    for (auto& entry : node_map) {
        auto& node_to_prioritize = entry.value;
        if (node_to_prioritize.status == NodeStatus::NotSeen)
            prioritize_nodes(node_to_prioritize, node_map);
    }

    return 0;
}
