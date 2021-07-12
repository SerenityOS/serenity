#!/usr/bin/env -S bash ../.port_include.sh
port=v
auth_type=sha256
version=weekly.2021.28
files="https://codeload.github.com/vlang/v/tar.gz/refs/tags/$version v-$version.tar.gz a6a28e5a7984439d20e8cc31a01984ced065da9d7bd9ccc1a171f94ca2a06c68"

build() {
    (
    cd "$workdir"
    make CC=gcc all # local build
    ./v -prod -cc "$CC" -o v2 cmd/v
    )
}

install() {
    # v requires having rw access to the srcdir to rebuild on demand
    # so we just copy that into the default user's home for now.
    # proper system-wide dist builds will be added in vlang later
    mkdir -p "${SERENITY_INSTALL_ROOT}/home/anon/vlang"
    cp -rf "$workdir"/* "${SERENITY_INSTALL_ROOT}/home/anon/vlang"
    ln -fs "${SERENITY_INSTALL_ROOT}/home/anon/vlang/v2" "${SERENITY_INSTALL_ROOT}/usr/local/bin/v"
}
