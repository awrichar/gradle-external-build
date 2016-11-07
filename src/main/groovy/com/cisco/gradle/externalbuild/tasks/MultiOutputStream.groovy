package com.cisco.gradle.externalbuild.tasks

class MultiOutputStream extends OutputStream {
    private List<OutputStream> streams

    MultiOutputStream(OutputStream... streams) {
        this.streams = streams
    }

    @Override
    void write(byte[] b) throws IOException {
        streams.each { it.write(b) }
    }

    @Override
    void write(byte[] b, int off, int len) throws IOException {
        streams.each { it.write(b, off, len) }
    }

    @Override
    void write(int b) throws IOException {
        streams.each { it.write(b) }
    }

    @Override
    void flush() throws IOException {
        streams.each{ it.flush() }
    }

    @Override
    void close() throws IOException {
        streams.each { it.close() }
    }
}
