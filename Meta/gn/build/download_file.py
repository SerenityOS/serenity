#!/usr/bin/env python3
r"""Downloads a file as a build artifact.

The file is downloaded to the specified directory.

It's intended to be used for files that are cached between runs.

"""

import argparse
import os
import pathlib
import shutil
import sys
import tempfile
import urllib.request


def main():
    parser = argparse.ArgumentParser(
                 epilog=__doc__,
                 formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('url', help='input url')
    parser.add_argument('-o', '--output', required=True,
                        help='output file')
    parser.add_argument('-v', '--version', required=True,
                        help='version of file to detect mismatches and redownload')
    parser.add_argument('-f', '--version-file', required=True,
                        help='filesystem location to cache version')
    parser.add_argument('-c', "--cache-path", required=False,
                        help='path for cached files to clear on version mismatch')
    args = parser.parse_args()

    version_from_file = ''
    version_file = pathlib.Path(args.version_file)
    if version_file.exists():
        with version_file.open('r') as f:
            version_from_file = f.readline().strip()

    if version_from_file == args.version:
        return 0

    # Fresh build or version mismatch, delete old cache
    if (args.cache_path):
        cache_path = pathlib.Path(args.cache_path)
        shutil.rmtree(cache_path, ignore_errors=True)
        cache_path.mkdir(parents=True)

    print(f"Downloading version {args.version} of {args.output}...", end='')

    with urllib.request.urlopen(args.url) as f:
        try:
            with tempfile.NamedTemporaryFile(delete=False,
                                             dir=pathlib.Path(args.output).parent) as out:
                out.write(f.read())
                os.rename(out.name, args.output)
        except IOError:
            os.unlink(out.name)

    print("done")

    with open(version_file, 'w') as f:
        f.write(args.version)


if __name__ == '__main__':
    sys.exit(main())
