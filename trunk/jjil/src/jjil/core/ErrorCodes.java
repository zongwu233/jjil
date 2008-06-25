package jjil.core;

/**
 * J2ME's version of Java doesn't support enums. So we simulate them
 * using an old trick...
 */
public class ErrorCodes {
        public static final int IMAGE_MASK_SIZE_MISMATCH = 0;	// image and mask sizes don't match
        public static final int MATH_DIVISION_ZERO = 
        	IMAGE_MASK_SIZE_MISMATCH + 1;				// attempt to divide by zero
        public static final int MATH_NEGATIVE_SQRT = 
        	MATH_DIVISION_ZERO + 1;						// attempt to take sqrt of negative
        public static final int MATH_PRODUCT_TOO_LARGE = 
        	MATH_NEGATIVE_SQRT + 1;						// operands too large to multiply
        public static final int MATH_SQUARE_TOO_LARGE = 
        	MATH_PRODUCT_TOO_LARGE + 1;					// operand too large to take square
        public static final int PIPELINE_EMPTY_PUSH = 
        	MATH_SQUARE_TOO_LARGE + 1;					// pipeline empty when image is being pushed
        public static final int PIPELINE_NO_RESULT = 
        	PIPELINE_EMPTY_PUSH + 1;					// pipeline didn't return an expected result
        
        public static final int COUNT = PIPELINE_EMPTY_PUSH + 1;
}
