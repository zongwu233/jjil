package jjil.algorithm;
//The code in this file is from
//http://www.java-tips.org/java-se-tips/java.lang/priority-queue-binary-heap-implementation-in-3.html

/**
 * Exception class for access in empty containers
 * such as stacks, queues, and priority queues.
 * @author Mark Allen Weiss
 */
public class UnderflowException extends RuntimeException {
    /**
     * Construct this exception object.
     * @param message the error message.
     */
    public UnderflowException( Throwable message ) {
        super( message );
    }
}

