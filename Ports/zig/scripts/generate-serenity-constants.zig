const std = @import("std");

const SerenityIncludes = @cImport({
    @cInclude("time.h");

    @cInclude("bits/pthread_integration.h");
    @cInclude("dirent.h");
    @cInclude("errno_codes.h");
    @cInclude("fcntl.h");
    @cInclude("limits.h");
    @cInclude("link.h");
    @cInclude("poll.h");
    @cInclude("semaphore.h");
    @cInclude("sys/file.h");
    @cInclude("sys/mman.h");
    @cInclude("sys/stat.h");
    @cInclude("sys/types.h");
    @cInclude("sys/wait.h");
    @cInclude("unistd.h");
    @cInclude("stdio.h");
});

const constant_file = @embedFile("./constants.txt");
const constants = blk: {
    @setEvalBranchQuota(10000);

    var constant_list: []const []const u8 = &.{};
    var constant_iterator = std.mem.tokenize(u8, constant_file, "\n");
    while (constant_iterator.next()) |constant| {
        constant_list = constant_list ++ &[_][]const u8{constant};
    }

    break :blk constant_list;
};

pub fn main() !void {
    const writer = std.io.getStdOut().writer();

    var arena = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    defer arena.deinit();
    const allocator = arena.allocator();

    inline for (constants) |constant| {
        const value = @field(SerenityIncludes, constant);
        const decl = try std.fmt.allocPrint(allocator, "pub const " ++ constant ++ " = {d};\n", .{value});
        defer allocator.free(decl);

        try writer.writeAll(decl);
    }

    // XXX: Serenity's MAP_FAILED constant confuses Zig.
    try writer.writeAll("pub const MAP_FAILED = @intToPtr(*anyopaque, @bitCast(usize, @as(isize, -1)));\n");
}
