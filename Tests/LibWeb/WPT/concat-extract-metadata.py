#!/usr/bin/env python3

"""
Usage:

- To concatenate all WPT expectation files in the metadata directory into metadata.tx:
  concat-extract-metadata.py --concat metadata > metadata.txt

- To extract expectation files from metadata.txt into the metadata directory:
  concat-extract-metadata.py --extract metadata.txt metadata
"""

import argparse
import os


def concat_metadata_files(metadata_directory):
    concatenated_metadata = ""

    for root, _, files in os.walk(metadata_directory):
        for filename in files:
            if filename.endswith(".ini"):
                filepath = os.path.join(root, filename)
                relative_path = os.path.relpath(filepath, metadata_directory)
                with open(filepath, "r") as file:
                    file_content = file.read()
                    concatenated_metadata += f"--- {relative_path} ---\n"
                    concatenated_metadata += file_content + "\n"

    return concatenated_metadata


def extract_metadata_files(concatenated_metadata_file, output_metadata_directory):
    with open(concatenated_metadata_file, "r") as file:
        concatenated_metadata = file.read()

    lines = concatenated_metadata.splitlines()

    current_filename = None
    current_content = []

    def flush_content_to_file(filename, content, output_directory):
        filepath = os.path.join(output_directory, filename)
        os.makedirs(os.path.dirname(filepath), exist_ok=True)
        with open(filepath, "w") as file:
            file.write("\n".join(content))
        return filepath

    for line in lines:
        if line.startswith("---"):
            if current_filename:
                flush_content_to_file(current_filename, current_content, output_metadata_directory)

            current_filename = line[4:-4].strip()
            current_content = []
        else:
            current_content.append(line)

    if current_filename:
        flush_content_to_file(current_filename, current_content, output_metadata_directory)


parser = argparse.ArgumentParser(description="Concatenate and extract WPT metadata files")

parser.add_argument("--concat", help="Concatenate expectation files from specified directory")
parser.add_argument("--extract", nargs=2, help="Extract expectation files from specified input concatenated file")

args = parser.parse_args()

if args.concat:
    metadata_directory_path = args.concat
    print(concat_metadata_files(metadata_directory_path))

if args.extract:
    input_concatenated_metadata_filename, output_metadata_directory = args.extract
    extract_metadata_files(input_concatenated_metadata_filename, output_metadata_directory)
