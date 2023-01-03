#!/usr/bin/env python3

import os
import sys
import subprocess

TEST_FILE_TEMPLATE = '''\
#include <{filename}>
// Check idempotency:
#include <{filename}>
'''


def get_headers_here():
    result = subprocess.run(
        [
            'git',
            'ls-files',
            'AK/*.h',
            'Userland/Libraries/LibArchive/*.h',
            'Userland/Libraries/LibAudio/*.h',
            'Userland/Libraries/LibC/*.h',
            'Userland/Libraries/LibCards/*.h',
            'Userland/Libraries/LibChess/*.h',
            'Userland/Libraries/LibCodeComprehension/*.h',
            'Userland/Libraries/LibCompress/*.h',
            'Userland/Libraries/LibConfig/*.h',
            'Userland/Libraries/LibCore/*.h',
            'Userland/Libraries/LibCoredump/*.h',
            'Userland/Libraries/LibCpp/*.h',
            'Userland/Libraries/LibCrypt/*.h',
            'Userland/Libraries/LibCrypto/*.h',
            'Userland/Libraries/LibDebug/*.h',
            'Userland/Libraries/LibDesktop/*.h',
            'Userland/Libraries/LibDeviceTree/*.h',
            'Userland/Libraries/LibDiff/*.h',
            'Userland/Libraries/LibDNS/*.h',
            'Userland/Libraries/LibDSP/*.h',
            'Userland/Libraries/LibEDID/*.h',
            'Userland/Libraries/LibELF/*.h',
            'Userland/Libraries/LibFileSystemAccessClient/*.h',
            'Userland/Libraries/LibGemini/*.h',
            'Userland/Libraries/LibGfx/*.h',
            'Userland/Libraries/LibGL/*.h',
            'Userland/Libraries/LibGLSL/*.h',
            'Userland/Libraries/LibGPU/*.h',
            'Userland/Libraries/LibGUI/*.h',
            'Userland/Libraries/LibHTTP/*.h',
            'Userland/Libraries/LibIDL/*.h',
            'Userland/Libraries/LibImageDecoderClient/*.h',
            'Userland/Libraries/LibIMAP/*.h',
            'Userland/Libraries/LibIPC/*.h',
            'Userland/Libraries/LibJS/*.h',
            'Userland/Libraries/LibKeyboard/*.h',
            'Userland/Libraries/LibLine/*.h',
            'Userland/Libraries/LibLocale/*.h',
            'Userland/Libraries/LibMain/*.h',
            # 'Userland/Libraries/LibManual/*.h',
            'Userland/Libraries/LibMarkdown/*.h',
            'Userland/Libraries/LibPartition/*.h',
            'Userland/Libraries/LibPCIDB/*.h',
            # 'Userland/Libraries/LibPDF/*.h',
            'Userland/Libraries/LibProtocol/*.h',
            'Userland/Libraries/LibRegex/*.h',
            'Userland/Libraries/LibSanitizer/*.h',
            # 'Userland/Libraries/LibSoftGPU/*.h',
            # 'Userland/Libraries/LibSQL/*.h',
            'Userland/Libraries/LibSymbolication/*.h',
            'Userland/Libraries/LibSyntax/*.h',
            'Userland/Libraries/LibSystem/*.h',
            'Userland/Libraries/LibTest/*.h',
            'Userland/Libraries/LibTextCodec/*.h',
            'Userland/Libraries/LibThreading/*.h',
            'Userland/Libraries/LibTimeZone/*.h',
            'Userland/Libraries/LibTLS/*.h',
            'Userland/Libraries/LibUnicode/*.h',
            'Userland/Libraries/LibUSBDB/*.h',
            'Userland/Libraries/LibVideo/*.h',
            # 'Userland/Libraries/LibVirtGPU/*.h',
            'Userland/Libraries/LibVT/*.h',
            'Userland/Libraries/LibWasm/*.h',
            # 'Userland/Libraries/LibWeb/*.h',
            'Userland/Libraries/LibWebSocket/*.h',
            'Userland/Libraries/LibWebView/*.h',
            'Userland/Libraries/LibX86/*.h',
            'Userland/Libraries/LibXML/*.h',
        ],
        check=True, capture_output=True, text=True)
    assert result.stderr == ''
    output = result.stdout.split('\n')
    assert output[-1] == ''  # Trailing newline
    assert len(output) > 500, 'There should be well over a thousand headers, not only {}?!'.format(len(output))
    return output[:-1]


def as_filename(header_path):
    return header_path.replace('/', '__') + '__test.cpp'


def verbosely_write(path, new_content):
    print(path)
    # FIXME: Ensure directory exists
    if os.path.exists(path):
        with open(path, 'r') as fp:
            old_data = fp.read()
        if old_data == new_content:
            # Fast path! Don't trigger ninja
            return
    with open(path, 'w') as fp:
        fp.write(new_content)


def generate_part(header):
    content = TEST_FILE_TEMPLATE.format(filename=header)
    if header.startswith('Kernel/'):
        content += '#define KERNEL\n'
    verbosely_write(as_filename(header), content)


def run(root_path, arch):
    os.chdir(root_path)
    headers_list = get_headers_here()

    generated_files_path = os.path.join(root_path, 'Build', arch, 'Meta', 'HeaderCheck')
    if not os.path.exists(generated_files_path):
        os.mkdir(generated_files_path)
    os.chdir(generated_files_path)
    for header in headers_list:
        generate_part(header)


if __name__ == '__main__':
    if 'SERENITY_SOURCE_DIR' not in os.environ:
        print('Must set SERENITY_SOURCE_DIR first!', file=sys.stderr)
        exit(1)
    if len(sys.argv) == 2:
        run(os.environ['SERENITY_SOURCE_DIR'], sys.argv[1])
    else:
        print('Usage: SERENITY_SOURCE_DIR=/path/to/serenity {} SERENITY_BUILD_ARCH'
              .format(sys.argv[0]), file=sys.stderr)
        exit(1)
