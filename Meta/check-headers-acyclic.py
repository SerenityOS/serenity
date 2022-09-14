#!/usr/bin/env python3

import os
import re
import subprocess
import sys

ANY_INCLUDE_RE = re.compile('^.*#[ \t]*include(?![A-Za-z]).*\n?$')
SYSTEM_INCLUDE_RE = re.compile('^.*#[ \t]*include *<([^>]+)> *(?:/[*/].*)?\n?$')
LOCAL_INCLUDE_RE = re.compile('^.*#[ \t]*include *"([^>]+)" *(?:/[*/].*)?\n?$')
SHOW_TIMING = False

# These are already very extensive. The only thing missing are the Toolchain includes.
INCLUDE_LOOKUP_PATHS_FORMAT = [
    '',  # For things that specify the full path, e.g. <AK/String.h>
    'Userland/Libraries/',
    'Userland/Libraries/LibC/',
    'Userland/Libraries/LibCrypt/',
    'Userland/Libraries/LibSystem/',
    'Userland/Services/',
    'Userland/',
    'Build/{}/',
    'Build/{}/Userland/Services/',
    'Build/{}/Userland/Libraries/',
    'Build/{}/Userland/',
]

# These system headers are too hard to find, and they're irrelevant for the purposes of this script anyway.
KNOWN_GOOD_SYSTEM_HEADERS = {
    'bits/endian.h',
    'cxxabi.h',
    'initializer_list',
    'libkern/OSByteOrder.h',
    'machine/endian.h',
    'malloc/malloc.h',
    'new',
    'sanitizer/asan_interface.h',
    'stdbool.h',
    'sys/random.h',
    'typeinfo',
    'utility',
    'xmmintrin.h',
}


def get_headers_here():
    # FIXME: Really test *ALL* other headers!
    result = subprocess.run(['git', 'ls-files', '*.h'], check=True, capture_output=True, text=True)
    assert result.stderr == ''
    output = result.stdout.split('\n')
    assert output[-1] == ''  # Trailing newline
    assert len(output) > 500, 'There should be well over a thousand headers, not only {}?!'.format(len(output))
    return output[:-1]


def find_system_include(arch, include_filename, magic=[]):
    if not magic:
        magic.extend([fmt.format(arch) for fmt in INCLUDE_LOOKUP_PATHS_FORMAT])

    for possible_prefix in magic:
        possible_full_filename = possible_prefix + include_filename
        if os.path.exists(possible_full_filename):
            return possible_full_filename
    return None


def resolve_local_path(filename, relative_path):
    filename = os.path.dirname(filename)

    # Collapse '..' into the path of `filename`.
    # Note that this is currently not used, and doesn't work in some edgecases.
    while relative_path.startswith('../') and filename:
        relative_path = relative_path[len('../'):]
        filename = os.path.dirname(filename)

    if not filename:
        return relative_path

    return os.path.join(filename, relative_path)


def get_dependencies(arch, filename):
    includes = []
    # FIXME: Use the readlines() iterator, that should do much less copying, and can also adapt to disk speed I guess
    with open(filename, 'r') as fp:
        for line_number, line in enumerate(fp):
            # Ignore any includes after the 80th line.
            # Scanning for #include preprocessor commands makes up the majority of the running time of this script.
            # Only two headers have any includes beyond this point:
            # - AK/Platform.h, which will be irrelevant for the purposes of this script, as it includes system-headers.
            # - Userland/Libraries/LibWeb/Bindings/WindowObjectHelper.h, which is an abomination, but whose includes
            #   mostly have the same dependencies anyway, so if any cycles ever arise we will catch them anyway.
            # So by using this shortcut, we don't lose much. And we gain a lot:
            # - At the time of writing, all headers together have 226236 lines that would need to be checked.
            #   Running a string test 226236 times takes a bunch of time.
            # - Ignoring everything after the 80th line cuts this down to 129758 lines
            # For reference, the #include on the highest-numbered-line except the above files is in StdLibExtras.h,
            # line 59 at the time of writing, and it is `#include <utility>`, so arguably also not interesting.
            # Thus, with 80 we have more than sufficient safety margin to catch all cycles that we might introduce,
            # and can halve the amount of data we even have to consider.
            if line_number > 80:
                break
            # Do a string search first.
            # This is faster than even a compiled regex. Timing showed that the compiled regex test makes up the
            # majority of the time of this script on a machine with fast file access. Doing this pre-check speeds up
            # the parsing step from about 34.54 µs per file on avg to about 25.55 µs per file on avg.
            if 'include' not in line:
                continue
            # So the current line contains the sequence "include", but it could still be in a comment or a function
            # name or otherwise unrelated. Is this an include at all?
            if not ANY_INCLUDE_RE.fullmatch(line):
                continue
            system_match = SYSTEM_INCLUDE_RE.fullmatch(line)
            local_match = LOCAL_INCLUDE_RE.fullmatch(line)
            if system_match:
                include_string = system_match.groups()[0]
                if include_string in KNOWN_GOOD_SYSTEM_HEADERS:
                    # This include line is irrelevant anyway.
                    # Discard it immediately.
                    continue
                include_filename = find_system_include(arch, include_string)
                if include_filename is None:
                    print(f'WARNING: Could not find include <{include_string}> from {filename}!', file=sys.stderr)
                    continue
            elif local_match:
                include_string = local_match.groups()[0]
                include_filename = resolve_local_path(filename, local_match.groups()[0])
                if not os.path.exists(include_filename):
                    print(f'WARNING: Local include "{include_string}" from {filename} does not exist as a file!',
                          file=sys.stderr)
                    continue
            else:
                print(f'WARNING: Could not parse include line in {filename}: >>>{line}<<<', file=sys.stderr)
                continue
            includes.append(include_filename)

    return includes


