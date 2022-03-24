#!/usr/bin/env -S bash ../.port_include.sh

port=cfunge
version=2bc4fb27ade2a816ca9a90a6d9f6958111123fa9
workdir=cfunge-${version}/build
useconfigure=true
files="https://codeload.github.com/VorpalBlade/cfunge/zip/${version} cfunge.zip 364994a890ed1083684956db576a2a5cfb94b3117bae868910d6a75111033f55"
auth_type=sha256

mkdir -p cfunge-${version}/build

patch_internal() {
    # patch if it was not yet patched (applying patches multiple times doesn't work!)
    if [ -z "${IN_SERENITY_PORT_DEV:-}" ] && [ -d patches ]; then
        for filepath in patches/*.patch; do
            filename=$(basename $filepath)
            if [ ! -f "$workdir"/.${filename}_applied ]; then
                run patch -d .. -p"$patchlevel" < "$filepath"
                run touch .${filename}_applied
            fi
        done
    fi
}

configure() {
    run cmake "${configopts[@]}" ..
}

install() {
    run cp cfunge "${SERENITY_INSTALL_ROOT}/bin"
}
