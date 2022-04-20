#!/usr/bin/env -S bash ../.port_include.sh
port=quake3
version=1.34
commit_hash=6d74896557d8c193a9f19bc6845a47e9d0f77db2
archive_hash=1db91cfd05170ed5b37c1ab56cdf7bbe6b3c86fc6baee8b68e8e539fddfd88c1
files="https://github.com/ioquake/ioq3/archive/$commit_hash.tar.gz ioq3.tar.gz $archive_hash"
auth_type=sha256
workdir="ioq3-${commit_hash}"
depends=("SDL2")
launcher_name="QuakeIII: Arena"
launcher_category="Games"
launcher_command=/usr/local/games/quake3/ioquake3
icon_file="misc/quake3.png"

install() {
    run make COPYDIR=${SERENITY_INSTALL_ROOT}/usr/local/games/quake3/ copyfiles
}

post_install() {
	# Let's create a more Serenity friendly `autoexec.cfg` file :^)
	cat <<- 'EOF' > ${SERENITY_INSTALL_ROOT}/usr/local/games/quake3/baseq3/autoexec.cfg
set cl_renderer "opengl1"
set r_fullscreen "0"
set cg_drawfps "1"
EOF

echo ""
echo ""
echo "==== Post installation instructions ===="
echo "Please remember to install baseq3 from your Quake3 install"
echo "into /usr/local/games/quake3/"
echo "Don't forget to add the following to Base/etc/fstab/:"
echo "/usr/local/games/quake3	/usr/local/games/quake3	bind	bind,nodev,nosuid,wxallowed"
}
