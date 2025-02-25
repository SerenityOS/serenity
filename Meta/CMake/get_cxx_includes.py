import argparse
import os
import subprocess
import sys


def get_includes_from_version_output(compiler):
    try:
        output = subprocess.check_output(
            [compiler, "-xc++", "/dev/null", "-E", "-Wp,-v"], stderr=subprocess.STDOUT
        )
    except subprocess.CalledProcessError as e:
        print("Error getting includes from compiler version output: " + e.output)
        sys.exit(1)

    includes = []
    in_search_list = False
    for line in output.splitlines():
        line = line.strip()
        if not in_search_list:
            if line == "#include <...> search starts here:":
                in_search_list = True
                continue
            continue
        if line == "End of search list.":
            break
        includes.append(line)

    return includes


def get_includes_from_preprocessor_output(compiler, test_file):
    try:
        output = subprocess.check_output(
            [compiler, "-E", test_file], stderr=subprocess.STDOUT
        )
    except subprocess.CalledProcessError as e:
        print("Error getting includes from preprocessor output: " + e.output)
        sys.exit(1)

    includes = []
    for line in output.splitlines():
        if not line.startswith(b"# 1 "):
            continue
        include = line.removeprefix(b"# 1 ")
        include = include[1:-1]  # Remove quotes
        includes.append(include)

    return includes


def convert_includes_to_dirs(includes):
    dirs = []
    for include in includes:
        if not os.path.isabs(include):
            continue
        include_dir = os.path.dirname(include)
        if include_dir.endswith(b"/bits"):
            include_dir = os.path.dirname(include_dir)

        while not os.path.isdir(include_dir):
            include_dir = os.path.dirname(include_dir)

        dirs.append(include_dir)

    return dirs


def main():
    parser = argparse.ArgumentParser(
        description="Extract System Includes from C++ Compiler"
    )

    parser.add_argument("--compiler", required=True, help="Specify C++ compiler to extract includes from")

    parser.add_argument(
        "--test-file", required=True, help="Specify the path to the test file containing '#include' directives"
    )

    args = parser.parse_args()
    compiler = args.compiler
    test_file = args.test_file

    includes = get_includes_from_version_output(compiler)
    system_includes = get_includes_from_preprocessor_output(compiler, test_file)
    system_includes = convert_includes_to_dirs(system_includes)

    print(
        ";".join(entry.decode("utf-8") for entry in set(includes + system_includes)),
        end="",
    )


if __name__ == "__main__":
    main()
