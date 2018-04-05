package com.commercehub.gradle.cucumber

import groovy.transform.CompileStatic
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Sends output to the Slf4j logger.
 */
@CompileStatic
class LoggingOutputStream extends OutputStream implements Closeable {
    private static final int NEWLINE = 10
    private static final int CARRIAGE_RETURN = 13

    /**
     * To prevent blocking if nothing is reading the log output, use a queue and a separate thread. The
     * queue size is to prevent running out of memory in case nothing is reading the output.
     */
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10000)
    private final Thread thread

    private final Logger logger
    private final LogLevel logLevel
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256)

    LoggingOutputStream(Logger logger, LogLevel logLevel = LogLevel.INFO) {
        this.logger = logger
        this.logLevel = logLevel

        thread = new Thread("LoggingOutputStream: ${logLevel}") {
            @Override
            void run() {
                try {
                    // we want to flush the queue when the thread is interrupted
                    while (!interrupted || !queue.empty) {
                        logger.log(logLevel, queue.take())
                    }
                } catch (InterruptedException e) {
                    // expected when closing the output stream and we're blocking on the queue
                }
            }
        }
        thread.setDaemon(true)
        thread.setPriority(Thread.MIN_PRIORITY)
        thread.start()
    }

    private static boolean isEOL(int b) {
        (b == NEWLINE) || (b == CARRIAGE_RETURN)
    }

    private flushCompleteLines() {
        if (buffer.size() == 0) {
            return
        }
        byte[] b = buffer.toByteArray()
        buffer.reset()
        int start = 0, end = 0
        for (; end < b.length; end++) {
            if (isEOL(b[end])) {
                if (end == start) {
                    queue.offer('')
                } else {
                    queue.offer(new String(b, start, end - start))
                }
                start = end + 1
                if (b[end] == CARRIAGE_RETURN && (start < b.length) && b[start] == NEWLINE) {
                    start++
                    end++
                }
            }
        }
        if (start < end && start < b.length) {
            // bytes left over
            buffer.write(b, start, end - start)
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
        flushCompleteLines()
    }

    @Override
    void close() throws IOException {
        if (thread.getState() == Thread.State.TERMINATED) {
            return
        }

        flushCompleteLines()
        if (buffer.size() > 0) {
            buffer.write(NEWLINE)
            flushCompleteLines()
        }

        waitForEmpty()
        thread.interrupt()
    }

    void waitForEmpty(int timeout = 5000) {
        long end = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < end &&
                (thread.getState() in [Thread.State.NEW, Thread.State.RUNNABLE] || !queue.isEmpty())) {
            Thread.yield()
            Thread.sleep(200)
        }
    }
}
