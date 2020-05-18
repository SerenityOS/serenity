/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>

#include "FTPRequest.h"

namespace FTP {

#define __ENUMERATE_FTP_COMMAND

#define ENUMERATE_FTP_COMMANDS                             \
    __ENUMERATE_FTP_COMMAND(AbortFileTransfer, "ABOR")     \
    __ENUMERATE_FTP_COMMAND(ChangeWorkingDirectory, "CWD") \
    __ENUMERATE_FTP_COMMAND(Delete, "DELE")                \
    __ENUMERATE_FTP_COMMAND(ListFiles, "LIST")             \
    __ENUMERATE_FTP_COMMAND(MakeDirectory, "MKD")          \
    __ENUMERATE_FTP_COMMAND(ModifyTimeFile, "MDTM")        \
    __ENUMERATE_FTP_COMMAND(TransferMode, "MODE")          \
    __ENUMERATE_FTP_COMMAND(Password, "PASS")              \
    __ENUMERATE_FTP_COMMAND(Port, "PORT")                  \
    __ENUMERATE_FTP_COMMAND(PrintWorkingDirectory, "PWD")  \
    __ENUMERATE_FTP_COMMAND(QuitConnection, "QUIT")        \
    __ENUMERATE_FTP_COMMAND(RemoveDirectory, "RMD")        \
    __ENUMERATE_FTP_COMMAND(RenameFileFrom, "RNFR")        \
    __ENUMERATE_FTP_COMMAND(RenameFileTo, "RNTO")          \
    __ENUMERATE_FTP_COMMAND(RetrieveFile, "RETR")          \
    __ENUMERATE_FTP_COMMAND(SizeFile, "SIZE")              \
    __ENUMERATE_FTP_COMMAND(StoreFile, "STOR")             \
    __ENUMERATE_FTP_COMMAND(TransferType, "TYPE")          \
    __ENUMERATE_FTP_COMMAND(Username, "USER")

#undef __ENUMERATE_FTP_COMMAND

struct FtpCommand {
    String log_name;
    String ftp_name;
};

FtpCommand command_to_string(FTPRequest::Command command)
{
#define __ENUMERATE_FTP_COMMAND(cmd, str) \
    case FTPRequest::Command::cmd:        \
        return FtpCommand{ #cmd, str };
    switch (command) {
        ENUMERATE_FTP_COMMANDS
    default:
        return FtpCommand{ "Unknown", "NOOP" };
    }
#undef __ENUMERATE_FTP_COMMAND
}

FTPRequest::FTPRequest()
{
}

FTPRequest::~FTPRequest()
{
}

void FTPRequest::set_command(Command command)
{
    m_command = command;
    m_args.clear();
}
void FTPRequest::add_arg(const String& arg)
{
    m_args.append(arg);
}

ByteBuffer FTPRequest::to_raw_request() const
{
    String command_name = command_to_string(m_command).ftp_name;
    StringBuilder builder;
    builder.append(command_name);
    builder.append(" ");
    for (auto& arg : m_args) {
        builder.append(arg);
        builder.append(" ");
    }
    dbg() << "FTPRequest::to_raw_request(): " << command_to_string(m_command).log_name << ": " << builder.to_string();
    return builder.to_byte_buffer();
}
}
