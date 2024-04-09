#!/usr/bin/env python3
"""
This script will create a new test file and expectations in the Tests/LibWeb directory
"""

import argparse
from pathlib import Path

TEST_DIR = Path(__file__).resolve().parent


def create_text_test(test_name: str, is_async: bool = False) -> None:
    """
    Create a new Text type test file with the given test name.

    Args:
        test_name (str): Name of the test.
        is_async (bool, optional): Whether it is an async test. Defaults to False.
    """
    input_prefix = TEST_DIR / "Text" / "input" / test_name
    input_dir = input_prefix.parent
    input_file = input_prefix.with_suffix(".html")

    expected_prefix = TEST_DIR / "Text" / "expected" / test_name
    expected_dir = expected_prefix.parent
    expected_file = expected_prefix.with_suffix(".txt")

    # Create directories if they don't exist
    input_dir.mkdir(parents=True, exist_ok=True)
    expected_dir.mkdir(parents=True, exist_ok=True)

    num_sub_levels = len(Path(test_name).parents) - 1
    path_to_include_js = "../" * num_sub_levels + "include.js"

    # Create input and expected files
    input_file.write_text(fR"""<!DOCTYPE html>
<script src="{path_to_include_js}"></script>
<script>
    {f"asyncTest(async (done)" if is_async else "test(()"} => {{
        println("Expected println() output");
    {f"    done();" if is_async else ""}
    }});
</script>
""")
    expected_file.write_text("Expected println() output\n")

    print(f"Text test '{Path(test_name).with_suffix('.html')}' created successfully.")


def main():
    parser = argparse.ArgumentParser(description="Create a new LibWeb Text test file.")
    parser.add_argument("test_name", type=str, help="Name of the test")
    parser.add_argument("--async", action="store_true", help="Flag to indicate if it's an async test", dest="is_async")
    args = parser.parse_args()

    # TODO: Add support for other test types: Layout and Ref
    create_text_test(args.test_name, args.is_async)


if __name__ == "__main__":
    main()
