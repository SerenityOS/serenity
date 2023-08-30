#!/bin/python3

import argparse
import os
import subprocess
import shutil
import sys


def die(message: str) -> None:
    print(f"Error: {message}")
    exit(1)


def TODO() -> None:
    print("TODO")
    exit(1)


def run_command(command: str, stderr=None) -> str:
    return subprocess.getoutput(command).strip()


class Serenity:
    BUILD_DIR = None
    SUPER_BUILD_DIR = None
    SERENITY_SOURCE = ""
    SERENITY_RUN: str = None
    TOOLCHAIN_DIR = ""
    CMAKE_ARGS = []
    CMAKE_PATH = "cmake"

    def __init__(self, args):
        self.args = args

        self.command = args.command
        self.target = args.target
        self.toolchain = args.toolchain
        self.SERENITY_RUN = os.environ.get("SERENITY_RUN", "qemu")

    def run(self):
        self.CMAKE_ARGS.append(f"-DSERENITY_TOOLCHAIN={self.toolchain}")

        self.cmd_with_target()

        if self.command == "recreate" and self.command == "rebuild":
            self.delete_target()

        if self.target != "lagom":
            self.ensure_toolchain()

        self.ensure_target()

        match self.command:
            case "build":
                self.build_target()
            case "install":
                self.build_target()
                self.build_target("install")
            case "image":
                self.lagom_unsupported()
                self.build_target()
                self.build_target("install")
                self.build_image()
            case "copy-src":
                self.lagom_unsupported()
                self.build_target()
                self.build_target("install")
                os.environ["SERENITY_COPY_SOURCE"] = 1
                self.build_image()
            case "run":
                if self.target == "lagom":
                    if len(self.args.args) > 0 and self.args.args[0] == "ladybird":
                        self.build_and_run_lagom_target("run-ladybird")
                    else:
                        self.build_and_run_lagom_target("run-lagom-target")
                else:
                    self.build_target()
                    self.build_target("install")
                    self.build_image()
                    if (len(self.args.args) > 0):
                        os.environ["SERENITY_KERNEL_CMDLINE"] = self.args.args[0]
                    self.build_target("run")
            case "test":
                self.build_target()
                if self.target == "lagom":
                    self.run_tests()

            case _:
                self.build_target(self.command)

    def run_tests(self):
        env = os.environ.copy()
        env["CTEST_OUTPUT_ON_FAILURE"] = "1"
        if (len(self.args.args) > 0):
            subprocess.run("ctest -R " + self.args.args[0], shell=True, cwd=self.BUILD_DIR, env=env)
        else:
            subprocess.run("ctest", shell=True, cwd=self.BUILD_DIR, env=env)

    def build_image(self):
        if self.SERENITY_RUN == "limine":
            self.build_target("limine-image")
        else:
            self.build_target("qemu-image")

    def ensure_target(self):
        if not os.path.exists(f"{self.SUPER_BUILD_DIR}/build.ninja"):
            self.create_build_dir()

    def build_and_run_lagom_target(self, run_target: str):
        lagom_target = ""
        if len(self.args.args) != 0:
            lagom_target = self.args.args[0]
        cmd_args = []
        for arg in self.args.args[1:]:
            cmd_args.append(arg.replace(";", "\\;"))

        lagom_args = ";".join(cmd_args)

        os.environ["LAGOM_TARGET"] = lagom_target
        os.environ["LAGOM_ARGS"] = lagom_args
        self.build_target(run_target)

    def build_target(self, target: str = None):
        if self.target == "lagom":
            cmake_args = f'-S "{self.SERENITY_SOURCE}/Meta/Lagom" -B "{self.BUILD_DIR}" -DBUILD_LAGOM=ON'
            extra_cmake_args = ""
            if len(self.args.args) > 0 and self.args.args[0] == "ladybird":
                extra_cmake_args = "-DENABLE_LAGOM_LADYBIRD=ON"
            os.system(f'{self.CMAKE_PATH} {cmake_args} {extra_cmake_args}')

        make_jobs = None
        try:
            make_jobs = os.environ["MAKEJOBS"]
        except KeyError:
            pass

        if make_jobs is None:
            make_jobs = str(os.cpu_count())

        if target is not None:
            os.system(f'ninja -j {make_jobs} -C "{self.BUILD_DIR}" -- "{target}"')
        else:
            os.environ["CMAKE_BUILD_PARALLEL_LEVEL"] = make_jobs
            os.system(f'{self.CMAKE_PATH} --build "{self.SUPER_BUILD_DIR}"')

    def create_build_dir(self):
        cmake_args = f'-GNinja {" ".join(self.CMAKE_ARGS)} -B "{self.SUPER_BUILD_DIR}"'
        if self.target != "lagom":
            os.system(f'{self.CMAKE_PATH} {cmake_args} -S "{self.SERENITY_SOURCE}/Meta/CMake/Superbuild"')
        else:
            os.system(f'{self.CMAKE_PATH} {cmake_args} -S "{self.SERENITY_SOURCE}/Meta/Lagom"')

    def ensure_toolchain(self):
        cmake_version = run_command(f'{self.CMAKE_PATH} -P "{self.SERENITY_SOURCE}/Meta/CMake/cmake-version.cmake"')
        if int(cmake_version.strip()) != 1:
            self.build_cmake()

        if not os.path.exists(self.TOOLCHAIN_DIR):
            self.build_toolchain()

    def build_cmake(self):
        print("CMake version check failed:")
        if os.name == "nt":
            print("\tWe don't know how to build CMake on Windows yet. Please install CMake manually.")
            sys.exit(1)
        else:
            print("\tBuilding CMake...")
            os.system(f'cd "{self.SERENITY_SOURCE}/Toolchain" && ./BuildCMake.sh')

    def build_toolchain(self):
        print(f"Building toolchain for {self.target} in {self.TOOLCHAIN_DIR}...")
        if self.toolchain == "Clang":
            os.system(f'cd "{self.SERENITY_SOURCE}/Toolchain" && ./BuildClang.sh')
        else:
            os.environ["ARCH"] = self.target
            os.system(f'cd "{self.SERENITY_SOURCE}/Toolchain" && ./BuildIt.sh')

    def delete_target(self):
        if self.BUILD_DIR is not None and os.path.exists(self.BUILD_DIR):
            shutil.rmtree(self.BUILD_DIR)
        if self.SUPER_BUILD_DIR is not None and os.path.exists(self.SUPER_BUILD_DIR):
            shutil.rmtree(self.SUPER_BUILD_DIR)

    def get_top_dir(self) -> str:
        return subprocess.getoutput("git rev-parse --show-toplevel").strip()

    def is_valid_target(self) -> bool:
        if self.target == "aarch64":
            self.CMAKE_ARGS.append("-DSERENITY_ARCH=aarch64")
            return True
        elif self.target == "x86_64":
            self.CMAKE_ARGS.append("-DSERENITY_ARCH=x86_64")
            return True
        elif self.target == "lagom":
            self.CMAKE_ARGS.append("-DBUILD_LAGOM=ON")
            if len(self.args.args) > 0 and self.args.args[0] == "ladybird":
                self.CMAKE_ARGS.append("-DENABLE_LAGOM_LADYBIRD=ON")
            return True
        return False

    def cmd_with_target(self):
        if not self.is_valid_target():
            die(f"Invalid target: {self.target}")

        [CC, CXX] = self.pick_host_compiler()
        self.CMAKE_ARGS.append(f"-DCMAKE_C_COMPILER={CC}")
        self.CMAKE_ARGS.append(f"-DCMAKE_CXX_COMPILER={CXX}")

        if self.SERENITY_SOURCE is None or not os.path.exists(self.SERENITY_SOURCE):
            self.SERENITY_SOURCE = self.get_top_dir()
            os.environ["SERENITY_SOURCE_DIR"] = self.SERENITY_SOURCE

        os.environ["TARGET_TOOLCHAIN"] = ""
        if self.toolchain != "GNU" and self.target != "lagom":
            os.environ["TARGET_TOOLCHAIN"] = os.system(f"echo {self.args.toolchain} | tr '[:upper:]' '[:lower:]'")

        target_toolchain = os.environ["TARGET_TOOLCHAIN"]
        self.BUILD_DIR = f"{self.SERENITY_SOURCE}/Build/{self.target}{target_toolchain}"

        if self.target != "lagom":
            os.environ["SERENITY_ARCH"] = self.target
            os.environ["SERENITY_TOOLCHAIN"] = self.toolchain
            if self.toolchain == "clang":
                self.TOOLCHAIN_DIR = f"{self.SERENITY_SOURCE}/Toolchain/Local/clang"
            else:
                self.TOOLCHAIN_DIR = f"{self.SERENITY_SOURCE}/Toolchain/Local/{target_toolchain}/{self.target}"
            self.SUPER_BUILD_DIR = f"{self.SERENITY_SOURCE}/Build/superbuild-{self.target}{target_toolchain}"
        else:
            self.SUPER_BUILD_DIR = f"{self.BUILD_DIR}"
            self.CMAKE_ARGS.append(f"-DCMAKE_INSTALL_PREFIX={self.SERENITY_SOURCE}/Build/lagom-install")
            self.CMAKE_ARGS.append(f"-DSERENITY_CACHE_DIR={self.SERENITY_SOURCE}/Build/caches")

        # If we have a local cmake, use that instead of the system one
        cmake_path = f"{self.SERENITY_SOURCE}/Toolchain/Local/cmake/bin"
        if os.path.exists(cmake_path):
            if os.name == "nt":
                cmake_path = f"{cmake_path}/cmake.exe"
            else:
                cmake_path = f"{cmake_path}/cmake"
            self.CMAKE_PATH = cmake_path

    def is_supported_compiler(self, path: str) -> bool:
        if path is None:
            return False

        try:
            dump_version = run_command([path, "-dumpversion"], stderr=subprocess.STDOUT)
            version = run_command([path, "--version"], stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError:
            return False

        if dump_version is None:
            return False

        major_version = int(dump_version.split(".")[0])
        if "Apple clang" in version:
            return major_version >= 14
        elif "clang" in version:
            return major_version >= 13
        else:
            return major_version >= 10

    def find_newest_compiler(self, compilers: [str]) -> str:
        best_version = 0
        best_compiler = None

        for compiler in compilers:
            try:
                dump_version = run_command([compiler, "-dumpversion"], stderr=subprocess.STDOUT)
            except Exception:
                continue

            if dump_version is None:
                continue

            try:
                major_version = int(dump_version.split(".")[0])
            except ValueError:
                continue

            if major_version > best_version:
                best_version = major_version
                best_compiler = compiler

        return best_compiler

    def pick_host_compiler(self) -> [str, str]:
        if self.is_supported_compiler(os.environ.get("CC")) and self.is_supported_compiler(os.environ.get("CXX")):
            return [os.environ.get("CC"), os.environ.get("CXX")]

        supported_clang = ["clang", "clang-13", "clang-14", "clang-15", "/opt/homebrew/opt/llvm/bin/clang"]
        host_compiler = self.find_newest_compiler(supported_clang)
        if self.is_supported_compiler(host_compiler):
            return [host_compiler, host_compiler.replace("clang", "clang++")]

        supported_gcc = ["egcc", "gcc", "gcc-12", "/usr/local/bin/gcc-12", "/opt/homebrew/bin/gcc-12"]
        host_compiler = self.find_newest_compiler(supported_gcc)
        if self.is_supported_compiler(host_compiler):
            return [host_compiler, host_compiler.replace("gcc", "g++")]

        die("Could not find a supported compiler. Please install clang 13 or newer, or gcc 10 or newer.")


def main():
    name = os.path.basename(__file__)
    epilog = f"""Examples:
    {name} run x86_64 GNU smp=on
        Runs the image in QEMU passing \"smp=on\" to the kernel command line
    {name} run x86_64 GNU 'init=/bin/UserspaceEmulator init_args=/bin/SystemServer'
        Runs the image in QEMU, and run the entire system through UserspaceEmulator (not fully supported yet)
    {name} run
        Runs the image for the default TARGET x86_64 in QEMU
    {name} run lagom js -A
        Runs the Lagom-built js(1) REPL
    {name} test lagom
        Runs the unit tests on the build host
    {name} kaddr2line x86_64 0x12345678
        Resolves the address 0x12345678 in the Kernel binary
    {name} addr2line x86_64 WindowServer 0x12345678
        Resolves the address 0x12345678 in the WindowServer binary
    {name} gdb x86_64 smp=on -ex 'hb *init'
        Runs the image for the TARGET x86_64 in qemu and attaches a gdb session
        setting a breakpoint at the init() function in the Kernel."""
    parser = argparse.ArgumentParser(
        description="Serenity Build & Run script",
        epilog=epilog,
        formatter_class=argparse.RawTextHelpFormatter,
    )

    command_choices = [
        "build",
        "install",
        "image",
        "run",
        "gdb",
        "test",
        "delete",
        "recreate",
        "rebuild",
        "kaddr2line",
        "addr2line",
        "rebuild-toolchain",
        "rebuild-world",
        "copy-src",
    ]
    parser.add_argument(
        "command",
        type=str,
        choices=command_choices,
        help="Supported commands:\n\t " + ", ".join(command_choices),
        metavar="command",
    )

    target_choices = ["aarch64", "x86_64", "lagom"]
    parser.add_argument(
        "target",
        type=str,
        choices=target_choices,
        help="Supported targets: " + ", ".join(target_choices),
        metavar="target",
        default=os.environ.get("SERENITY_ARCH") or "x86_64",
        nargs="?",
    )

    parser.add_argument(
        "args", type=str, nargs="*", help="The arguments to pass to the command"
    )

    group = parser.add_mutually_exclusive_group()
    group.add_argument(
        "--toolchain",
        type=str,
        help="Specify the toolchain to use",
        nargs="?",
        default=os.environ.get("SERENITY_TOOLCHAIN") or "GNU",
    )

    args = parser.parse_args()
    Serenity(args).run()


if __name__ == "__main__":
    main()
