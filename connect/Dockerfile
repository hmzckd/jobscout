FROM apache/kafka:4.2.2

ARG DEBEZIUM_VERSION=3.7.1.Final
ARG DEBEZIUM_SHA256=0b2ec2bb5b84dacf4501773aca754bba05c299ca2ae278985f1af260460af955

ADD --checksum=sha256:${DEBEZIUM_SHA256} --chown=appuser:appuser \
    https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/${DEBEZIUM_VERSION}/debezium-connector-postgres-${DEBEZIUM_VERSION}-plugin.tar.gz \
    /tmp/debezium-connector-postgres.tar.gz

USER root
RUN mkdir -p /opt/connect-plugins \
    && tar -xzf /tmp/debezium-connector-postgres.tar.gz -C /opt/connect-plugins \
    && rm /tmp/debezium-connector-postgres.tar.gz \
    && chown -R appuser:appuser /opt/connect-plugins
USER appuser

COPY connect-distributed.properties /opt/connect/connect-distributed.properties

EXPOSE 8083
CMD ["/opt/kafka/bin/connect-distributed.sh", "/opt/connect/connect-distributed.properties"]
