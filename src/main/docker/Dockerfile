FROM golang:1.13

ADD discovery/ /discovery
RUN cd /discovery && go build -ldflags "-linkmode external -extldflags -static" -o discovery -a *.go

FROM prom/prometheus

COPY --from=0 /discovery/discovery /bin/.
ADD entrypoint.sh /
ADD prometheus.yml /etc/prometheus/prometheus.yml

ENTRYPOINT [ "/bin/sh", "/entrypoint.sh" ]
