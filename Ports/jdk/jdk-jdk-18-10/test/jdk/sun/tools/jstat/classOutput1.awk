#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# Loaded  Bytes  Unloaded  Bytes     Time   
#    407   436.3        0     0.0       0.36

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^Loaded  Bytes  Unloaded  Bytes     Time   $/	{
	    headerlines++;
	}

/^[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+$/	{
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
