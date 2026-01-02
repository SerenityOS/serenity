#!/usr/bin/env python3

# In Xcode 26.2, Cocoa.h doesn't compile with -std=c++26,
# https://openradar.appspot.com/21478051
#
# This patches up bad headers in the meantime.
#
# Use like this, and add `foobar` to your include search path:
#     Meta/Lagom/patch_mac_sdk_headers_for_cxx26.py -o foobar --sdk_path $(xcrun --show-sdk-path)


import argparse
import os
import subprocess


patches = {
    "CoreGraphics/CGImage.h": [
        (
         'return alpha | component | byteOrder | pixelFormat;',
         'return (CGBitmapInfo)((CGBitmapInfo)alpha | (CGBitmapInfo)component | (CGBitmapInfo)byteOrder | (CGBitmapInfo)pixelFormat);'  # noqa: E501
        ),
    ],
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--sdk-path', help='Path to Mac SDK', required=True)
    parser.add_argument('-o', '--output-dir', help='Output path for the patched header files', required=True)
    args = parser.parse_args()

    sdk_path = args.sdk_path
    if not sdk_path:
        sdk_path = subprocess.check_output(['xcrun', '--show-sdk-path']).decode().strip()

    for relative_path, replacements in patches.items():
        framework_name, header_name = relative_path.split("/")
        sdk_file_path = f'{sdk_path}/System/Library/Frameworks/{framework_name}.framework/Headers/{header_name}'
        with open(sdk_file_path, "r") as f:
            content = f.read()

        for old, new in replacements:
            content = content.replace(old, new)

        output_path = f'{args.output_dir}/{relative_path}'
        os.makedirs(os.path.dirname(output_path), exist_ok=True)

        with open(output_path, "w") as f:
            f.write(content)


if __name__ == '__main__':
    main()
