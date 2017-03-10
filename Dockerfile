FROM openjdk:8

COPY ./target/universal/listan-server-1.0.4.zip /
RUN unzip listan-server-1.0.4.zip

ENV DB_URL=localhost/listan \
    DB_USER=listan \
    CRYPTO_SECRET=changeme

ENV PATH=/listan-server-1.0.4:$PATH

EXPOSE 9000

WORKDIR /listan-server-1.0.4
ENTRYPOINT ["bin/listan-server"]
CMD ["-Dconfig.file=conf/application.prod.conf"]

