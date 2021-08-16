#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# Compiled  Size  Type Method
#       45    131    1 sun/misc/FloatingDecimal countBits

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^Compiled  Size  Type Method$/	{
	    headerlines++;
	}

/^[ ]*[0-9]+[ ]*[0-9]+[ ]*[0-9]+[ ]*[a-zA-Z_\$\+]+[a-zA-Z0-9_\$\+\/]* [a-zA-Z_\$\+]+[a-zA-Z0-9_\$\+]*$/	{
	    datalines++;
	}

/^[ ]*[0-9]+[ ]*[0-9]+[ ]*[0-9]+[ ]*[a-zA-Z_\$\+]+[a-zA-Z0-9_\$\+\/]* <init>$/	{
	    datalines++;
	}

	{ totallines++; print $0 }

END	{
	    if ((headerlines == 1) && (datalines == 1)) {
	        exit 0
	    }
	    else {
	        exit 1
	    }
	}
