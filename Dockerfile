FROM amazoncorretto:17-alpine-jdk as os
RUN apk add --update docker

FROM os as base
WORKDIR bridge
COPY ./src ./src
COPY ./gradlew ./
COPY ./gradle ./gradle
COPY ./build.gradle.kts ./
COPY ./settings.gradle.kts ./

FROM base as build
RUN ./gradlew build -x test --console=verbose

#FROM build as test
#RUN ./gradlew test --console=verbose

FROM amazoncorretto:17-alpine
ENV USERNAME=$USERNAME
ENV PASSWORD=$PASSWORD
ENV BRIDGEMQTT_URL=$BRIDGEMQTT_URL
WORKDIR bridge
COPY --from=build ./bridge/build/libs/roborock-bridge-0.0.1-SNAPSHOT.jar ./
ENTRYPOINT java -jar roborock-bridge-0.0.1-SNAPSHOT.jar