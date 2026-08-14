package com.samleighton.sethomestwo.updates;

import java.io.IOException;

@FunctionalInterface
public interface ReleaseSource {
    String latestTag() throws IOException;
}
