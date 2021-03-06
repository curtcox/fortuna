package com.grunka.random.fortuna;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

class Generator {
    private static final int KEY_LENGTH = 32;
    private static final int BLOCK_LENGTH = 16;
    private final Counter counter;
    private final byte[] key = new byte[KEY_LENGTH];
    private final MessageDigest reseedDigest;
    private final Encryption encryption;

    Generator() {
        encryption = new Encryption();
        counter = new Counter(128);
        try {
            reseedDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Was not able to initialize digest", e);
        }
    }

    void reseed(byte[] seed) {
        reseedDigest.update(key);
        System.arraycopy(reseedDigest.digest(seed), 0, key, 0, KEY_LENGTH);
        encryption.setKey(key);
        counter.increment();
    }

    private byte[] generateBlocks(int blocks) {
        if (counter.isZero()) {
            throw new IllegalStateException("Generator not yet initialized");
        }
        byte[] result = new byte[blocks * BLOCK_LENGTH];
        for (int block = 0; block < blocks; block++) {
            byte[] encryptedBytes = encryption.encrypt(counter.getState());
            System.arraycopy(encryptedBytes, 0, result, block * BLOCK_LENGTH, BLOCK_LENGTH);
            counter.increment();
        }
        return result;
    }

    byte[] pseudoRandomData(int bytes) {
        if (bytes < 0 || bytes > 1048576) {
            throw new IllegalArgumentException("Cannot generate " + bytes + " bytes of random data");
        }
        try {
            return Arrays.copyOf(generateBlocks(Util.ceil(bytes, BLOCK_LENGTH)), bytes);
        } finally {
            System.arraycopy(generateBlocks(2), 0, key, 0, KEY_LENGTH);
            encryption.setKey(key);
        }
    }
}
