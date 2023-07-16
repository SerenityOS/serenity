#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

. "${script_path}/shell_include.sh"

cleanup() {
    if [ -d mnt ]; then
        umount mnt || ( sleep 1 && sync && umount mnt )
        rmdir mnt
        echo "done"
    fi
}

if [ "$(id -u)" != 0 ]; then
    set +e
    ${SUDO} -- "${SHELL}" -c "\"$0\" $* || exit 42"
    case $? in
        1)
            die "this script needs to run as root"
            ;;
        42)
            exit 1
            ;;
        *)
            exit 0
            ;;
    esac
else
    : "${SUDO_UID:=0}" "${SUDO_GID:=0}"
fi

if [ -z "$SERENITY_TARGET_INSTALL_PARTITION" ]; then
    die "SERENITY_TARGET_INSTALL_PARTITION environment variable was not set!"
fi

printf "verifying partition %s is already ext2... " "$SERENITY_TARGET_INSTALL_PARTITION"
if file -sL "$SERENITY_TARGET_INSTALL_PARTITION" 2>&1 | grep "ext2" > /dev/null; then
    echo "done"
else
    die "$SERENITY_TARGET_INSTALL_PARTITION is not an ext2 partition!"
fi

trap cleanup EXIT

printf "mounting filesystem on device %s... " "$SERENITY_TARGET_INSTALL_PARTITION"
mkdir -p mnt
if ! eval "mount $SERENITY_TARGET_INSTALL_PARTITION mnt/"; then
    die "could not mount existing ext2 filesystem on $SERENITY_TARGET_INSTALL_PARTITION"
else
    echo "done"
fi

"$script_path/build-root-filesystem.sh"
