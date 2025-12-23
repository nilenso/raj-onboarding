# Sentinel Dockerfile for compiler service
FROM alpine:latest

RUN echo "Compiler service placeholder" > /msg.txt

CMD ["cat", "/msg.txt"]