class TarjanNode:
    def __init__(self, filename):
        self.filename = filename
        self.index = None
        self.lowlink = None
        self.onStack = False


class TarjanState:
    def __init__(self, deps):
        self.deps = deps
        self.stack = []
        self.next_index = 0  # Called 'index' on Wikipedia
        self.lowlink = None
        self.on_stack = False
        self.nodes = dict()  # Filename to TarjanNode
        self.components = []

    def neighbors(self, node):
        for neighbor_name in self.deps[node.filename]:
            neighbor = self.nodes.get(neighbor_name)
            if neighbor is None:
                # neighbor_name either:
                # - doesn't exist (then we already complained),
                # - exists but was not scanned (then it doesn't matter),
                # - or was scanned but contains no includes (then it can be safely ignored).
                continue
            yield neighbor

    def push(self, node):
        assert node.index is None
        node.index = self.next_index
        node.lowlink = self.next_index
        self.next_index += 1
        self.stack.append(node)
        node.on_stack = True

    def pop(self, node):
        # If v is a root node, pop the stack and generate an SCC
        if node.lowlink != node.index:
            return
        # Start a new strongly connected component
        component = set()
        while True:
            subnode = self.stack.pop()
            subnode.on_stack = False
            component.add(subnode.filename)
            if subnode == node:
                break
        if len(component) > 1:
            self.components.append(component)


# This implements Tarjan's strongly connected components algorithm. Heavily inspired by:
# https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm#The_algorithm_in_pseudocode
# Arguably, this code might be licensed under CC-BY-SA-3.0, but this license is therefore satisfied.
def tarjan_recurse(state, node):
    state.push(node)

    # Consider successors of node
    for successor in state.neighbors(node):
        if successor.index is None:
            # Successor has not yet been visited; recurse on it
            # Theoretically, python runs into trouble at pretty much exactly 1000 stack frames.
            # Practically, if any inclusion chain comes anywhere near this number, we are doomed anyway.
            tarjan_recurse(state, successor)
            node.lowlink = min(node.lowlink, successor.lowlink)
        elif successor.on_stack:
            # Successor is in stack and hence in the current SCC
            # Note: The next line may look odd - but is correct.
            node.lowlink = min(node.lowlink, successor.index)

    state.pop(node)


def find_nontrivial_connected_components(deps):
    state = TarjanState(deps)
    for key, value_list in deps.items():
        if not value_list:
            # This header does not include anything, therefore it cannot be part of a cycle anyway.
            continue
        assert key not in state.nodes
        state.nodes[key] = TarjanNode(key)

    # Tarjan's strongly connected components algorithm starts here.
    for node in state.nodes.values():
        if node.index is None:
            tarjan_recurse(state, node)

    return state.components


def print_components(components, deps):
    print('digraph G {')
    for component in components:
        any_filename = list(component)[0]
        print(f'  // Cycle or component around {any_filename}')
        for filename in component:
            for neighbor in deps[filename]:
                if neighbor not in component:
                    continue
                print(f'  "{filename}" -> "{neighbor}";')
    print('}')


def run(arch):
    if SHOW_TIMING:
        import time
        time_begin = time.time_ns()
    headers_list = get_headers_here()
    if SHOW_TIMING:
        time_listed = time.time_ns()
    deps = {header_name: get_dependencies(arch, header_name) for header_name in headers_list}
    if SHOW_TIMING:
        time_deps_read = time.time_ns()
    nontrivial_connected_components = find_nontrivial_connected_components(deps)
    if SHOW_TIMING:
        time_resolved = time.time_ns()
        print(f'// Took {(time_listed - time_begin) / 1000000} + {(time_deps_read - time_listed) / 1000000} + '
              f'{(time_resolved - time_deps_read) / 1000000} = {(time_resolved - time_begin) / 1000000} '
              ' milliseconds to compute everything')
        print(f'// The second number splits into reading and parsing phases for each of the {len(deps)} headers.')

    if not nontrivial_connected_components:
        # Intentionally not to stderr
        num_deps = sum(len(d) for d in deps.values())
        print(f'// No cycles detected in {len(headers_list)} files ({num_deps} successfully-resolved includes)')
        return

    num_cyclic_files = sum(len(cc) for cc in nontrivial_connected_components)
    print(f'WARNING: Found cycles involving {num_cyclic_files} files! (Fewer is better)', file=sys.stderr)
    print(f'These cycles can be split into {len(nontrivial_connected_components)} components. (More is better.)',
          file=sys.stderr)

    print_components(nontrivial_connected_components, deps)

    print('// You can generate a fancy diagram like this:')
    print(f'// {" ".join(sys.argv)} > cycles.gv || dot cycles.gv -Tpng > cycles.png')
    exit(1)  # Fail pre-commit hook


def guess_architecture():
    matches = []
    for arch in 'aarch64 i686 x86_64 lagom'.split():
        # build.ninja is updated on basically any build command.
        # Hence, use this file to guess which directory is the most recent one:
        try:
            mtime = os.path.getmtime('Build/{}/build.ninja'.format(arch))
        except FileNotFoundError:
            pass  # Oh well, didn't work
        else:
            matches.append((arch, mtime))

    if not matches:
        return 'UNKNOWN'

    # Choose the most recently used build directory:
    matches.sort(reverse=True, key=lambda e: e[1])
    return matches[0][0]


if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__) + "/..")
    if 'SERENITY_ARCH' in os.environ:
        run(os.environ['SERENITY_ARCH'])
    else:
        # Try to guess the architecture. We need the architecture to find generated headers,
        # which are unlikely to be included in a cycle, so it is not too bad if we guess wrong.
        run(guess_architecture())
