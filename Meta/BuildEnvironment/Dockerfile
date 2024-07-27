FROM ubuntu

RUN apt update
RUN apt install -y build-essential cmake curl libmpfr-dev libmpc-dev libgmp-dev sudo grub-common grub-pc-bin mtools grub-pc extlinux bash e2fsprogs ninja-build ccache rsync unzip texinfo libssl-dev gcc-13 g++-13 git
RUN useradd -m builder && echo "builder:builder" | chpasswd && adduser builder sudo
USER builder
CMD /bin/bash

RUN cd /tmp && git clone https://github.com/SerenityOS/serenity.git