package com.commercehub.gradle.cucumber

import groovy.transform.CompileStatic
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

/**
 * Sends output to the Slf4j logger.
 */
@CompileStatic
class LoggingOutputStream extends OutputStream implements Closeable {
    private static final int NEWLINE = 10
    private static final int CARRIAGE_RETURN = 13

    private final Logger logger
    private final LogLevel logLevel
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream()

    LoggingOutputStream(Logger logger, LogLevel logLevel = LogLevel.INFO) {
        this.logger = logger
        this.logLevel = logLevel
    }

    private static boolean isEOL(int b) {
        (b == NEWLINE) || (b == CARRIAGE_RETURN)
    }

    private flushCompleteLines() {
        byte[] b = buffer.toByteArray()
        buffer.reset()
        int start = 0, end = 0
        for (; end < b.length; end++) {
            if (isEOL(b[end])) {
                if (end == start) {
                    logger.log(logLevel, '')
                } else {
                    logger.log(logLevel, new String(b, start, end - start))
                }
                start = end + 1
            }
        }
        if (start <= end && start < b.length) {
            // bytes left over
            buffer.write(b, start, end - start + 1)
        }
    }

    @Override
    void write(int b) throws IOException {
        buffer.write(b)
        if (isEOL(b)) {
            flushCompleteLines()
        }
    }

    @Override
    void write(byte[] b, int off, int len) throws IOException {
        buffer.write(b, off, len)
        // start from the end because the newline is most likely to be at the end
        for (int x = off + len - 1; x >= off; x--) {
            if (isEOL(b[x])) {
                flushCompleteLines()
                break
            }
        }
    }

    @Override
    void flush() throws IOException {
        buffer.write(NEWLINE)
        flushCompleteLines()
    }

    @Override
    void close() throws IOException {
        flush()
    }
}
