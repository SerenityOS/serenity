#
BEGIN	{ totallines=0; count=0; }

	{ totallines++; print $0 }

$0 !~ /[java|jdk|sun|com\.sun]\..+=.*$/	{ count++ }

END	{
	    if ((count == 0) && (totallines != 0)) {
	        exit 0
	    }
	    else {
	        exit 1
	    }
	}
