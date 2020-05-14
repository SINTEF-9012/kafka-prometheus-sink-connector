FROM golang:1.13

ADD discovery/ /discovery
RUN cd /discovery && go build -ldflags "-linkmode external -extldflags -static" -o discovery -a *.go && ls -la

FROM prom/prometheus

COPY --from=0 /discovery/discovery /bin/.
ADD entrypoint.sh /

ENTRYPOINT [ "/bin/sh", "/entrypoint.sh" ]
