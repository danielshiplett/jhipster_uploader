FROM java:8
VOLUME /tmp
ADD target/upload-example-0.0.1-SNAPSHOT.war app.war
RUN bash -c 'touch /app.war'
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.war","-Pprod"]