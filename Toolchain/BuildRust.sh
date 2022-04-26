#!/usr/bin/env bash
set -eo pipefail

# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

PREFIX="$DIR/Local/rust/"
BUILD="$DIR/../Build/"

MD5SUM="md5sum"
SED="sed"
REALPATH="realpath"
NPROC="nproc"

SYSTEM_NAME="$(uname -s)"

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    REALPATH="readlink -f"
    NPROC="sysctl -n hw.ncpuonline"
    MD5SUM="md5 -q"
    export CC=egcc
    export CXX=eg++
    export LDFLAGS=-Wl,-z,notext
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    NPROC="sysctl -n hw.ncpu"
    MD5SUM="md5 -q"
elif [ "$SYSTEM_NAME" = "Darwin" ]; then
    MD5SUM="md5 -q"
    NPROC="sysctl -n hw.ncpu"
    REALPATH="grealpath"  # GNU coreutils
    SED="gsed"            # GNU sed
fi

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$($NPROC)
fi

if [ ! -d "$BUILD" ]; then
    mkdir -p "$BUILD"
fi
BUILD=$($REALPATH "$BUILD")

git_patch=
while [ "$1" != "" ]; do
    case $1 in
        --dev )           git_patch=1
                          ;;
    esac
    shift
done

echo PREFIX is "$PREFIX"

mkdir -p "$DIR/Tarballs"

RUST_VERSION=1.65.0
RUST_NAME="rustc-${RUST_VERSION}-src"
RUST_PACKAGE="${RUST_NAME}.tar.gz"
RUST_URL="https://static.rust-lang.org/dist/${RUST_PACKAGE}"
RUST_MD5SUM=a4b7f70abcba3933fe955e3229a0095d

RUST_LIBC_VERSION=0.2.137
RUST_LIBC_NAME="libc-${RUST_LIBC_VERSION}"
RUST_LIBC_PACKAGE="${RUST_LIBC_NAME}.tar.gz"
RUST_LIBC_URL="https://github.com/rust-lang/libc/archive/refs/tags/${RUST_LIBC_VERSION}.tar.gz"
RUST_LIBC_MD5SUM=58229505df135b4a107dcf9518c74517

buildstep() {
    NAME=$1
    shift
    "$@" 2>&1 | sed $'s|^|\x1b[34m['"${NAME}"$']\x1b[39m |'
}

### Grab rust and rust/libc
pushd "$DIR/Tarballs"

    ## Download
    md5=""
    if [ -e "$RUST_PACKAGE" ]; then
        md5="$($MD5SUM ${RUST_PACKAGE} | cut -f1 -d' ')"
        echo "rust md5='$md5'"
    fi

    if [ "$md5" != ${RUST_MD5SUM} ] ; then
        rm -f ${RUST_PACKAGE}
        curl -LO "${RUST_URL}"
    else
        echo "Skipped downloading rust"
    fi

    md5=""
    if [ -e "${RUST_LIBC_PACKAGE}" ]; then
        md5="$($MD5SUM ${RUST_LIBC_PACKAGE} | cut -f1 -d' ')"
        echo "rust libc md5='$md5'"
    fi
    if [ "$md5" != ${RUST_LIBC_MD5SUM} ] ; then
        rm -f "${RUST_LIBC_PACKAGE}"
        curl -o "${RUST_LIBC_PACKAGE}" -L "${RUST_LIBC_URL}" 
    else
        echo "Skipped downloading rust libc"
    fi

    ## Extract

    patch_rust_md5="$(${MD5SUM} "${DIR}"/Patches/rust/*.patch)"

    if [ ! -d "${RUST_NAME}" ] || [ "$(cat ${RUST_NAME}/.patch.applied)" != "${patch_rust_md5}" ]; then
        if [ -d ${RUST_NAME} ]; then
            rm -rf "${RUST_NAME}"
            rm -rf "${DIR}/Build/rust/${RUST_NAME}"
        fi
        echo "Extracting rust..."
        tar -xzf ${RUST_PACKAGE}

        pushd ${RUST_NAME}
            if [ "${git_patch}" = "1" ]; then
                git init > /dev/null
                git add . > /dev/null
                git commit -am "BASE" > /dev/null
                git am --keep-non-patch "${DIR}"/Patches/rust/*.patch > /dev/null
            else
                for patch in "${DIR}"/Patches/rust/*.patch; do
                    patch -p1 < "${patch}" > /dev/null
                done
            fi
            ${MD5SUM} "${DIR}"/Patches/rust/*.patch > .patch.applied
        popd
    else
        echo "Using existing rust source directory"
    fi

    patch_libc_md5="$(${MD5SUM} "${DIR}"/Patches/rust-libc/*.patch)"

    if [ ! -d "${RUST_LIBC_NAME}" ] || [ "$(cat ${RUST_LIBC_NAME}/.patch.applied)" != "${patch_libc_md5}" ]; then
        if [ -d ${RUST_LIBC_NAME} ]; then
            rm -rf "${RUST_LIBC_NAME}"
        fi
        echo "Extracting rust libc..."
        tar -xzf ${RUST_LIBC_PACKAGE}

        # NOTE: Always create a git repo here, or the rust Cargo.toml will be unhappy with our libc
        pushd ${RUST_LIBC_NAME}
            git init > /dev/null
            git add . > /dev/null
            git commit -am "BASE" > /dev/null
            git am --keep-non-patch "${DIR}"/Patches/rust-libc/*.patch > /dev/null
            git tag -a "${RUST_LIBC_VERSION}" -m ""
            ${MD5SUM} "${DIR}"/Patches/rust-libc/*.patch > .patch.applied
        popd
    else
        echo "Using existing rust libc source directory"
    fi
popd

#### Build rust for serenity

rm -rf "${PREFIX}"
mkdir -p "$PREFIX"

mkdir -p "$DIR/Build/rust"

export CARGO_HOME="$DIR/Local/rust/.cargo"
mkdir -p "${CARGO_HOME}"

pushd "$DIR/Tarballs/${RUST_NAME}"

    export DESTDIR="$PREFIX"

    # Generate config.toml for building rust itself with proper absolute paths for the current serenity tree
    "$SED" "s|@SERENITY_TOOLCHAIN_ROOT@|${DIR}|g" "$DIR/Rust/rust-config.toml.in" > "$DIR/Tarballs/${RUST_NAME}/config.toml"

    # Generate config.toml for use by rust and any rust Ports
    # @TODO: Consider "cloning" local libc source to Local/rust somewhere
    "$SED" "s|@SERENITY_TOOLCHAIN_ROOT@|${DIR}|g" "$DIR/Rust/config.toml.in" > "${CARGO_HOME}/config.toml"
    "$SED" -i "s|@RUST_LIBC_NAME@|${RUST_LIBC_NAME}|g" "${CARGO_HOME}/config.toml"

    cargo update -p libc --precise "${RUST_LIBC_VERSION}"

    buildstep serenity-stage1 python3 ./x.py --color always --build-dir "$DIR/Build/rust" install -i --stage 1 --target x86_64-unknown-serenity compiler/rustc library/std cargo rustfmt

    # Make sure we have proc_macros available for host with a matching version, in case the developer doesn't have
    # a nightly installed that they keep up to date. If adding more/different hosts here, update config.toml.in [build.target]
    buildstep host python3 ./x.py --color always --build-dir "$DIR/Build/rust"  install -i --stage 1 --target x86_64-unknown-linux-gnu library/std

popd
