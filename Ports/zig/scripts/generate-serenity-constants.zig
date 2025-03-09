const std = @import("std");

const serenity_includes = @cImport({
    @cInclude("bits/pthread_integration.h");
    @cInclude("dirent.h");
    @cInclude("errno_codes.h");
    @cInclude("fcntl.h");
    @cInclude("limits.h");
    @cInclude("link.h");
    @cInclude("poll.h");
    @cInclude("semaphore.h");
    @cInclude("stdio.h");
    @cInclude("sys/file.h");
    @cInclude("sys/mman.h");
    @cInclude("sys/socket.h");
    @cInclude("sys/stat.h");
    @cInclude("sys/types.h");
    @cInclude("sys/uio.h");
    @cInclude("sys/wait.h");
    @cInclude("time.h");
    @cInclude("unistd.h");
});

const constant_file = @embedFile("./constants.txt");
const constants = blk: {
    @setEvalBranchQuota(10000);

    var constant_list: []const []const u8 = &.{};
    var constant_iterator = std.mem.tokenizeScalar(u8, constant_file, '\n');
    while (constant_iterator.next()) |constant| {
        constant_list = constant_list ++ &[_][]const u8{constant};
    }

    break :blk constant_list;
};

pub fn main() !void {
    const writer = std.io.getStdOut().writer();
    inline for (constants) |constant| {
        const value = comptime @field(serenity_includes, constant);
        try writer.writeAll(std.fmt.comptimePrint("pub const {s} = {d};\n", .{ constant, value }));
    }
}
