🧠 GhostShift OS

A modern, open-source operating system inspired by SerenityOS — built from scratch with a focus on speed, modularity, and learning.

🚀 Overview

GhostShift OS is a lightweight, UNIX-like operating system designed for learning, experimentation, and creativity.
It aims to combine modern design principles with a classic kernel architecture, enabling developers to explore every layer of a full system — from low-level hardware to high-level user interfaces.

🧩 Key Features

🧱 Monolithic Kernel — simple, fast, and modular by design.

🖥️ Custom Build Toolchain — cross-compilation using GCC and Binutils.

🧮 Lagom Libraries — portable versions of core system libraries for development.

🧰 Modern C++23 Codebase — clean, type-safe, and standards-compliant.

🔧 CMake + Ninja Build System — fast, parallelized builds.

🧊 QEMU Virtualization — test your OS safely in a virtual machine.

⚙️ System Requirements
Component	Minimum	Recommended
CPU	Dual-Core	4+ Cores
RAM	2 GB	4 GB or higher
Disk Space	10 GB	20 GB
Platform	Linux (Ubuntu preferred)	Ubuntu 22.04+ or WSL2
🛠️ Build Instructions
1️⃣ Install Dependencies
sudo apt update
sudo apt install -y build-essential cmake ninja-build qemu-system-x86 g++-multilib libgmp-dev libmpfr-dev libmpc-dev texinfo git curl unzip ccache

2️⃣ Clone the Repository
git clone https://github.com/ayu-haker/GhostShift-OS.git
cd GhostShift-OS

3️⃣ Build the Toolchain
Meta/serenity.sh rebuild-toolchain

4️⃣ Build the OS
Meta/serenity.sh build

5️⃣ Run GhostShift in QEMU
Meta/serenity.sh run

📁 Project Structure
GhostShift-OS/
├── AK/                # Base utilities and data structures
├── Kernel/            # Core kernel components
├── Userland/          # User-space applications and libraries
├── Meta/              # Build scripts, tools, and configuration
├── Toolchain/         # Cross-compilation toolchain
└── Build/             # Output and build artifacts

💡 Goals

Build an educational operating system from scratch.

Understand the internals of compilers, linkers, and kernels.

Explore modern C++ features in systems programming.

Encourage open-source contributions and experimentation.

🤝 Contributing

Contributions are always welcome!
If you find a bug, want to add a feature, or improve documentation:

Fork the repository

Create a feature branch

Submit a pull request

📜 License

This project is licensed under the MIT License — feel free to use, modify, and distribute with attribution.

👨‍💻 Author

Ayushman Bosu Roy
“Learning by breaking and building again.” ⚙️
