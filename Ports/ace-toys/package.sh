#!/usr/bin/env -S bash ../.port_include.sh
port='ace-toys'
version='79e42c1353d0f1bb34322bd7525c7b25644f0f33'
files=("git+https://github.com/harbin-ctrl/ace-toys.git#${version}")
workdir="ace-toys-${version}"

build() {
    # Generate launcher icons on the host before cross compiling the toys.
    host_env
    (
        run make clean
        for toy in splat poingo balloons; do
            run make -C "$toy" icons CC="$HOST_CC" CXX="$HOST_CXX" CCACHE_PREFIX=
            mkdir -p "${workdir}/port-icons"
            if [ "$toy" = balloons ]; then
                cp "${workdir}/balloons/assets/icon_32x32.png" "${workdir}/port-icons/balloons.png"
            else
                cp "${workdir}/${toy}/icon_32.png" "${workdir}/port-icons/${toy}.png"
            fi
        done
    )

    target_env
    run make clean
    run make "${makeopts[@]}" CC="${CC}" CXX="${CXX}" CCACHE_PREFIX=
}

install() {
    for toy in splat poingo balloons; do
        case "$toy" in
            splat) name='Splat' ;;
            poingo) name='Poingo' ;;
            balloons) name='Balloons' ;;
        esac
        command install -Dm755 "${workdir}/${toy}/${toy}" "${DESTDIR}/usr/local/bin/${toy}"
        install_launcher "$name" '&Demos' "/usr/local/bin/${toy}" ''
        install_icon "port-icons/${toy}.png" "/usr/local/bin/${toy}"
    done
}
