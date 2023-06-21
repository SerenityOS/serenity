static int g_pid = -1;

static void handle_sigint(int)
{
    if (g_pid == -1)
        return;

    if (ptrace(PT_DETACH, g_pid, 0, 0) == -1) {
        perror("detach");
    }
}

int main(int argc, char** argv)
{
    if (pledge("stdio wpath cpath proc exec ptrace sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> child_argv;

    const char* output_filename = nullptr;
    auto trace_file_or_error = Core::File::standard_error();
    if (trace_file_or_error.is_error()) {
        outln(stderr, "Failed to open stderr: {}", trace_file_or_error.error());
        return 1;
    }
    auto trace_file = trace_file_or_error.release_value();

    Core::ArgsParser parser;
    parser.set_general_help(
        "Trace all syscalls and their result.");
    parser.add_option(g_pid, "Trace the given PID", "pid", 'p', "pid");
    parser.add_option(output_filename, "Filename to write output to", "output", 'o', "output");
    parser.add_positional_argument(child_argv, "Arguments to exec", "argument", Core::ArgsParser::Required::No);

    parser.parse(argc, argv);

    if (output_filename != nullptr) {
        auto open_result = Core::File::open(output_filename, Core::File::OpenMode::Write);
        if (open_result.is_error()) {
            outln(stderr, "Failed to open output file: {}", open_result.error());
            return 1;
        }
        trace_file = open_result.release_value();
    }

    if (pledge("stdio proc exec ptrace sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    int status;
    if (g_pid == -1) {
        if (child_argv.is_empty()) {
            outln(stderr, "strace: Expected either a pid or some arguments\n");
            return 1;
        }

        child_argv.append(nullptr);
        int pid = fork();
        if (pid < 0) {
            perror("fork");
            return 1;
        }

        if (!pid) {
            if (ptrace(PT_TRACE_ME, 0, 0, 0) == -1) {
                perror("traceme");
                return 1;
            }
            int rc = execvp(child_argv.first(), const_cast<char**>(child_argv.data()));
            if (rc < 0) {
                perror("execvp");
                exit(1);
            }
            VERIFY_NOT_REACHED();
        }

        g_pid = pid;
        if (waitpid(pid, &status, WSTOPPED | WEXITED) != pid || !WIFSTOPPED(status)) {
            perror("waitpid");
            return 1;
        }
    }

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    if (ptrace(PT_ATTACH, g_pid, 0, 0) == -1) {
        perror("attach");
        return 1;
    }
    if (waitpid(g_pid, &status, WSTOPPED | WEXITED) != g_pid || !WIFSTOPPED(status)) {
        perror("waitpid");
        return 1;
    }

    for (;;) {
        if (ptrace(PT_SYSCALL, g_pid, 0, 0) == -1) {
            perror("syscall");
            return 1;
        }
        if (waitpid(g_pid, &status, WSTOPPED | WEXITED) != g_pid || !WIFSTOPPED(status)) {
            perror("wait_pid");
            return 1;
        }
        PtraceRegisters regs = {};
        if (ptrace(PT_GETREGS, g_pid, &regs, 0) == -1) {
            perror("getregs");
            return 1;
        }
        u32 syscall_index = regs.eax;
        u32 arg1 = regs.edx;
        u32 arg2 = regs.ecx;
        u32 arg3 = regs.ebx;

        if (ptrace(PT_SYSCALL, g_pid, 0, 0) == -1) {
            perror("syscall");
            return 1;
        }
        if (waitpid(g_pid, &status, WSTOPPED | WEXITED) != g_pid || !WIFSTOPPED(status)) {
            perror("wait_pid");
            return 1;
        }

        if (ptrace(PT_GETREGS, g_pid, &regs, 0) == -1) {
            perror("getregs");
            return 1;
        }

        u32 res = regs.eax;

        auto string = String::formatted("{}({:#08x}, {:#08x}, {:#08x})\t={}\n",
            Syscall::to_string((Syscall::Function)syscall_index),
            arg1,
            arg2,
            arg3,
            res);

        auto result = trace_file->write_value(string);
        if (result.is_error()) {
            warnln("write: {}", result->error());
            return 1;
        }
    }

    return 0;
}
