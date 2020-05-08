#!/bin/sh
set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path"

fast_mode=
while [ "$1" != "" ]; do
    case $1 in
        -f | --fast )           fast_mode=1
                                ;;
        -h | --help )           printf -- "-f or --fast: build fast without cleaning or running tests\n"
                                exit 0
                                ;;
    esac
    shift
done

if ! (command -v genext2fs 1>/dev/null && command -v fakeroot 1>/dev/null); then
	sudo id
fi

MAKE="make"

if [ "$(uname -s)" = "OpenBSD" ] || [ "$(uname -s)" = "FreeBSD" ]; then
	MAKE="gmake"
fi

if [ "$fast_mode" = "1" ]; then
    $MAKE -C ../ && \
        $MAKE -C ../ install &&
        ./sync.sh
else
    $MAKE -C ../ clean && \
        $MAKE -C ../ && \
        $MAKE -C ../ test && \
        $MAKE -C ../ install &&
        ./sync.sh
fi
