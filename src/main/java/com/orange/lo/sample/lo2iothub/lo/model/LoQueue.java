package com.orange.lo.sample.lo2iothub.lo.model;

public class LoQueue {

    private static final long DEFAULT_LENGTH_BYTES = 10485760;
    private static final long DEFAULT_MESSAGE_TTL = 604800;

    private String name;
    private long maxLengthBytes;
    private long messageTtl;

    public LoQueue(String name) {
        this(name, DEFAULT_LENGTH_BYTES, DEFAULT_MESSAGE_TTL);
    }

    public LoQueue(String name, long maxLengthBytes, long messageTtl) {
        this.name = name;
        this.maxLengthBytes = maxLengthBytes;
        this.messageTtl = messageTtl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxLengthBytes() {
        return maxLengthBytes;
    }

    public void setMaxLengthBytes(long maxLengthBytes) {
        this.maxLengthBytes = maxLengthBytes;
    }

    public long getMessageTtl() {
        return messageTtl;
    }

    public void setMessageTtl(long messageTtl) {
        this.messageTtl = messageTtl;
    }
}
