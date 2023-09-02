#!/usr/bin/env -S bash ../.port_include.sh
port='kakoune'
version='24d6072353f7c7e7cac84b4eb085036a8c852f96'
files=(
    "https://github.com/mawww/kakoune/archive/${version}.tar.gz#16440b204770972f318e24e4e178ada474b7cfeb029cefa69e9ff035700a129e"
)
depends=("bash" "sed")
makeopts+=(
    "LDFLAGS=-L${DESTDIR}/usr/local/lib"
)
