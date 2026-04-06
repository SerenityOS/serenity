#!/usr/bin/env -S bash ../.port_include.sh
port='os-test'
version='e1272e6a83f55b56af044096a74b46bfc6f991d2'
files=(
    "git+https://gitlab.com/sortix/os-test.git#$version"
)

depends=(
    'make'
    'bash'
)

makeopts=(
    "-j${MAKEJOBS}"
    'OS=SerenityOS'
    'CFLAGS='
    'LDFLAGS='
    'EXTRA_LDFLAGS='
    "CC_FOR_BUILD=${HOST_CC}"
)

install() {
    outdir="$SERENITY_INSTALL_ROOT/usr/local/os-test/"
    mkdir -p "$outdir"
    cp -pR "$SERENITY_BUILD_DIR/Ports/os-test/os-test-$version/"* "$outdir"

    known_failures=(
        'pty/tcsetpgrp-wrong-orphan'
        'signal/block-chld-default-unblock'
        'signal/ppoll-block-raise'
        'stdio/printf-g-negative-width'
        'udp/accept'
        'udp/connect-send-error-accept'
    )

    for p in "${known_failures[@]}"; do
      echo skipped > "$outdir/out/serenityos/$p.out"
    done

    run mkdir -p "$SERENITY_INSTALL_ROOT/root/"
    run cp "$SERENITY_SOURCE_DIR/Meta/Contrib/os-test/run-os-test-and-shutdown.sh" "$SERENITY_INSTALL_ROOT/root/"
}
