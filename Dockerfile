FROM amazoncorretto:11 as builder

WORKDIR /usr/src/jdime

COPY . .

RUN ./gradlew installDist

FROM gcr.io/distroless/java:8

WORKDIR /usr/bin/jdime

COPY --from=builder /usr/src/jdime/build .

WORKDIR /usr/bin/jdime/install/JDime/bin

CMD [ "./JDime" ]
