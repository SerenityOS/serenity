#! /bin/sh -f
#
# Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
# This script checks a copyright notice.
#
# The script should be located in the main jdk repository under make/scripts.
# It works with the templates in the make/data/license-templates directory of the jdk source.
#
# Usage: "lic_check.sh [-gpl] or [-gplcp] or [-bsd] file(s)"

script_directory=`dirname $0`
script_name=`basename $0`
first_option=$1

# parse the first argument

case "$1" in
	"-gpl")
		header="gpl-header"
		;;
	"-gplcp")
		header="gpl-cp-header"
		;;
	"-bsd")
		header="bsd-header"
                ;;
	*)
		echo "Usage: $0 [-gpl] or [-gplcp] or [-bsd] file(s)" 1>&2
		exit 1
		;;
esac
shift

#initialize error status
error_status=0

# determine and set the absolute path for the script directory
D=`dirname "${script_directory}"`
B=`basename "${script_directory}"`
script_dir="`cd \"${D}\" 2>/dev/null && pwd || echo \"${D}\"`/${B}"

# set up a variable for the template directory
template_dir=${script_dir}/../data/license-templates

# Check existence of the template directory.
if [ ! -d ${template_dir} ] ; then
        echo "ERROR: The template directory "${template_dir}" doesn't exist." 1>&2
        exit 1
fi

# set the temporary file location
tmpfile=/tmp/source_file.$$
rm -f ${tmpfile}

# check number of lines in the template file
lines=`cat ${template_dir}/${header} | wc -l`

# the template file has one empty line at the end, we need to ignore it
lines=`expr ${lines} - 1`

# A loop through the all script parameters:
#
# 1. Given a set of source files and a license template header, read a file name of each source file.
# 2. Check if a given file exists. When a directory is encountered, dive in and process all sources in those directories.
# 3. Read each line of the given file and check it for a copyright string.
# 4. If a copyright string found, check the correctness of the years format in the string and replace years with %YEARS%.
# 5. Continue reading the file until the number of lines is equal to the length of the license template header ($lines) and remove a comment prefix for each line.
# 6. Store the result (the license header from a given file) into a temporary file.
# 7. If a temporary file is not empty, compare it with a template file to verify if the license text is the same as in a template.
# 8. Produce a error in case a temporary file is empty, it means we didn't find a copyright string, or it's not correct
#
while [ "$#" -gt "0" ] ; do
	touch ${tmpfile}

	# In case of the directory as a parameter check recursively every file inside.
	if [ -d $1 ] ; then
		curdir=`pwd`
		cd $1
		echo "*** Entering directory: "`pwd`
		echo "***"
		files=`ls .`
		sh ${script_dir}/${script_name} ${first_option} ${files}
		status=$?
		if [ ${error_status} -ne 1 ] ; then
			error_status=${status}
		fi
		cd ${curdir}
		shift
		continue
	else
		echo "### Checking copyright notice in the file: "$1
		echo "###"
	fi

	# Check the existence of the source file.
	if [ ! -f $1 ] ; then
        	echo "ERROR: The source file "$1" doesn't exist." 1>&2
		error_status=1
		shift
        	continue
	fi

	# read the source file and determine where the header starts, then get license header without prefix
	counter=0
	while read line ; do
		# remove windows "line feed" character from the line (if any)
		line=`echo "${line}" | tr -d '\r'`
		# check if the given line contains copyright
		check_copyright=`echo "${line}" | grep "Copyright (c) "`
		if [ "${check_copyright}" != "" ] ; then
			# determine the comment prefix
			prefix=`echo "${line}" | cut -d "C" -f 1`
			# remove prefix (we use "_" as a sed delimiter, since the prefix could be like //)
			copyright_without_prefix=`echo "${line}" | sed s_"^${prefix}"__g`
			# copyright years
			year1=`echo "${copyright_without_prefix}" | cut -d " " -f 3`
			year2=`echo "${copyright_without_prefix}" | cut -d " " -f 4`
			# Processing the first year in the copyright string
			length=`expr "${year1}" : '.*'`
			if [ ${length} -ne 5 ] ; then
        			break
			fi
			check_year1=`echo ${year1} | egrep "19[0-9][0-9],|2[0-9][0-9][0-9],"`
			if [ "${check_year1}" = "" ] ; then
        			break
			fi
			# Processing the second year in the copyright string
			if [ "${year2}" != "Oracle" ] ; then
        			length=`expr "${year2}" : '.*'`
        			if [ ${length} -ne 5 ] ; then
                			break
        			else
                			check_year2=`echo ${year2} | egrep "19[0-9][0-9],|2[0-9][0-9][0-9],"`
                			if [ "${check_year2}" = "" ] ; then
                        			break
                			fi
        			fi
			fi

			# copyright string without copyright years
			no_years=`echo "${copyright_without_prefix}" | sed 's/[0-9,]*//g'`
			# copyright string before years
			before_years=`echo "${no_years}" | cut -d "O" -f 1`
			# copyright string after years
			after_years=`echo "${no_years}" | cut -d ")" -f 2`
			# form a new copyright string with %YEARS%
			new_copyright=`echo ${before_years}"%YEARS%"${after_years}`
			# save the new copyright string to a file
			echo "${new_copyright}" > ${tmpfile}
			# start counting the lines
                       	counter=1
			# move to the next line
			continue
		fi
		if [ ${counter} -ne 0 ] ; then
			# this should be a license header line, hence increment counter
			counter=`expr ${counter} + 1`
			# record a string without a prefix to a file
			newline=`echo "${line}" | sed s_"^${prefix}"__`

			# we need to take care of the empty lines in the header, i.e. check the prefix without spaces
			trimmed_prefix=`echo "${prefix}" | tr -d " "`
			trimmed_line=`echo "${line}"  | tr -d " "`
			if [ "${trimmed_line}" = "${trimmed_prefix}" ] ; then
				echo "" >> ${tmpfile}
			else
				echo "${newline}" >> ${tmpfile}
			fi
		fi
		# stop reading lines when a license header ends and add an empty line to the end
		if [ ${counter} -eq ${lines} ] ; then
			echo "" >> ${tmpfile}
			break
		fi
	done < $1

	# compare the license header with a template file
	if [ -s ${tmpfile} ] ; then
		diff -c ${tmpfile} ${template_dir}/${header} 1>&2
		if [ "$?" = "0" ] ; then
			echo "SUCCESS: The license header for "`pwd`"/"$1" has been verified."
			echo "###"
		else
			echo "ERROR: License header is not correct in "`pwd`"/"$1 1>&2
			echo "See diffs above. " 1>&2
			echo "###" 1>&2
			echo "" 1>&2
			error_status=1
		fi
	else
		# If we don't have a temporary file, there is a problem with a copyright string (or no copyright string)
		echo "ERROR: Copyright string is not correct or missing in "`pwd`"/"$1 1>&2
		echo "###" 1>&2
		echo "" 1>&2
		error_status=1
	fi
	rm -f ${tmpfile}
	shift
done
if [ ${error_status} -ne 0 ] ; then
	exit 1
fi
