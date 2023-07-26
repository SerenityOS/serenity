#!/usr/bin/env python3

import argparse
import shutil
import subprocess
import sys
from functools import cmp_to_key
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(
                 epilog=__doc__,
                 formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('kernel', help='kernel binary location')
    parser.add_argument('-n', '--nm', required=True, help='path to nm')
    parser.add_argument('-o', '--objcopy', required=True, help='path to objcopy')
    args = parser.parse_args()

    kernel_path = Path(args.kernel)
    binary_directory = kernel_path.parent

    # Write out kernel.map, which contains kernel symbols and their addresses
    symbols = subprocess.check_output([args.nm, '-C', '-n', str(kernel_path)])
    if not symbols:
        print(f"Unable to dump symbols from {kernel_path}", file=sys.stderr)
        return 1

    def filter_symbol(s):
        return s and not (".Lubsan_data" in s or s.split()[1] == "a")

    symbols = filter(filter_symbol, symbols.decode().split('\n'))
    symbols = list(set(symbols))

    def sort_symbols(s1, s2):
        return int(s1.split()[0], base=16) - int(s2.split()[0], base=16)
    symbols = sorted(symbols, key=cmp_to_key(sort_symbols))

    kernel_map = binary_directory / "kernel.map"
    with open(kernel_map, 'w') as out:
        out.write(f'{len(symbols):#x}\n')
        out.write('\n'.join(symbols))
        out.write('\0')

    kernel_final = binary_directory / "Kernel"

    shutil.copyfile(kernel_path, kernel_final)

    kernel_final = str(kernel_final)

    subprocess.check_call([args.objcopy, "--update-section", f".ksyms={kernel_map}", kernel_final])
    subprocess.check_call([args.objcopy, "--only-keep-debug", kernel_final, kernel_final + ".debug"])
    subprocess.check_call([args.objcopy, "--strip-debug", kernel_final])
    subprocess.check_call([args.objcopy, f"--add-gnu-debuglink={kernel_final}.debug", kernel_final])

    return 0


if __name__ == "__main__":
    sys.exit(main())
