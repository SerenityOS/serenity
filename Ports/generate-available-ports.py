#!/usr/bin/env python3

from os import chdir
from pathlib import Path
import re
import sys
import asyncio

CONCURRENCY = 20
GIT_HASH_REGEX = re.compile(r'^[0-9a-f]{40}$')


def escape_markdown(value):
    return value.replace("_", "\\_").replace("*", "\\*")


def format_link(target, name=None):
    return f'[`{name or target}`]({target}/)'


def format_version(version):
    return '' if version == "git" \
        else version[0:7] if GIT_HASH_REGEX.match(version) \
        else escape_markdown(version)


HEADERS = {
    'port': {'caption': 'Port', 'min_width': 51, 'formatter': format_link},
    'description': {'caption': 'Name', 'min_width': 63, 'formatter': escape_markdown},
    'version': {'caption': 'Version', 'min_width': 24, 'formatter': format_version},
    'website': {'caption': 'Website', 'min_width': 78}
}

PROPERTIES = HEADERS.keys()


def die(message, code):
    print(f"\n{message}\n", file=sys.stderr)
    sys.exit(code)


def join(data, separator, prefix='', postfix=''):
    return f"{prefix}{separator.join(data)}{postfix}"


async def read_port_properties(port):
    if not Path(f'{port}/package.sh').exists:
        die(f"No package for port: {port}\n", 1)

    read_property_command = f"./package.sh showproperty {' '.join(PROPERTIES)}"
    process = await asyncio.create_subprocess_shell(read_property_command, cwd=port,
                                                    stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)

    print(".", file=sys.stderr, end='')
    sys.stderr.flush()

    if await process.wait() != 0:
        error = await process.stderr.readline()
        print('\n\n  ✖ ', file=sys.stderr, end='')
        sys.stderr.writelines(error.decode('utf-8'))

        die(f'Could not collect package details for port: {port}  (exit code: {process.returncode})', 2)

    properties = {name: (await process.stdout.readuntil(b"\n\n")).decode('utf-8').strip() for name in PROPERTIES}

    if properties['port'] != port:
        die(f"Unexpected name '{properties['port']}' for port: {port}", 3)

    for name in PROPERTIES:
        if properties[name] == '':
            die(f"Missing property '{name}' for port: {port}", 3)

    return properties


async def read_ports():
    ports = sorted((port.name for port in Path().iterdir() if Path(port).is_dir()), key=str.lower)
    chunks = (ports[index:index + CONCURRENCY] for index in range(0, len(ports), CONCURRENCY))

    print(f'Reading {len(ports)} ports…', file=sys.stderr)
    properties = [await asyncio.gather(*(read_port_properties(port) for port in chunk), return_exceptions=True) for
                  chunk in chunks]
    print('\n', file=sys.stderr)
    sys.stderr.flush()

    return dict(zip(ports, (item for chunk in properties for item in chunk)))


prologue = """\
# Available ports

This list is also available at [ports.serenityos.net](https://ports.serenityos.net)
"""

epilogue = """"""


def get_settings(setting, default):
    return dict((name, header.get(setting, default)) for name, header in HEADERS.items())


async def main():
    ports = await read_ports()

    captions = get_settings('caption', '?')
    min_widths = get_settings('min_width', 0)
    formatters = get_settings('formatter', lambda x: x)

    rows = [{header: formatters[header](port[header]) for header in PROPERTIES} for port in ports.values()]
    widths = {header: max(min_widths[header], *(len(row[header]) for row in rows)) for header in PROPERTIES}

    def aligned(data): return (data[header].ljust(widths[header]) for header in PROPERTIES)

    def filled_with(fill): return (fill * widths[header] for header in PROPERTIES)

    table_header = aligned(captions)
    table_underline = filled_with('-')
    table_rows = (join(aligned(row), ' | ', '| ', ' |') for row in rows)

    print(
        prologue +
        join(table_header, ' | ', '\n| ', ' |\n') +
        join(table_underline, '-|-', '|-', '-|') +
        join(table_rows, '\n', '\n', '\n') +
        epilogue,
        end=''
    )


if __name__ == '__main__':
    chdir(Path(__file__).parent)
    asyncio.run(main())
