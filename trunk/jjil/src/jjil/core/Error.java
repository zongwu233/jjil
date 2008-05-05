/**
 * Error defines a common error-reporting mechanism for all JJIL classes.
 * It includes an error code and up to three string objects representing
 * objects that explain the error, for example file names or images.
 * 
 * Build-specific libraries like jjil.android or jjil.j2se will define
 * a Error.toString() class that converts the Error object into a localized
 * error message.
 * 
 * 
 */
package jjil.core;

/**
 * @author webb
 *
 */
public class Error extends Throwable {
	
        /**
         * J2ME's Java is only 1.4 so no enums. We must simulate them...
         */
	public static class PACKAGE {
            public static final int ALGORITHM = 0;
            public static final int ANDROID = ALGORITHM + 1;
            public static final int CORE = ANDROID + 1;
            public static final int J2ME = CORE + 1;
            public static final int J2SE = J2ME + 1;
            
            public static final int COUNT = J2SE + 1;
	}
	
	
	/**
	 * nCode is a general error code. Possible values are defined in the CODES enumerated
	 * type (really, we use ints for compatibility with J2ME).
	 */
	private int nCode;
	
	/**
	 * The package where the error code is defined.
	 */
	private int nPackage;
	
	
	/**
	 * szParam1 is a primary parameter giving detailed error information.
	 */
	private String szParam1;
	/**
	 * szParam2 is a secondary parameter giving detailed error information.
	 */
	private String szParam2;
	/**
	 * szParam3 is a tertiary parameter giving detailed error information.
	 */
	private String szParam3;
        
        /**
         * Copy constructor.
         */
        public Error(Error e) {
            this.nPackage = e.getPackage();
            this.nCode = e.getCode();
            this.szParam1 = e.getParam1();
            this.szParam2 = e.getParam2();
            this.szParam3 = e.getParam3();
        }
	
	/**
	 * This is how Error objects are created. The first two parameters determine
	 * the specific type of error. The other parameters give information about
	 * the objects causing the error.
	 * @param nCode: the error code
	 * @param szParam1: a first parameter giving detailed information
	 * @param szParam2: a second parameter giving detailed information
	 * @param szParam3: a third parameter giving detailed information
	 */
	public Error(
			int nPackage,
			int nCode, 
			String szParam1, 
			String szParam2, 
			String szParam3) {
		this.nPackage = nPackage;
		this.nCode = nCode;
		this.szParam1 = szParam1;
		this.szParam2 = szParam2;
		this.szParam3 = szParam3;
	}

	public int getCode() {
		return this.nCode;
	}
	
	public int getPackage() {
		return this.nPackage;
	}
	
	public String getParam1() {
		return this.szParam1;
	}
	
	public String getParam2() {
		return this.szParam2;
	}
	
	public String getParam3() {
		return this.szParam3;
	}
        
    protected String Parameters() {
        String sz = "(";
        if (this.getParam1() != null) {
            sz += this.getParam1();
        }
        sz += ",";
        if (this.getParam2() != null) {
            sz += this.getParam2();
        }
        sz += ",";
        if (this.getParam3() != null) {
            sz += this.getParam3();
        }
        sz += ")";
        return sz;
    }

    public String toString() {
            return new Integer(this.nPackage).toString() + " " +
                    new Integer(this.nCode).toString() + 
                    Parameters();
        }
}
