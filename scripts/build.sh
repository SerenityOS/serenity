# For building on Unix-based systems
cd Toolchain
./BuildIt.sh

cd ../Build/i686

cmake ../.. -G Ninja
ninja install

ninja image
