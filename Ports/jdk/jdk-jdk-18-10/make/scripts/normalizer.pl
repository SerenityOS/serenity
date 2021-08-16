#!/usr/bin/perl

#
# Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

#
# Parses java files:
#   1. Removes from the end of lines spaces and TABs
#   2. Replaces TABs by spaces
#   3. Replaces all NewLine separators by Unix NewLine separators
#   4. Makes one and only one empty line at the end of each file

if ($#ARGV < 0) {
    &usage;

    die;
}

use Cwd 'abs_path';

my @extensions = ("java");

# Read options
my $dirpos = 0;

while ($dirpos < $#ARGV) {
    if ($ARGV[$dirpos] eq "-e") {
        @extensions = split(/,/, $ARGV[$dirpos + 1]);
    } else {
        last;
    }

    $dirpos += 2;
}

if ($dirpos > $#ARGV) {
    &usage;

    die;
}

use Cwd;
my $currdir = getcwd;

my $allfiles = 0;

my $filecount = 0;

my @tabvalues;

# Init tabvalues
push (@tabvalues, " ");

for (my $i = 1; $i < 8; $i++) {
    push(@tabvalues, $tabvalues[$i - 1] . " ");
}

open(FILELIST, ">$currdir/filelist") or die "Failed while open $currdir/filelist: $!\n";

while ($dirpos <= $#ARGV) {
    use File::Find;

    find(\&parse_file, abs_path($ARGV[$dirpos]));

    $dirpos += 1;
}

close(FILELIST);

use Cwd 'chdir';
chdir $currdir;

print "Checked $allfiles file(s)\n";
print "Modified $filecount file(s)\n";
print "See results in the file $currdir/filelist\n";

sub parse_file {
    my $filename = $File::Find::name;

    # Skip directories
    return if -d;

    # Skip SCCS files
    return if ($filename =~ /\/SCCS\//);

    # Skip files with invalid extensions
    my $accepted = 0;
    foreach my $ext (@extensions) {
        if ($_ =~ /\.$ext$/i) {
            $accepted = 1;

            last;
        }
    }
    return if ($accepted == 0);

    use File::Basename;
    my $dirname = dirname($filename);

    use Cwd 'chdir';
    chdir $dirname;

    open(FILE, $filename) or die "Failed while open $filename: $!\n";

    # Read file
    my @content;
    my $line;
    my $emptylinescount = 0;
    my $modified = 0;

    while ($line = <FILE>) {
        my $originalline = $line;

        # Process line

        # Remove from the end of the line spaces and return character
        while ($line =~ /\s$/) {
            chop($line);
        }

        # Replace TABs
        for (my $i = 0; $i < length($line); $i++) {
            if (substr($line, $i, 1) =~ /\t/) {
                $line = substr($line, 0, $i) . $tabvalues[7 - ($i % 8)] . substr($line, $i + 1);
            }
        }

        if (length($line) == 0) {
            $emptylinescount++;
        } else {
            while ($emptylinescount > 0) {
                push(@content, "");

                $emptylinescount--;
            }

            push(@content, $line);
        }

        if ($originalline ne ($line . "\n")) {
            $modified = 1;
        }

    }

    $allfiles++;

    if ($emptylinescount > 0) {
        $modified = 1;
    }

    close(FILE);

    if ($modified != 0) {
        # Write file
        open(FILE, ">$filename") or die "Failed while open $filename: $!\n";

        for (my $i = 0; $i <= $#content; $i++) {
            print FILE "$content[$i]\n";
        }

        close(FILE);

        # Print name from current dir
        if (index($filename, $currdir) == 0) {
           print FILELIST substr($filename, length($currdir) + 1);
        } else {
           print FILELIST $filename;
        }
        print FILELIST "\n";

        $filecount++;

        print "$filename: modified\n";
    }
}

sub usage {
    print "Usage:\n";
    print "  normalizer.pl [-options] <dir> [dir2 dir3 ...]\n";
    print "  Available options:\n";
    print "    -e    comma separated files extensions. By default accepts only java files\n";
    print "\n";
    print "Examples:\n";
    print "  normalizer.pl -e c,cpp,h,hpp .\n";
}
