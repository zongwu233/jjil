package jjil.j2me;

/**
 * This is the class used to create Strings from Error objects when running
 * under J2ME. The point is that the J2ME localization support is used so
 * the same approach won't work with Android, J2SE, etc.<br>
 * Usage:<br>
 * 		jjil.j2me.Error eJ2me = new jjil.j2me.Error(e);<br>
 * 	 	... eJ2me.toString() ...
 * @author webb
 *
 */
public class Error extends jjil.core.Error {
    private static final String szMessage[][] = new String[jjil.core.Error.PACKAGE.COUNT][];
    
    {
        Error.szMessage[jjil.core.Error.PACKAGE.CORE] = new String[jjil.core.ErrorCodes.COUNT];
        Error.szMessage[jjil.core.Error.PACKAGE.CORE][jjil.core.ErrorCodes.IMAGE_MASK_SIZE_MISMATCH] =
            LS.getMessage("IMAGE_MASK_SIZE_MISMATCH");
        Error.szMessage[jjil.core.Error.PACKAGE.CORE][jjil.core.ErrorCodes.MATH_DIVISION_ZERO] =
            LS.getMessage("MATH_DIVISION_ZERO");
        Error.szMessage[jjil.core.Error.PACKAGE.CORE][jjil.core.ErrorCodes.MATH_NEGATIVE_SQRT] =
            LS.getMessage("MATH_NEGATIVE_SQRT");
        Error.szMessage[jjil.core.Error.PACKAGE.CORE][jjil.core.ErrorCodes.MATH_PRODUCT_TOO_LARGE] =
            LS.getMessage("MATH_PRODUCT_TOO_LARGE");
        Error.szMessage[jjil.core.Error.PACKAGE.CORE][jjil.core.ErrorCodes.MATH_SQUARE_TOO_LARGE] =
            LS.getMessage("MATH_SQUARE_TOO_LARGE");
        Error.szMessage[jjil.core.Error.PACKAGE.CORE][jjil.core.ErrorCodes.PIPELINE_EMPTY_PUSH] =
            LS.getMessage("PIPELINE_EMPTY_PUSH");
        Error.szMessage[jjil.core.Error.PACKAGE.CORE][jjil.core.ErrorCodes.PIPELINE_NO_RESULT] =
            LS.getMessage("PIPELINE_NO_RESULT");

        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM] = new String[jjil.algorithm.ErrorCodes.COUNT];
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.BOUNDS_OUTSIDE_IMAGE] =
            LS.getMessage("BOUNDS_OUTSIDE_IMAGE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.CONN_COMP_LABEL_COMPARETO_NULL] =
            LS.getMessage("CONN_COMP_LABEL_COMPARETO_NULL");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.CONN_COMP_LABEL_OUT_OF_BOUNDS] =
            LS.getMessage("CONN_COMP_LABEL_OUT_OF_BOUNDS");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.INPUT_TERMINATED_EARLY] =
            LS.getMessage("INPUT_TERMINATED_EARLY");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.FFT_SIZE_LARGER_THAN_MAX] =
            LS.getMessage("FFT_SIZE_LARGER_THAN_MAX");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.FFT_SIZE_NOT_POWER_OF_2] =
            LS.getMessage("FFT_SIZE_NOT_POWER_OF_2");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.HEAP_EMPTY] =
            LS.getMessage("HEAP_EMPTY");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.HISTOGRAM_LENGTH_NOT_256] =
            LS.getMessage("HISTOGRAM_LENGTH_NOT_256");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.ILLEGAL_COLOR_CHOICE] =
            LS.getMessage("ILLEGAL_COLOR_CHOICE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_NOT_COMPLEX32IMAGE] =
            LS.getMessage("IMAGE_NOT_COMPLEX32IMAGE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_NOT_GRAY16IMAGE] =
            LS.getMessage("IMAGE_NOT_GRAY16IMAGE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_NOT_GRAY32IMAGE] =
            LS.getMessage("IMAGE_NOT_GRAY32IMAGE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_NOT_GRAY8IMAGE] =
            LS.getMessage("IMAGE_NOT_GRAY8IMAGE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_NOT_RGBIMAGE] =
            LS.getMessage("IMAGE_NOT_RGBIMAGE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_NOT_SQUARE] =
            LS.getMessage("IMAGE_NOT_SQUARE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_SIZES_DIFFER] =
            LS.getMessage("IMAGE_SIZES_DIFFER");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.IMAGE_TOO_SMALL] =
            LS.getMessage("IMAGE_TOO_SMALL");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.INPUT_IMAGE_SIZE_NEGATIVE] =
            LS.getMessage("INPUT_IMAGE_SIZE_NEGATIVE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.LOOKUP_TABLE_LENGTH_NOT_256] =
            LS.getMessage("LOOKUP_TABLE_LENGTH_NOT_256");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.OBJECT_NOT_EXPECTED_TYPE] =
            LS.getMessage("OBJECT_NOT_EXPECTED_TYPE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.OUTPUT_IMAGE_SIZE_NEGATIVE] =
            LS.getMessage("OUTPUT_IMAGE_SIZE_NEGATIVE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.PARAMETER_OUT_OF_RANGE] =
            LS.getMessage("PARAMETER_OUT_OF_RANGE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.PARAMETER_RANGE_NULL_OR_NEGATIVE] =
            LS.getMessage("PARAMETER_RANGE_NULL_OR_NEGATIVE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.PARSE_ERROR] =
            LS.getMessage("PARSE_ERROR");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.REDUCE_INPUT_IMAGE_NOT_MULTIPLE_OF_OUTPUT_SIZE] =
            LS.getMessage("REDUCE_INPUT_IMAGE_NOT_MULTIPLE_OF_OUTPUT_SIZE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.SHRINK_OUTPUT_LARGER_THAN_INPUT] =
            LS.getMessage("SHRINK_OUTPUT_LARGER_THAN_INPUT");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.STATISTICS_VARIANCE_LESS_THAN_ZERO] =
            LS.getMessage("STATISTICS_VARIANCE_LESS_THAN_ZERO");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.STRETCH_OUTPUT_SMALLER_THAN_INPUT] =
            LS.getMessage("STRETCH_OUTPUT_SMALLER_THAN_INPUT");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.SUBIMAGE_NO_IMAGE_AVAILABLE] =
            LS.getMessage("SUBIMAGE_NO_IMAGE_AVAILABLE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.THRESHOLD_NEGATIVE] =
            LS.getMessage("THRESHOLD_NEGATIVE");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.WARP_END_LEFT_COL_GE_END_RIGHT_COL] =
            LS.getMessage("WARP_END_LEFT_COL_GE_END_RIGHT_COL");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.WARP_START_LEFT_COL_GE_START_RIGHT_COL] =
            LS.getMessage("WARP_START_LEFT_COL_GE_START_RIGHT_COL");
        Error.szMessage[jjil.core.Error.PACKAGE.ALGORITHM][jjil.algorithm.ErrorCodes.WARP_START_ROW_GE_END_ROW] =
            LS.getMessage("WARP_START_ROW_GE_END_ROW");
    }
    
    /**
     * Copy constructor.
     * @param e the Error object to copy.
     */
    public Error(jjil.core.Error e) {
        super(e);
    }
        
    /**
     * Get a localized message for the Error.
     * @return a localized String describing the Error.
     */
    public String getLocalizedMessage() {
        String szResult = null;
        switch (this.getPackage()) {
            case Error.PACKAGE.CORE:
                if (this.getCode() < 0 || this.getCode() >= jjil.core.ErrorCodes.COUNT) {
                    szResult = LS.getMessage("Illegal_error_code_core") + 
                            new Integer(this.getCode()).toString();
                 } else {
                    szResult = szMessage[this.getPackage()][this.getCode()];
                }
                break;
            case Error.PACKAGE.ALGORITHM:
                if (this.getCode() < 0 || this.getCode() >= jjil.algorithm.ErrorCodes.COUNT) {
                    szResult = LS.getMessage("Illegal_error_code_algorithm") + 
                            new Integer(this.getCode()).toString();
                 } else {
                    szResult = szMessage[this.getPackage()][this.getCode()];
                }
                break;
            case jjil.core.Error.PACKAGE.J2ME:
                szResult = LS.getMessage("Illegal_error_code_j2me") +  " " +
                        new Integer(this.getCode()).toString();
                break;
            default:
                szResult = LS.getMessage("Illegal_error_code_package") + " " +
                        new Integer(this.getPackage()).toString() + " " +
                        new Integer(this.getCode()).toString();
                break;
           }
            return szResult + ":  " + parameters();
    }
}
