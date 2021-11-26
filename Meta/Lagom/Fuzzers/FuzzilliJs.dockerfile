# Build the image:
# $ podman build \
#       --tag fuzzillijs \
#       -f ./FuzzilliJs.dockerfile
# Run the container:
# $ podman run \
#       -it --rm \
#       -v ./path/to/fuzzilli-storage:/home/fuzzilli-storage:Z \
#       localhost/fuzzillijs
# To pass more options to fuzzilli, e.g. '--resume' (use '--help' to see all options):
# $ podman run \
#       -it --rm \
#       -v ./path/to/fuzzilli-storage:/home/fuzzilli-storage:Z \
#       -e FUZZILLI_CLI_OPTIONS='--resume' \
#       localhost/fuzzillijs
# Invocations with `docker` should be similar or even identical.
# NB: There are Dockerfiles & build scripts available for Fuzzilli-supported JS engines,
# but this doesn't use the same approach - that would require a fair amount of patching
# which is not worth it, unless we plan to add LibJS support to Fuzzilli upstream.

FROM fedora:33 AS serenity-build

WORKDIR /home
RUN dnf install -y clang cmake git-core ninja-build
RUN git clone --depth=1 https://github.com/SerenityOS/serenity
RUN mkdir /home/serenity/Build

WORKDIR /home/serenity/Build
RUN sed -i 's/-Wmissing-declarations //' ../CMakeLists.txt

# In file included from ../Libraries/LibGfx/Font.cpp:37:
# ../Libraries/LibCore/FileStream.h:96:5: error: explicitly defaulted default constructor is implicitly deleted [-Werror,-Wdefaulted-function-deleted]
#     InputFileStream() = default;
#     ^
# -------------------------------------------------------------------
# I have no idea how to fix this, so I'll allow it. It's not relevant
# as LibJS doesn't use LibGfx; but I suppose Lagom builds it anyway.
# ¯\_(ツ)_/¯
RUN CXXFLAGS="-Wno-defaulted-function-deleted" \
    cmake -GNinja \
          -DBUILD_LAGOM=ON \
          -DENABLE_FUZZER_SANITIZER=ON \
          -DCMAKE_C_COMPILER=clang \
          -DCMAKE_CXX_COMPILER=clang++ \
          ..
RUN ninja FuzzilliJs


FROM fedora:33 AS fuzzilli-build

WORKDIR /home
RUN dnf install -y git-core patch swift-lang
RUN git clone --depth=1 https://github.com/googleprojectzero/fuzzilli

WORKDIR /home/fuzzilli
COPY --from=serenity-build /home/serenity/Meta/Lagom/Fuzzers/add-serenity-support-to-fuzzilli.patch .
RUN patch -p1 < add-serenity-support-to-fuzzilli.patch
RUN swift build -c release


FROM fedora:33

WORKDIR /home
# This is unfortunate, but we need libswiftCore.so (and possibly other files) from the
# Swift runtime. The "swift-lang-runtime" package doesn't seem to exist in Fedora 33 :/
RUN dnf install -y swift-lang
COPY --from=serenity-build /home/serenity/Build/Meta/Lagom/Fuzzers/FuzzilliJs .
COPY --from=fuzzilli-build /home/fuzzilli/.build/x86_64-unknown-linux-gnu/release/FuzzilliCli .
RUN mkdir fuzzilli-storage
ENV FUZZILLI_CLI_OPTIONS ""
CMD [ "sh", "-c", "./FuzzilliCli --profile=serenity --storagePath=fuzzilli-storage ${FUZZILLI_CLI_OPTIONS} ./FuzzilliJs" ]
