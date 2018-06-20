FROM openjdk:8

ENV VERSION=2.1.0

COPY ./target/universal/listan-${VERSION}.zip /
RUN unzip listan-${VERSION}.zip


ENV PATH=/listan-${VERSION}:$PATH

EXPOSE 9000

WORKDIR /listan-${VERSION}
ENTRYPOINT ["bin/listan"]
CMD ["-Dconfig.file=conf/application.prod.conf"]

