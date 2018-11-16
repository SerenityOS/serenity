cp ../../figlet-2.2.5/figlet mnt/bin/
mkdir -p mnt/usr/local/share/figlet
FIGLET_FONTDIR=/
cp ../../figlet-2.2.5/fonts/standard.flf mnt/$FIGLET_FONTDIR
cp ../../figlet-2.2.5/fonts/big.flf mnt/$FIGLET_FONTDIR
cp ../../figlet-2.2.5/fonts/slant.flf mnt/$FIGLET_FONTDIR
cp ../../figlet-2.2.5/fonts/lean.flf mnt/$FIGLET_FONTDIR

cp ../../bash-2.05b/bash mnt/bin/bash

