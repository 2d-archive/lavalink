FROM frolvlad/alpine-glibc:alpine-3.9

RUN apk add --no-cache libstdc++
RUN wget "https://www.archlinux.org/packages/core/x86_64/zlib/download" -O /tmp/libz.tar.xz \
    && mkdir -p /tmp/libz \
    && tar -xf /tmp/libz.tar.xz -C /tmp/libz \
    && cp /tmp/libz/usr/lib/libz.so.1.2.11 /usr/glibc-compat/lib \
    && /usr/glibc-compat/sbin/ldconfig \
    && rm -rf /tmp/libz /tmp/libz.tar.xz

FROM azul/zulu-openjdk:13

# Run as non-root user
RUN groupadd -g 322 lavalink && \
    useradd -r -u 322 -g lavalink lavalink
USER lavalink

WORKDIR /opt/Lavalink

COPY Lavalink.jar Lavalink.jar

ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.1,TLSv1.2","-Dnativeloader.os=linux", "-Xmx4G", "-jar", "Lavalink.jar"]
