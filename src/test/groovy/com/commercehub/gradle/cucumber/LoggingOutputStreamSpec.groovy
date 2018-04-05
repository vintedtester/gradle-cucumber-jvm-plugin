package com.commercehub.gradle.cucumber

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import spock.lang.Specification

class LoggingOutputStreamSpec extends Specification {
    Logger logger
    LoggingOutputStream outputStream

    void setup() {
        logger = Mock(Logger)
        outputStream = new LoggingOutputStream(logger, LogLevel.INFO)

        // wait for the logging thread to get started
        while (!outputStream.thread.isAlive()) {
            Thread.sleep(1000)
        }
    }

    void cleanup() {
        outputStream.close()
    }

    void "flush empty"() {
        when:
        outputStream.close()
        outputStream.waitForEmpty()
        then:
        0 * logger.log(LogLevel.INFO, _)
    }

    void "one line with newline"() {
        when:
        outputStream.write('one line\n'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "two lines with newline"() {
        when:
        outputStream.write('one line\ntwo line\n'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'two line')
    }

    void "closes flushes data without newline"() {
        when:
        outputStream.write('one line'.bytes)
        outputStream.close()
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "flush waits for newline"() {
        when:
        outputStream.write('one line'.bytes)
        outputStream.flush()
        outputStream.waitForEmpty()
        then:
        0 * logger.log(LogLevel.INFO,_)
    }

    void "consecutive newlines"() {
        when:
        outputStream.write('one line\n\n'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'')
    }

    void "one line with newline by each byte"() {
        when:
        'one line\n'.bytes.each { outputStream.write(it) }
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "two lines with newline by each byte"() {
        when:
        'one line\ntwo line\n'.bytes.each { outputStream.write(it) }
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'two line')
    }

    void "one line with CRLF"() {
        when:
        outputStream.write('one line\r\n'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "two lines with CRLF"() {
        when:
        outputStream.write('one line\r\ntwo line\r\n'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'two line')
    }

    void "closes flushes data without CRLF"() {
        when:
        outputStream.write('one line'.bytes)
        outputStream.close()
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "flush waits for CRLF"() {
        when:
        outputStream.write('one line'.bytes)
        outputStream.flush()
        outputStream.waitForEmpty()
        then:
        0 * logger.log(LogLevel.INFO,_)
    }

    void "consecutive CRLFs"() {
        when:
        outputStream.write('one line\r\n\r\n'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'')
    }

    void "one line with CRLF by each byte"() {
        when:
        'one line\r\n'.bytes.each { outputStream.write(it) }
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "two lines with CRLF by each byte"() {
        when:
        'one line\r\ntwo line\r\n'.bytes.each { outputStream.write(it) }
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'two line')
    }

    void "one line with CR"() {
        when:
        outputStream.write('one line\r'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "two lines with CR"() {
        when:
        outputStream.write('one line\rtwo line\r'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'two line')
    }

    void "closes flushes data without CR"() {
        when:
        outputStream.write('one line'.bytes)
        outputStream.close()
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "flush waits for CR"() {
        when:
        outputStream.write('one line'.bytes)
        outputStream.flush()
        outputStream.waitForEmpty()
        then:
        0 * logger.log(LogLevel.INFO,_)
    }

    void "consecutive CRs"() {
        when:
        outputStream.write('one line\r\r'.bytes)
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'')
    }

    void "one line with CR by each byte"() {
        when:
        'one line\r'.bytes.each { outputStream.write(it) }
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
    }

    void "two lines with CR by each byte"() {
        when:
        'one line\rtwo line\r'.bytes.each { outputStream.write(it) }
        outputStream.waitForEmpty()
        then:
        1 * logger.log(LogLevel.INFO,'one line')
        1 * logger.log(LogLevel.INFO,'two line')
    }
}
