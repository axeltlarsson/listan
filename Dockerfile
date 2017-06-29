FROM openjdk:8

ENV DB_URL=localhost/listan \
    DB_USER=listan \
    DB_PASSWORD=pasword \
    CRYPTO_SECRET=changeme \
    VERSION=1.1.0

COPY ./target/universal/listan-server-${VERSION}.zip /
RUN unzip listan-server-${VERSION}.zip


ENV PATH=/listan-server-${VERSION}:$PATH

EXPOSE 9000

WORKDIR /listan-server-${VERSION}
ENTRYPOINT ["bin/listan-server"]
CMD ["-Dconfig.file=conf/application.prod.conf"]

