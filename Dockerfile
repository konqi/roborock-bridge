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
RUN <<EOF
    for file in build/libs/roborock-bridge-*.jar; do
      [ -e "$file" ] && mv "$file" ./build/libs/roborock-bridge.jar && break
    done
EOF

#FROM build as test
#RUN ./gradlew test --console=verbose

FROM amazoncorretto:17-alpine
ENV USERNAME=$USERNAME
ENV PASSWORD=$PASSWORD
ENV BRIDGEMQTT_URL=$BRIDGEMQTT_URL
WORKDIR bridge
COPY --from=build ./bridge/build/libs/roborock-bridge.jar ./
ENTRYPOINT java -jar roborock-bridge.jar