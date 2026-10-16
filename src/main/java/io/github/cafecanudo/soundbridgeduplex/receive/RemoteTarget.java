package io.github.cafecanudo.soundbridgeduplex.receive;

record RemoteTarget(String destination, String file) {

    private static final char SEPARATOR = '/';

    static RemoteTarget of(String remoteName) {
        String normalized = remoteName.replace('\\', SEPARATOR);
        int cut = normalized.lastIndexOf(SEPARATOR);
        return cut < 0 ? new RemoteTarget("", normalized)
                : new RemoteTarget(normalized.substring(0, cut), normalized.substring(cut + 1));
    }
}
