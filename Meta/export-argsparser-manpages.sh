#!/bin/sh

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

if ! command -v tar >/dev/null 2>&1 ; then
    echo "Please install tar!"
    exit 1
fi

if [ "$#" = "0" ]; then
    VERIFY_GIT_STATE=n
elif [ "$#" = "1" ] && [ "$1" = "--verify-git-state" ]; then
    VERIFY_GIT_STATE=y
else
    echo "USAGE: $0 [--verify-git-state]"
    echo "This script runs Serenity and exports a set of manpages through ArgsParser,"
    echo "and places them in Base/usr/share/man/."
    echo "If --verify-git-state is given, this script verifies that this does not modify"
    echo "the git state, i.e. that all exported manpages already were in the repository"
    echo "with the exact same content."
    exit 1
fi

echo "This script assumes passwordless sudo."
sudo true

if [ -z "$BUILD_DIR" ]; then
    if [ -z "$SERENITY_ARCH" ]; then
        export SERENITY_ARCH="x86_64"
        echo "SERENITY_ARCH not given. Assuming ${SERENITY_ARCH}."
    fi
    BUILD_DIR=Build/"$SERENITY_ARCH"
    echo "BUILD_DIR not given. Assuming ${BUILD_DIR}."
fi

if [ -e fsmount ]; then
    echo "Directory 'fsmount' already exists."
    echo "Manual cleanup needed. You might also need to unmount first."
    exit 1
fi

if ! [ -d Base/usr/share/man/ ]; then
    echo "Base/usr/share/man/ does not exist. How did that happen?! o.O"
    exit 1
fi

echo "Using 'ninja run' to generate manpages ..."
export SERENITY_RUN="ci"
export SERENITY_KERNEL_CMDLINE="graphics_subsystem_mode=off panic=shutdown system_mode=generate-manpages"
# The 'sed' gets rid of the clear-screen escape sequence.
ninja -C "$BUILD_DIR" -- run | sed -re 's,''c,,'
echo

mkdir fsmount
sudo mount -t ext2 -o loop,rw "$BUILD_DIR"/_disk_image fsmount

if sudo test -f "fsmount/root/generate_manpages_error.log"; then
    echo ":^( Generating manpages failed, error log:"
    sudo cat fsmount/root/generate_manpages_error.log

    sudo umount fsmount
    rmdir fsmount

    exit 1
fi

echo "Extracting generated manpages ..."
# 'cp' would create the new files as root, but we don't want that.
sudo tar -C fsmount/root/generated_manpages --create . | tar -C Base/usr/share/man/ --extract
sudo umount fsmount
rmdir fsmount

echo "Successfully (re-)generated manpages in Base/usr/share/man/"

if [ "$VERIFY_GIT_STATE" = "y" ]; then
    echo "Verifying git state ..."
    if [ "" != "$(git clean -n Base/usr/share/man)" ] || ! git diff --quiet Base/usr/share/man; then
        echo "Failed: There are missing and/or outdated manpages."
        echo "$ git status Base/usr/share/man"
        git status Base/usr/share/man
        echo "$ git diff Base/usr/share/man"
        git diff Base/usr/share/man
        echo "You may need to run ./Meta/export-argsparser-manpages.sh on your system and commit/squash the resulting changes."
        exit 1
    else
        echo "Verified: No missing or outdated manpages. Great!"
    fi
fi
